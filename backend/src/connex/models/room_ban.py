from datetime import datetime
from sqlalchemy import DateTime, ForeignKey, Integer, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column
from connex.db.base import Base


class RoomBan(Base):
    __tablename__ = "room_bans"
    __table_args__ = (
        UniqueConstraint("room_id", "user_id", name="uq_room_ban"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    room_id: Mapped[int] = mapped_column(Integer, ForeignKey("rooms.id"), nullable=False)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False)
    banned_by_user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False)
    banned_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
