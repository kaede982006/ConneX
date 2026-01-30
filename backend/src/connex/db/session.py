from collections.abc import AsyncGenerator
from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from connex.settings import settings

db_ready = True


def set_db_ready(ready: bool) -> None:
    global db_ready
    db_ready = ready


def is_db_ready() -> bool:
    return db_ready

engine = create_async_engine(settings.SQLALCHEMY_DATABASE_URI, echo=False, future=True)
SessionLocal = async_sessionmaker(engine, expire_on_commit=False, class_=AsyncSession)

async def get_session() -> AsyncGenerator[AsyncSession, None]:
    if not is_db_ready():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="database unavailable",
        )
    async with SessionLocal() as session:
        yield session
