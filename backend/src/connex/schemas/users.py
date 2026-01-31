from pydantic import BaseModel, Field
from typing import Optional

class PublicKeyUpsertRequest(BaseModel):
    public_key_pem: str = Field(alias="publicKeyPem")

    model_config = {"populate_by_name": True}

class UserProfileResponse(BaseModel):
    user_id: int = Field(alias="userId")
    username: str
    display_name: Optional[str] = Field(alias="displayName")

    model_config = {"populate_by_name": True}

class UpdateProfileRequest(BaseModel):
    display_name: str = Field(alias="displayName")

    model_config = {"populate_by_name": True}
