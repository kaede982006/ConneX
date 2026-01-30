from pydantic import BaseModel, Field

class RegisterRequest(BaseModel):
    username: str
    password: str
    display_name: str = Field(alias="displayName")

    model_config = {"populate_by_name": True}

class LoginRequest(BaseModel):
    username: str
    password: str

class AuthResponse(BaseModel):
    access_token: str = Field(alias="accessToken")
    token_type: str = "bearer"
    user_id: int = Field(alias="userId")
    username: str

    model_config = {"populate_by_name": True}
