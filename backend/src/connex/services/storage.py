from pathlib import Path
import uuid
from connex.settings import settings

class StorageService:
    def __init__(self) -> None:
        self.base = Path(settings.UPLOAD_DIR)
        self.base.mkdir(parents=True, exist_ok=True)

    def save(self, filename: str, data: bytes) -> tuple[str, str]:
        ext = Path(filename).suffix
        attachment_id = str(uuid.uuid4())
        path = self.base / f"{attachment_id}{ext}"
        path.write_bytes(data)
        return attachment_id, str(path)
