from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Enum
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
import enum
from connex.infra.db.base import Base

class RoomType(str, enum.Enum):
    DIRECT = "direct"
    GROUP = "group"

class Room(Base):
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=True) # Null for DM? or auto-generated
    type = Column(String, default=RoomType.GROUP) 
    owner_id = Column(Integer, ForeignKey("user.id"), nullable=True)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())

class Role(str, enum.Enum):
    OWNER = "owner"
    ADMIN = "admin"
    MEMBER = "member"

class Membership(Base):
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("user.id"), nullable=False)
    room_id = Column(Integer, ForeignKey("room.id"), nullable=False)
    role = Column(String, default=Role.MEMBER)
    
    joined_at = Column(DateTime(timezone=True), server_default=func.now())
