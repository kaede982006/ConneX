from fastapi import APIRouter, Depends, File, HTTPException, UploadFile
from fastapi.responses import FileResponse
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from connex.core.deps import get_current_user
from connex.db.session import get_session
from connex.models.attachment import Attachment
from connex.models.user import User
from connex.schemas.uploads import UploadResponse
from connex.services.storage import StorageService

router = APIRouter()

@router.post("", response_model=UploadResponse)
async def upload_file(
    file: UploadFile = File(...),
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    data = await file.read()
    storage = StorageService()
    attachment_id, path = storage.save(file.filename, data)
    attachment = Attachment(
        id=attachment_id,
        uploader_id=current_user.id,
        name=file.filename,
        mime_type=file.content_type or "application/octet-stream",
        size_bytes=len(data),
        storage_path=path,
    )
    session.add(attachment)
    await session.commit()

    return UploadResponse(
        attachment_id=attachment.id,
        download_url=f"/api/v1/uploads/{attachment.id}",
        mime_type=attachment.mime_type,
        size_bytes=attachment.size_bytes,
        name=attachment.name,
    )

@router.get("/{attachment_id}")
async def download_file(
    attachment_id: str,
    session: AsyncSession = Depends(get_session),
    current_user: User = Depends(get_current_user),
):
    result = await session.execute(select(Attachment).where(Attachment.id == attachment_id))
    attachment = result.scalar_one_or_none()
    if attachment is None:
        raise HTTPException(status_code=404, detail="attachment not found")
    return FileResponse(path=attachment.storage_path, media_type=attachment.mime_type, filename=attachment.name)
