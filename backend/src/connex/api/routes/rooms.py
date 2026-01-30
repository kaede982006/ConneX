from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from connex.core.deps import get_current_user
from connex.db.session import get_session
from connex.models.channel import Channel
from connex.models.role import Role
from connex.models.role_assignment import RoleAssignment
from connex.models.room import Room
from connex.models.room_member import RoomMember
from connex.models.user import User
from connex.schemas.rooms import (
    ChannelResponse,
    CreateChannelRequest,
    CreateRoleRequest,
    CreateRoomRequest,
    GrantRoleRequest,
    JoinLeaveResponse,
    MemberResponse,
    RevokeRoleRequest,
    RoleResponse,
    RoomResponse,
)

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
    session.add(owner_role)
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
    if room_obj.owner_id != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="only owner can create channels")
    ch = Channel(room_id=room_id, name=request.name)
    session.add(ch)
    await session.commit()
    await session.refresh(ch)
    return ChannelResponse(channel_id=ch.id, room_id=ch.room_id, name=ch.name)

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
    result = await session.execute(
        select(User, Role)
        .join(RoomMember, RoomMember.user_id == User.id)
        .outerjoin(RoleAssignment, RoleAssignment.user_id == User.id)
        .outerjoin(Role, Role.id == RoleAssignment.role_id)
        .where(RoomMember.room_id == room_id)
    )
    responses = []
    for user, role in result.all():
        responses.append(
            MemberResponse(
                user_id=user.id,
                username=user.username,
                display_name=user.display_name,
                role=role.name if role else None,
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
    if room_obj.owner_id != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="only owner can create roles")
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
    if room_obj.owner_id != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="only owner can grant roles")
    role = await session.execute(select(Role).where(Role.id == int(request.role_id), Role.room_id == room_id))
    role_obj = role.scalar_one_or_none()
    if role_obj is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="role not found")
    assignment = RoleAssignment(room_id=room_id, user_id=int(request.user_id), role_id=role_obj.id)
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
    if room_obj.owner_id != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="only owner can revoke roles")
    result = await session.execute(
        select(RoleAssignment)
        .where(
            RoleAssignment.room_id == room_id,
            RoleAssignment.user_id == int(request.user_id),
            RoleAssignment.role_id == int(request.role_id),
        )
    )
    assignment = result.scalar_one_or_none()
    if assignment is None:
        return {"status": "not_found"}
    await session.delete(assignment)
    await session.commit()
    return {"status": "revoked"}
