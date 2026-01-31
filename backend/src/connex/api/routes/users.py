from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from connex.core.deps import get_current_user
from connex.db.session import get_session
from connex.models.user import User
from connex.schemas.users import PublicKeyUpsertRequest, UpdateProfileRequest, UserProfileResponse

router = APIRouter()

@router.post("/keys")
async def upsert_public_key(
    request: PublicKeyUpsertRequest,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    current_user.public_key_pem = request.public_key_pem
    session.add(current_user)
    await session.commit()
    return {"status": "ok"}

@router.get("/me", response_model=UserProfileResponse)
async def get_profile(
    current_user: User = Depends(get_current_user),
):
    return UserProfileResponse(
        user_id=current_user.id,
        username=current_user.username,
        display_name=current_user.display_name,
    )

@router.patch("/me", response_model=UserProfileResponse)
async def update_profile(
    request: UpdateProfileRequest,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    display_name = request.display_name.strip()
    if not display_name:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="display name is required")
    current_user.display_name = display_name
    session.add(current_user)
    await session.commit()
    await session.refresh(current_user)
    return UserProfileResponse(
        user_id=current_user.id,
        username=current_user.username,
        display_name=current_user.display_name,
    )
