from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from connex.api.schemas import UserCreate, UserLogin
from connex.domain.models.user import User
from connex.infra.db.session import get_db

router = APIRouter()

@router.post("/register", status_code=status.HTTP_201_CREATED)
async def register(
    user_data: UserCreate,
    db: AsyncSession = Depends(get_db)
):
    # Check duplicate
    stmt = select(User).where(User.username == user_data.username)
    result = await db.execute(stmt)
    if result.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Username already registered"
        )
    
    # Create User
    new_user = User(
        username=user_data.username,
        hashed_password=user_data.password,
        identity_key=user_data.public_key
    )
    db.add(new_user)
    await db.commit()
    await db.refresh(new_user)
    return {"user_id": new_user.id, "username": new_user.username}

@router.post("/login")
async def login(
    user_data: UserLogin,
    db: AsyncSession = Depends(get_db)
):
    stmt = select(User).where(User.username == user_data.username)
    result = await db.execute(stmt)
    user = result.scalar_one_or_none()
    
    if not user or user.hashed_password != user_data.password:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password"
        )
        
    return {
        "access_token": "jwt_token_placeholder", 
        "token_type": "bearer",
        "user_id": user.id
    }
