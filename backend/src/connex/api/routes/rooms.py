from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import and_, case, delete, func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession
from connex.core.deps import get_current_user
from connex.db.session import get_session
from connex.models.channel import Channel
from connex.models.role import Role
from connex.models.role_assignment import RoleAssignment
from connex.models.room import Room
from connex.models.room_ban import RoomBan
from connex.models.room_member import RoomMember
from connex.models.room_key import RoomKey
from connex.models.user import User
from connex.services.ws_manager import manager
from connex.schemas.rooms import (
    ChannelResponse,
    CreateChannelRequest,
    CreateRoleRequest,
    CreateRoomRequest,
    BanMemberRequest,
    BanResponse,
    GrantRoleRequest,
    JoinLeaveResponse,
    MemberResponse,
    MessageResponse,
    RevokeRoleRequest,
    RoleResponse,
    RoomResponse,
    SubAdminRequest,
)
from connex.models.message import Message

async def is_owner(session: AsyncSession, user_id: int, room_id: int) -> bool:
    result = await session.execute(select(Room).where(Room.id == room_id, Room.owner_id == user_id))
    return result.scalar_one_or_none() is not None

async def has_role(session: AsyncSession, user_id: int, room_id: int, role_name: str) -> bool:
    result = await session.execute(
        select(Role)
        .join(RoleAssignment, RoleAssignment.role_id == Role.id)
        .where(
            RoleAssignment.room_id == room_id,
            RoleAssignment.user_id == user_id,
            Role.name == role_name,
        )
    )
    return result.scalar_one_or_none() is not None

async def is_banned(session: AsyncSession, user_id: int, room_id: int) -> bool:
    result = await session.execute(
        select(RoomBan).where(RoomBan.room_id == room_id, RoomBan.user_id == user_id)
    )
    return result.scalar_one_or_none() is not None

async def can_ban(session: AsyncSession, user_id: int, room_id: int) -> bool:
    if await is_owner(session, user_id, room_id):
        return True
    return await has_role(session, user_id, room_id, "manager")

async def check_permission(
    session: AsyncSession, user_id: int, room_id: int, required_perm: str
) -> bool:
    if await is_owner(session, user_id, room_id):
        return True
    if required_perm == "MANAGE_CHANNELS":
        return await has_role(session, user_id, room_id, "manager")
    return False

async def get_manager_role(session: AsyncSession, room_id: int) -> Role | None:
    result = await session.execute(select(Role).where(Role.room_id == room_id, Role.name == "manager"))
    return result.scalar_one_or_none()

async def ensure_manager_role(session: AsyncSession, room_id: int) -> Role:
    manager_role = await get_manager_role(session, room_id)
    if manager_role is not None:
        return manager_role
    manager_role = Role(room_id=room_id, name="manager", permissions=["MANAGE_CHANNELS"])
    session.add(manager_role)
    await session.commit()
    await session.refresh(manager_role)
    return manager_role

router = APIRouter()

