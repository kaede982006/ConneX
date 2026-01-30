from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from connex.infra.db.session import get_db
from connex.domain.models.room import Room, RoomType
from sqlalchemy import select

router = APIRouter()

@router.post("/", status_code=status.HTTP_201_CREATED)
async def create_room(
    room_data: dict, # schema
    db: AsyncSession = Depends(get_db)
):
    # Owner ID should come from current_user
    new_room = Room(
        name=room_data["name"],
        type=room_data.get("type", RoomType.GROUP),
        owner_id=1 # Placeholder
    )
    db.add(new_room)
    await db.commit()
    await db.refresh(new_room)
    return new_room

@router.get("/")
async def get_rooms(
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(Room))
    return result.scalars().all()
