from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from connex.db.session import get_session
from connex.models.user import User
from connex.schemas.auth import AuthResponse, LoginRequest, RegisterRequest
from connex.security.auth import create_access_token, hash_password, verify_password

router = APIRouter()

@router.post("/register", response_model=AuthResponse)
async def register(request: RegisterRequest, session: AsyncSession = Depends(get_session)):
    result = await session.execute(select(User).where(User.username == request.username))
    if result.scalar_one_or_none() is not None:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="username already exists")
    user = User(
        username=request.username,
        password_hash=hash_password(request.password),
        display_name=request.display_name,
    )
    session.add(user)
    await session.commit()
    await session.refresh(user)
    token = create_access_token(str(user.id))
    return AuthResponse(access_token=token, user_id=user.id, username=user.username)

@router.post("/login", response_model=AuthResponse)
async def login(request: LoginRequest, session: AsyncSession = Depends(get_session)):
    result = await session.execute(select(User).where(User.username == request.username))
    user = result.scalar_one_or_none()
    if user is None or not verify_password(request.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid credentials")
    token = create_access_token(str(user.id))
    return AuthResponse(access_token=token, user_id=user.id, username=user.username)
