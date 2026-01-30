from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession
from connex.core.deps import get_current_user
from connex.db.session import get_session
from connex.models.room_key import RoomKey
from connex.schemas.keys import EncryptedKeyResponse, RoomKeyPushRequest
from connex.models.user import User

router = APIRouter()

@router.post("/push")
async def push_key(
    request: RoomKeyPushRequest,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    await session.execute(
        delete(RoomKey).where(
            RoomKey.room_id == request.room_id,
            RoomKey.channel_id == request.channel_id,
            RoomKey.target_user_id == request.target_user_id,
        )
    )
    entry = RoomKey(
        room_id=request.room_id,
        channel_id=request.channel_id,
        target_user_id=request.target_user_id,
        encrypted_key_b64=request.encrypted_key_b64,
    )
    session.add(entry)
    await session.commit()
    return {"status": "ok"}

@router.get("/pull", response_model=EncryptedKeyResponse)
async def pull_key(
    room_id: int,
    channel_id: int,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    result = await session.execute(
        select(RoomKey)
        .where(
            RoomKey.room_id == room_id,
            RoomKey.channel_id == channel_id,
            RoomKey.target_user_id == current_user.id,
        )
    )
    key = result.scalar_one_or_none()
    if key is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="key not found")
    return EncryptedKeyResponse(encrypted_key_b64=key.encrypted_key_b64)
