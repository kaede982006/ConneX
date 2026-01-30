from pydantic import BaseModel, Field

class PublicKeyUpsertRequest(BaseModel):
    public_key_pem: str = Field(alias="publicKeyPem")

    model_config = {"populate_by_name": True}
