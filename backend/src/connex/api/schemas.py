from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict

from connex.domain.models.room import RoomType


class UserCreate(BaseModel):
    username: str
    password: str
    public_key: Optional[str] = None


class UserLogin(BaseModel):
    username: str
    password: str


class RoomCreate(BaseModel):
    name: Optional[str] = None
    type: RoomType = RoomType.GROUP


class RoomOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: Optional[str]
    type: RoomType
    owner_id: Optional[int]
    created_at: datetime


class MessageCreate(BaseModel):
    room_id: int
    sender_id: int
    content: str
    nonce: Optional[str] = None


class MessageOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    room_id: int
    sender_id: int
    content: str
    nonce: Optional[str]
    created_at: datetime
