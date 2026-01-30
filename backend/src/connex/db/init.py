from connex.db.base import Base
from connex.db.session import engine
import connex.models  # noqa: F401

async def init_db() -> None:
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