@router.get("", response_model=list[RoomResponse])
async def list_rooms(
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    result = await session.execute(
        select(Room).join(RoomMember, RoomMember.room_id == Room.id).where(RoomMember.user_id == current_user.id)
    )
    return [RoomResponse(room_id=r.id, title=r.title, owner_id=r.owner_id) for r in result.scalars().all()]

@router.get("/search", response_model=list[RoomResponse])
async def search_rooms(
    q: str = Query(..., min_length=1),
    limit: int = Query(20, ge=1, le=50),
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    query = q.strip().lower()
    if not query:
        return []

    tokens = [token for token in query.split() if token]
    title_filters = [func.lower(Room.title).like(f"%{token}%") for token in tokens]
    title_match = and_(*title_filters) if title_filters else None

    room_id_match = None
    if query.isdigit():
        room_id_match = Room.id == int(query)

    filters = []
    if title_match is not None:
        filters.append(title_match)
    if room_id_match is not None:
        filters.append(room_id_match)

    if not filters:
        return []

    order_rank = case(
        (room_id_match, 0) if room_id_match is not None else (Room.id == -1, 0),
        (func.lower(Room.title).like(f"{query}%"), 1),
        else_=2,
    )

    stmt = (
        select(Room)
        .where(or_(*filters))
        .order_by(order_rank, Room.title.asc())
        .limit(limit)
    )
    result = await session.execute(stmt)
    return [RoomResponse(room_id=r.id, title=r.title, owner_id=r.owner_id) for r in result.scalars().all()]

@router.post("", response_model=RoomResponse)
async def create_room(
    request: CreateRoomRequest,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    room = Room(title=request.title, owner_id=current_user.id)
    session.add(room)
    await session.commit()
    await session.refresh(room)

    membership = RoomMember(room_id=room.id, user_id=current_user.id)
    session.add(membership)

    channel = Channel(room_id=room.id, name="general")
    session.add(channel)

    owner_role = Role(room_id=room.id, name="owner", permissions=["ALL"])
    manager_role = Role(room_id=room.id, name="manager", permissions=["MANAGE_CHANNELS"])
    session.add_all([owner_role, manager_role])
    await session.commit()
    await session.refresh(owner_role)

    assignment = RoleAssignment(room_id=room.id, user_id=current_user.id, role_id=owner_role.id)
    session.add(assignment)
    await session.commit()

    return RoomResponse(room_id=room.id, title=room.title, owner_id=room.owner_id)

@router.post("/{room_id}/join", response_model=JoinLeaveResponse)
async def join_room(
    room_id: int,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    result = await session.execute(select(Room).where(Room.id == room_id))
    room = result.scalar_one_or_none()
    if room is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="room not found")
    if await is_banned(session, current_user.id, room_id):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="banned")

    existing = await session.execute(
        select(RoomMember).where(RoomMember.room_id == room_id, RoomMember.user_id == current_user.id)
    )
    if existing.scalar_one_or_none() is None:
        session.add(RoomMember(room_id=room_id, user_id=current_user.id))
        await session.commit()
    return JoinLeaveResponse(status="joined")

@router.post("/{room_id}/leave", response_model=JoinLeaveResponse)
async def leave_room(
    room_id: int,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    room_result = await session.execute(select(Room).where(Room.id == room_id))
    room = room_result.scalar_one_or_none()
    if room is None:
        return JoinLeaveResponse(status="not_found")
    if room.owner_id == current_user.id:
        await session.execute(delete(Message).where(Message.room_id == room_id))
        await session.execute(delete(RoomKey).where(RoomKey.room_id == room_id))
        await session.execute(delete(RoleAssignment).where(RoleAssignment.room_id == room_id))
        await session.execute(delete(Role).where(Role.room_id == room_id))
        await session.execute(delete(Channel).where(Channel.room_id == room_id))
        await session.execute(delete(RoomBan).where(RoomBan.room_id == room_id))
        await session.execute(delete(RoomMember).where(RoomMember.room_id == room_id))
        await session.delete(room)
        await session.commit()
        return JoinLeaveResponse(status="room_deleted")
    result = await session.execute(
        select(RoomMember).where(RoomMember.room_id == room_id, RoomMember.user_id == current_user.id)
    )
    membership = result.scalar_one_or_none()
    if membership is None:
        return JoinLeaveResponse(status="not_member")
    await session.delete(membership)
    await session.commit()
    return JoinLeaveResponse(status="left")

@router.get("/{room_id}/channels", response_model=list[ChannelResponse])
async def list_channels(
    room_id: int,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    membership = await session.execute(
        select(RoomMember).where(RoomMember.room_id == room_id, RoomMember.user_id == current_user.id)
    )
    if membership.scalar_one_or_none() is None:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="not a member")
    result = await session.execute(select(Channel).where(Channel.room_id == room_id))
    return [ChannelResponse(channel_id=c.id, room_id=c.room_id, name=c.name) for c in result.scalars().all()]

@router.post("/{room_id}/channels", response_model=ChannelResponse)
async def create_channel(
    room_id: int,
    request: CreateChannelRequest,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    room = await session.execute(select(Room).where(Room.id == room_id))
    room_obj = room.scalar_one_or_none()
    if room_obj is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="room not found")
    if not await check_permission(session, current_user.id, room_id, "MANAGE_CHANNELS"):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="missing permission: MANAGE_CHANNELS")
    ch = Channel(room_id=room_id, name=request.name)
    session.add(ch)
    await session.commit()
    await session.refresh(ch)
    return ChannelResponse(channel_id=ch.id, room_id=ch.room_id, name=ch.name)

@router.get("/{room_id}/channels/{channel_id}/messages", response_model=list[MessageResponse])
async def list_messages(
    room_id: int,
    channel_id: int,
    limit: int = 50,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    membership = await session.execute(
        select(RoomMember).where(RoomMember.room_id == room_id, RoomMember.user_id == current_user.id)
    )
    if membership.scalar_one_or_none() is None:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="not a member")

    # Fetch messages joined with sender for username/display_name
    result = await session.execute(
        select(Message, User)
        .join(User, User.id == Message.sender_id)
        .where(Message.room_id == room_id, Message.channel_id == channel_id)
        .order_by(Message.created_at.desc())
        .limit(limit)
    )
    
    # Return reversed list to show oldest->newest in UI if needed, or UI handles it. 
    # Usually API returns desc for efficiency, UI reverses or prepends.
    # Let's return as is (descending time) for now.
    
    msgs = []
    for msg, sender in result.all():
        msgs.append(
            MessageResponse(
                message_id=msg.id,
                room_id=msg.room_id,
                channel_id=msg.channel_id,
                sender_id=msg.sender_id,
                sender_name=sender.display_name or sender.username,
                envelope=msg.content,
                created_at_ms=int(msg.created_at.timestamp() * 1000)
            )
        )
    return msgs

@router.get("/{room_id}/members", response_model=list[MemberResponse])
async def list_members(
    room_id: int,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    membership = await session.execute(
        select(RoomMember).where(RoomMember.room_id == room_id, RoomMember.user_id == current_user.id)
    )
    if membership.scalar_one_or_none() is None:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="not a member")
    stmt = (
        select(User, Role)
        .join(RoomMember, RoomMember.user_id == User.id)
        .outerjoin(
            RoleAssignment, 
            (RoleAssignment.user_id == User.id) & (RoleAssignment.room_id == room_id)
        )
        .outerjoin(Role, Role.id == RoleAssignment.role_id)
        .where(RoomMember.room_id == room_id)
    )
    
    result = await session.execute(stmt)
    
    members_map: dict[int, dict[str, object]] = {}
    for user, role in result.all():
        if user.id not in members_map:
            members_map[user.id] = {
                "user": user,
                "roles": set()
            }
        if role:
            members_map[user.id]["roles"].add(role.name)
            
    responses = []
    for uid, data in members_map.items():
        user = data["user"]
        responses.append(
            MemberResponse(
                user_id=user.id,
                username=user.username,
                display_name=user.display_name,
                roles=sorted(data["roles"])
            )
        )
    return responses

@router.get("/{room_id}/roles", response_model=list[RoleResponse])
async def list_roles(
    room_id: int,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    membership = await session.execute(
        select(RoomMember).where(RoomMember.room_id == room_id, RoomMember.user_id == current_user.id)
    )
    if membership.scalar_one_or_none() is None:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="not a member")
    result = await session.execute(select(Role).where(Role.room_id == room_id))
    return [RoleResponse(id=r.id, name=r.name, permissions=r.permissions) for r in result.scalars().all()]

@router.post("/{room_id}/roles", response_model=RoleResponse)
async def create_role(
    room_id: int,
    request: CreateRoleRequest,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    room = await session.execute(select(Room).where(Room.id == room_id))
    room_obj = room.scalar_one_or_none()
    if room_obj is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="room not found")
    if not await check_permission(session, current_user.id, room_id, "MANAGE_ROLES"):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="missing permission: MANAGE_ROLES")
    role = Role(room_id=room_id, name=request.name, permissions=request.permissions)
    session.add(role)
    await session.commit()
    await session.refresh(role)
    return RoleResponse(id=role.id, name=role.name, permissions=role.permissions)

@router.post("/{room_id}/roles/grant")
async def grant_role(
    room_id: int,
    request: GrantRoleRequest,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    room = await session.execute(select(Room).where(Room.id == room_id))
    room_obj = room.scalar_one_or_none()
    if room_obj is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="room not found")
    if not await check_permission(session, current_user.id, room_id, "MANAGE_ROLES"):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="missing permission: MANAGE_ROLES")
    role = await session.execute(select(Role).where(Role.id == request.role_id, Role.room_id == room_id))
    role_obj = role.scalar_one_or_none()
    if role_obj is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="role not found")
    if role_obj.name == "manager" and not await is_owner(session, current_user.id, room_id):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="only owner can grant manager")
    assignment = RoleAssignment(room_id=room_id, user_id=request.user_id, role_id=role_obj.id)
    session.add(assignment)
    await session.commit()
    return {"status": "granted"}

