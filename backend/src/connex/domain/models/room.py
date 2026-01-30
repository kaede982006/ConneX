from sqlalchemy import Column, DateTime, Enum, ForeignKey, Integer, String
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
import enum
from connex.infra.db.base import Base

class RoomType(str, enum.Enum):
    DIRECT = "direct"
    GROUP = "group"

class Room(Base):
    __tablename__ = "rooms"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=True) # Null for DM? or auto-generated
    type = Column(Enum(RoomType), default=RoomType.GROUP)
    owner_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    memberships = relationship("Membership", back_populates="room", cascade="all, delete-orphan")
    messages = relationship("Message", back_populates="room", cascade="all, delete-orphan")

class Role(str, enum.Enum):
    OWNER = "owner"
    ADMIN = "admin"
    MEMBER = "member"

class Membership(Base):
    __tablename__ = "memberships"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    room_id = Column(Integer, ForeignKey("rooms.id"), nullable=False)
    role = Column(Enum(Role), default=Role.MEMBER)
    
    joined_at = Column(DateTime(timezone=True), server_default=func.now())

    user = relationship("User")
    room = relationship("Room", back_populates="memberships")
