from fastapi import APIRouter, Header, HTTPException, Response, status
from connex.api.schemas import (
    ChannelDto,
    CreateChannelReq,
    CreateRoleReq,
    CreateRoomReq,
    GrantRoleReq,
    MemberResp,
    RevokeRoleReq,
    RoleResp,
    RoomDto,
)
from connex.api.state import state

router = APIRouter()


def _get_user_id(x_user_id: int | None) -> int:
    user_id = x_user_id or state.last_user_id
    if user_id is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not authenticated",
        )
    return user_id


@router.get("/", response_model=list[RoomDto])
async def list_rooms() -> list[RoomDto]:
    return [
        RoomDto(roomId=room.id, title=room.title, ownerId=room.owner_id)
        for room in state.rooms.values()
    ]


@router.post("/", response_model=RoomDto, status_code=status.HTTP_201_CREATED)
async def create_room(
    request: CreateRoomReq,
    x_user_id: int | None = Header(default=None, alias="X-User-Id"),
) -> RoomDto:
    owner_id = _get_user_id(x_user_id)
    room = state.create_room(request.title, owner_id)
    state.ensure_membership(owner_id, room.id, role="owner")
    return RoomDto(roomId=room.id, title=room.title, ownerId=room.owner_id)


@router.post("/{room_id}/join", status_code=status.HTTP_204_NO_CONTENT)
async def join_room(
    room_id: int,
    x_user_id: int | None = Header(default=None, alias="X-User-Id"),
) -> Response:
    if room_id not in state.rooms:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Room not found")
    user_id = _get_user_id(x_user_id)
    state.ensure_membership(user_id, room_id, role="member")
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post("/{room_id}/leave", status_code=status.HTTP_204_NO_CONTENT)
async def leave_room(
    room_id: int,
    x_user_id: int | None = Header(default=None, alias="X-User-Id"),
) -> Response:
    user_id = _get_user_id(x_user_id)
    state.remove_membership(user_id, room_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.get("/{room_id}/channels", response_model=list[ChannelDto])
async def list_channels(room_id: int) -> list[ChannelDto]:
    room = state.rooms.get(room_id)
    if not room:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Room not found")
    return [
        ChannelDto(channelId=ch.id, roomId=ch.room_id, name=ch.name)
        for ch in room.channels.values()
    ]


@router.post("/{room_id}/channels", response_model=ChannelDto, status_code=status.HTTP_201_CREATED)
async def create_channel(room_id: int, request: CreateChannelReq) -> ChannelDto:
    room = state.rooms.get(room_id)
    if not room:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Room not found")
    channel = state.create_channel(room_id, request.name)
    return ChannelDto(channelId=channel.id, roomId=channel.room_id, name=channel.name)


@router.get("/{room_id}/members", response_model=list[MemberResp])
async def list_members(room_id: int) -> list[MemberResp]:
    members = []
    for membership in state.list_members(room_id):
        user = state.get_user(membership.user_id)
        if not user:
            continue
        members.append(
            MemberResp(
                userId=user.id,
                username=user.username,
                displayName=user.display_name,
                role=membership.role,
            )
        )
    return members


@router.get("/{room_id}/roles", response_model=list[RoleResp])
async def list_roles(room_id: int) -> list[RoleResp]:
    return [
        RoleResp(id=role.id, name=role.name, permissions=role.permissions)
        for role in state.list_roles(room_id)
    ]


@router.post("/{room_id}/roles", response_model=RoleResp, status_code=status.HTTP_201_CREATED)
async def create_role(room_id: int, request: CreateRoleReq) -> RoleResp:
    role = state.create_role(room_id, request.name, request.permissions)
    return RoleResp(id=role.id, name=role.name, permissions=role.permissions)


@router.post("/{room_id}/roles/grant", status_code=status.HTTP_204_NO_CONTENT)
async def grant_role(room_id: int, request: GrantRoleReq) -> Response:
    state.set_member_role(room_id, int(request.userId), request.roleId)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post("/{room_id}/roles/revoke", status_code=status.HTTP_204_NO_CONTENT)
async def revoke_role(room_id: int, request: RevokeRoleReq) -> Response:
    state.set_member_role(room_id, int(request.userId), None)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
