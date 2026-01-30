from pydantic import BaseModel, Field

class Envelope(BaseModel):
    alg: str
    nonce_b64: str = Field(alias="nonceB64")
    cipher_b64: str = Field(alias="cipherB64")
    aad_b64: str = Field(alias="aadB64")

    model_config = {"populate_by_name": True}