@router.post("/{room_id}/roles/revoke")
async def revoke_role(
    room_id: int,
    request: RevokeRoleRequest,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    room = await session.execute(select(Room).where(Room.id == room_id))
    room_obj = room.scalar_one_or_none()
    if room_obj is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="room not found")
    if not await check_permission(session, current_user.id, room_id, "MANAGE_ROLES"):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="missing permission: MANAGE_ROLES")
    result = await session.execute(
        select(RoleAssignment)
        .where(
            RoleAssignment.room_id == room_id,
            RoleAssignment.user_id == request.user_id,
            RoleAssignment.role_id == request.role_id,
        )
    )
    assignment = result.scalar_one_or_none()
    if assignment is None:
        return {"status": "not_found"}
    role = await session.execute(select(Role).where(Role.id == assignment.role_id, Role.room_id == room_id))
    role_obj = role.scalar_one_or_none()
    if role_obj and role_obj.name == "manager" and not await is_owner(session, current_user.id, room_id):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="only owner can revoke manager")
    await session.delete(assignment)
    await session.commit()
    return {"status": "revoked"}

@router.post("/{room_id}/subadmins")
async def assign_subadmin(
    room_id: int,
    request: SubAdminRequest,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    room = await session.execute(select(Room).where(Room.id == room_id))
    room_obj = room.scalar_one_or_none()
    if room_obj is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="room not found")
    if not await is_owner(session, current_user.id, room_id):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="only owner can assign subadmin")
    membership = await session.execute(
        select(RoomMember).where(RoomMember.room_id == room_id, RoomMember.user_id == request.user_id)
    )
    if membership.scalar_one_or_none() is None:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="user not in room")
    manager_role = await ensure_manager_role(session, room_id)
    existing = await session.execute(
        select(RoleAssignment).where(
            RoleAssignment.room_id == room_id,
            RoleAssignment.user_id == request.user_id,
            RoleAssignment.role_id == manager_role.id,
        )
    )
    if existing.scalar_one_or_none() is not None:
        return {"status": "already_granted"}
    assignment = RoleAssignment(room_id=room_id, user_id=request.user_id, role_id=manager_role.id)
    session.add(assignment)
    await session.commit()
    return {"status": "granted"}

