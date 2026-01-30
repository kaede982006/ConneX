from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Text
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from connex.infra.db.base import Base

class Message(Base):
    id = Column(Integer, primary_key=True, index=True)
    room_id = Column(Integer, ForeignKey("room.id"), nullable=False)
    sender_id = Column(Integer, ForeignKey("user.id"), nullable=False)
    
    # Encrypted Content
    content = Column(Text, nullable=False) # Ciphertext
    nonce = Column(String, nullable=True) # IV/Nonce for encryption
    
    created_at = Column(DateTime(timezone=True), server_default=func.now())
