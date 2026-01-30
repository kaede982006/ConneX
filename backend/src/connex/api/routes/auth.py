from fastapi import APIRouter, HTTPException, status
from connex.api.schemas import AuthResponse, LoginReq, PublicKeyUpsertReq, RegisterReq
from connex.api.state import state

router = APIRouter()


@router.post("/register", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
async def register(request: RegisterReq) -> AuthResponse:
    if request.username in state.users_by_name:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Username already registered",
        )

    user = state.create_user(request.username, request.password, request.displayName)
    return AuthResponse(
        accessToken=f"token-{user.id}",
        token_type="bearer",
        userId=user.id,
        username=user.username,
    )


@router.post("/login", response_model=AuthResponse)
async def login(request: LoginReq) -> AuthResponse:
    user = state.authenticate(request.username, request.password)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
        )
    return AuthResponse(
        accessToken=f"token-{user.id}",
        token_type="bearer",
        userId=user.id,
        username=user.username,
    )