@router.delete("/{room_id}/subadmins/{user_id}")
async def revoke_subadmin(
    room_id: int,
    user_id: int,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    room = await session.execute(select(Room).where(Room.id == room_id))
    room_obj = room.scalar_one_or_none()
    if room_obj is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="room not found")
    if not await is_owner(session, current_user.id, room_id):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="only owner can revoke subadmin")
    manager_role = await ensure_manager_role(session, room_id)
    existing = await session.execute(
        select(RoleAssignment).where(
            RoleAssignment.room_id == room_id,
            RoleAssignment.user_id == user_id,
            RoleAssignment.role_id == manager_role.id,
        )
    )
    assignment = existing.scalar_one_or_none()
    if assignment is None:
        return {"status": "not_found"}
    await session.delete(assignment)
    await session.commit()
    return {"status": "revoked"}

@router.post("/{room_id}/bans", response_model=BanResponse)
async def ban_member(
    room_id: int,
    request: BanMemberRequest,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    room = await session.execute(select(Room).where(Room.id == room_id))
    room_obj = room.scalar_one_or_none()
    if room_obj is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="room not found")
    if not await can_ban(session, current_user.id, room_id):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="missing permission: BAN_MEMBERS")
    if request.user_id == current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="cannot ban self")
    if await is_owner(session, request.user_id, room_id):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="cannot ban owner")
    if not await is_owner(session, current_user.id, room_id) and await has_role(session, request.user_id, room_id, "manager"):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="only owner can ban manager")

    membership = await session.execute(
        select(RoomMember).where(RoomMember.room_id == room_id, RoomMember.user_id == request.user_id)
    )
    if membership.scalar_one_or_none() is None:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="user not in room")

    existing_ban = await session.execute(
        select(RoomBan).where(RoomBan.room_id == room_id, RoomBan.user_id == request.user_id)
    )
    if existing_ban.scalar_one_or_none() is not None:
        return BanResponse(status="already_banned")

    session.add(
        RoomBan(
            room_id=room_id,
            user_id=request.user_id,
            banned_by_user_id=current_user.id,
        )
    )
    await session.execute(
        delete(RoleAssignment).where(RoleAssignment.room_id == room_id, RoleAssignment.user_id == request.user_id)
    )
    await session.execute(
        delete(RoomMember).where(RoomMember.room_id == room_id, RoomMember.user_id == request.user_id)
    )
    await session.commit()
    await manager.disconnect_user(room_id, request.user_id)
    return BanResponse(status="banned")
