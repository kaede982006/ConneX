from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from connex.core.deps import get_current_user
from connex.db.session import get_session
from connex.models.user import User
from connex.schemas.users import PublicKeyUpsertRequest

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
