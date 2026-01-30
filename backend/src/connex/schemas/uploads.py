from pydantic import BaseModel, Field

class UploadResponse(BaseModel):
    attachment_id: str = Field(alias="attachmentId")
    download_url: str = Field(alias="downloadUrl")
    mime_type: str = Field(alias="mimeType")
    size_bytes: int = Field(alias="sizeBytes")
    name: str

    model_config = {"populate_by_name": True}
