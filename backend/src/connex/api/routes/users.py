from fastapi import APIRouter, Header, HTTPException, Response, status
from connex.api.schemas import PublicKeyUpsertReq
from connex.api.state import state

router = APIRouter()


@router.post("/keys", status_code=status.HTTP_204_NO_CONTENT)
async def upsert_public_key(
    request: PublicKeyUpsertReq,
    x_user_id: int | None = Header(default=None, alias="X-User-Id"),
) -> Response:
    user_id = x_user_id or state.last_user_id
    if not state.upsert_public_key(user_id, request.publicKeyPem):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found",
        )
    return Response(status_code=status.HTTP_204_NO_CONTENT)
