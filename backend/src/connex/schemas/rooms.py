from pydantic import BaseModel, Field
from typing import List, Optional

class CreateRoomRequest(BaseModel):
    title: str

class RoomResponse(BaseModel):
    room_id: int = Field(alias="roomId")
    title: Optional[str]
    owner_id: Optional[int] = Field(alias="ownerId")

    model_config = {"populate_by_name": True}

class CreateChannelRequest(BaseModel):
    name: str

class ChannelResponse(BaseModel):
    channel_id: int = Field(alias="channelId")
    room_id: int = Field(alias="roomId")
    name: str

    model_config = {"populate_by_name": True}

class JoinLeaveResponse(BaseModel):
    status: str

class MemberResponse(BaseModel):
    user_id: int = Field(alias="userId")
    username: str
    display_name: Optional[str] = Field(alias="displayName")
    role: Optional[str]

    model_config = {"populate_by_name": True}

class CreateRoleRequest(BaseModel):
    name: str
    permissions: List[str]

class RoleResponse(BaseModel):
    id: int
    name: str
    permissions: List[str]

    model_config = {"populate_by_name": True}

class GrantRoleRequest(BaseModel):
    user_id: int = Field(alias="userId")
    role_id: int = Field(alias="roleId")

    model_config = {"populate_by_name": True}

class RevokeRoleRequest(BaseModel):
    user_id: int = Field(alias="userId")
    role_id: int = Field(alias="roleId")

    model_config = {"populate_by_name": True}
