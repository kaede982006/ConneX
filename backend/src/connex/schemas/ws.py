from pydantic import BaseModel, Field
from typing import Optional
from connex.security.crypto import Envelope

class IncomingWire(BaseModel):
    type: str
    room_id: Optional[str] = Field(default=None, alias="roomId")
    channel_id: Optional[str] = Field(default=None, alias="channelId")
    message_id: Optional[str] = Field(default=None, alias="messageId")
    envelope: Optional[Envelope] = None
    is_typing: Optional[bool] = Field(default=None, alias="isTyping")

    model_config = {"populate_by_name": True}

class OutgoingWire(BaseModel):
    type: str
    room_id: Optional[str] = Field(default=None, alias="roomId")
    channel_id: Optional[str] = Field(default=None, alias="channelId")
    message_id: Optional[str] = Field(default=None, alias="messageId")
    sender_id: Optional[str] = Field(default=None, alias="senderId")
    envelope: Optional[Envelope] = None
    created_at_ms: Optional[int] = Field(default=None, alias="createdAtMs")
    is_typing: Optional[bool] = Field(default=None, alias="isTyping")
    code: Optional[str] = None
    message: Optional[str] = None

    model_config = {"populate_by_name": True}
