import os
from pathlib import Path
from uuid import uuid4

from fastapi import APIRouter, File, HTTPException, UploadFile
from fastapi.responses import FileResponse
from connex.api.schemas import UploadResp
from connex.settings import settings

router = APIRouter()


@router.post("", response_model=UploadResp)
async def upload(file: UploadFile = File(...)) -> UploadResp:
    upload_dir = Path(settings.UPLOAD_DIR)
    upload_dir.mkdir(parents=True, exist_ok=True)
    attachment_id = str(uuid4())
    safe_name = os.path.basename(file.filename or "upload")
    stored_name = f"{attachment_id}_{safe_name}"
    destination = upload_dir / stored_name

    contents = await file.read()
    destination.write_bytes(contents)

    return UploadResp(
        attachmentId=attachment_id,
        downloadUrl=f"{settings.API_V1_STR}/uploads/{attachment_id}",
        mimeType=file.content_type or "application/octet-stream",
        sizeBytes=len(contents),
        name=safe_name,
    )


@router.get("/{attachment_id}")
async def download(attachment_id: str) -> FileResponse:
    upload_dir = Path(settings.UPLOAD_DIR)
    matches = list(upload_dir.glob(f"{attachment_id}_*"))
    if not matches:
        raise HTTPException(status_code=404, detail="Attachment not found")
    return FileResponse(matches[0])
