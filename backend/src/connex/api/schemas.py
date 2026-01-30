from typing import List, Optional
from pydantic import BaseModel


class RegisterReq(BaseModel):
    username: str
    password: str
    displayName: str


class LoginReq(BaseModel):
    username: str
    password: str


class PublicKeyUpsertReq(BaseModel):
    publicKeyPem: str


class AuthResponse(BaseModel):
    accessToken: str
    token_type: str
    userId: int
    username: str


class CreateRoomReq(BaseModel):
    title: str


class RoomDto(BaseModel):
    roomId: int
    title: Optional[str]
    ownerId: Optional[int]


class CreateChannelReq(BaseModel):
    name: str


class ChannelDto(BaseModel):
    channelId: int
    roomId: int
    name: str


class RoomKeyPushReq(BaseModel):
    roomId: int
    channelId: int
    targetUserId: str
    encryptedKeyB64: str


class EncryptedKeyResp(BaseModel):
    encryptedKeyB64: str


class CreateRoleReq(BaseModel):
    name: str
    permissions: List[str]


class RoleResp(BaseModel):
    id: int
    name: str
    permissions: List[str]


class GrantRoleReq(BaseModel):
    userId: str
    roleId: str


class RevokeRoleReq(BaseModel):
    userId: str
    roleId: str


class MemberResp(BaseModel):
    userId: int
    username: str
    displayName: Optional[str]
    role: Optional[str]


class UploadResp(BaseModel):
    attachmentId: str
    downloadUrl: str
    mimeType: str
    sizeBytes: int
    name: str
