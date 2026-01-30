from fastapi import APIRouter, Header, HTTPException
from connex.api.schemas import EncryptedKeyResp, RoomKeyPushReq
from connex.api.state import state

router = APIRouter()


@router.post("/push", status_code=204)
async def push_channel_key(request: RoomKeyPushReq) -> None:
    key = (request.roomId, request.channelId, request.targetUserId)
    state.keys[key] = request.encryptedKeyB64


@router.get("/pull", response_model=EncryptedKeyResp)
async def pull_channel_key(
    roomId: int,
    channelId: int,
    x_user_id: int | None = Header(default=None, alias="X-User-Id"),
) -> EncryptedKeyResp:
    user_id = x_user_id or state.last_user_id
    if user_id is None:
        raise HTTPException(status_code=401, detail="User not authenticated")
    lookup_key = (roomId, channelId, str(user_id))
    encrypted = state.keys.get(lookup_key)
    if encrypted is None:
        raise HTTPException(status_code=404, detail="Key not found")
    return EncryptedKeyResp(encryptedKeyB64=encrypted)
