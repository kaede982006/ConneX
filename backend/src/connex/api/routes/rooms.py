from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from connex.api.schemas import RoomCreate, RoomOut
from connex.domain.models.room import Room
from connex.infra.db.session import get_db

router = APIRouter()

@router.post("/", status_code=status.HTTP_201_CREATED)
async def create_room(
    room_data: RoomCreate,
    db: AsyncSession = Depends(get_db)
):
    # Owner ID should come from current_user
    new_room = Room(
        name=room_data.name,
        type=room_data.type,
        owner_id=1 # Placeholder
    )
    db.add(new_room)
    await db.commit()
    await db.refresh(new_room)
    return RoomOut.model_validate(new_room)

@router.get("/")
async def get_rooms(
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(Room))
    rooms = result.scalars().all()
    return [RoomOut.model_validate(room) for room in rooms]
