from pydantic import BaseModel, Field

class RoomKeyPushRequest(BaseModel):
    room_id: int = Field(alias="roomId")
    channel_id: int = Field(alias="channelId")
    target_user_id: str = Field(alias="targetUserId")
    encrypted_key_b64: str = Field(alias="encryptedKeyB64")

    model_config = {"populate_by_name": True}

class EncryptedKeyResponse(BaseModel):
    encrypted_key_b64: str = Field(alias="encryptedKeyB64")

    model_config = {"populate_by_name": True}
