import asyncio
import logging

from connex.db.base import Base
from connex.db.session import engine
from connex.settings import settings
import connex.models  # noqa: F401

logger = logging.getLogger(__name__)

async def init_db() -> None:
    last_error: Exception | None = None
    for attempt in range(1, settings.DB_INIT_RETRIES + 1):
        try:
            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all)
            return
        except Exception as exc:
            last_error = exc
            if attempt >= settings.DB_INIT_RETRIES:
                break
            logger.warning(
                "Database init failed (attempt %s/%s). Retrying in %.1fs.",
                attempt,
                settings.DB_INIT_RETRIES,
                settings.DB_INIT_RETRY_DELAY_SECONDS,
            )
            await asyncio.sleep(settings.DB_INIT_RETRY_DELAY_SECONDS)

    assert last_error is not None
    logger.exception("Database init failed after %s attempts.", settings.DB_INIT_RETRIES)
    raise last_error
