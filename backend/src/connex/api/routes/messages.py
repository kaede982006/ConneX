from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from connex.api.schemas import MessageCreate, MessageOut
from connex.domain.models.message import Message
from connex.domain.models.room import Room
from connex.domain.models.user import User
from connex.infra.db.session import get_db

router = APIRouter()


@router.post("/", response_model=MessageOut, status_code=status.HTTP_201_CREATED)
async def send_message(
    message_data: MessageCreate,
    db: AsyncSession = Depends(get_db)
):
    room = await db.scalar(select(Room).where(Room.id == message_data.room_id))
    if not room:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Room not found")

    sender = await db.scalar(select(User).where(User.id == message_data.sender_id))
    if not sender:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Sender not found")

    message = Message(
        room_id=message_data.room_id,
        sender_id=message_data.sender_id,
        content=message_data.content,
        nonce=message_data.nonce,
    )
    db.add(message)
    await db.commit()
    await db.refresh(message)
    return MessageOut.model_validate(message)


@router.get("/room/{room_id}", response_model=list[MessageOut])
async def list_room_messages(
    room_id: int,
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(Message)
        .where(Message.room_id == room_id)
        .order_by(Message.created_at.asc())
    )
    messages = result.scalars().all()
    return [MessageOut.model_validate(message) for message in messages]
