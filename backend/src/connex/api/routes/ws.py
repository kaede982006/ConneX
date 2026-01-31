import time
from datetime import datetime
from fastapi import APIRouter, Depends, WebSocket, WebSocketDisconnect
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from connex.core.deps import get_current_user
from connex.db.session import get_session
from connex.models.room_ban import RoomBan
from connex.models.room_member import RoomMember
from connex.models.user import User
from connex.schemas.ws import IncomingWire, OutgoingWire
from connex.security.auth import decode_access_token
from connex.services.ws_manager import manager

router = APIRouter()

@router.websocket("/chat")
async def chat_ws(websocket: WebSocket):
    session_gen = get_session()
    session = await session_gen.__anext__()
    print(f"WS DEBUG: Connected. Params: {websocket.query_params}")
    token = websocket.query_params.get("token")
    room_id = websocket.query_params.get("room_id")
    channel_id = websocket.query_params.get("channel_id")
    
    print(f"WS DEBUG: Token: {token}")
    user_id = decode_access_token(token) if token else None
    print(f"WS DEBUG: Decoded UserID: {user_id}")
    
    if user_id is None or room_id is None or channel_id is None:
        print("WS DEBUG: Validation Failed (Missing params or token)")
        await websocket.close(code=1008)
        return
    try:
        room_id_int = int(room_id)
        channel_id_int = int(channel_id)
        user_id_int = int(user_id)
    except ValueError:
        print("WS DEBUG: ValueError in params")
        await websocket.close(code=1008)
        return

    membership = await session.execute(
        select(RoomMember).where(RoomMember.room_id == room_id_int, RoomMember.user_id == user_id_int)
    )
    ban_result = await session.execute(
        select(RoomBan).where(RoomBan.room_id == room_id_int, RoomBan.user_id == user_id_int)
    )
    if ban_result.scalar_one_or_none() is not None:
        print("WS DEBUG: User Banned")
        await websocket.close(code=1008)
        return
    if membership.scalar_one_or_none() is None:
        print("WS DEBUG: Membership Check Failed")
        await websocket.close(code=1008)
        return
    user_result = await session.execute(select(User).where(User.id == user_id_int))
    user = user_result.scalar_one_or_none()
    sender_name = user.display_name or user.username if user else str(user_id_int)

    print("WS DEBUG: Connecting to manager")
    await websocket.accept()
    await manager.connect(room_id, channel_id, websocket, user_id_int)
    try:
        while True:
            raw = await websocket.receive_text()
            incoming = IncomingWire.model_validate_json(raw)
            if incoming.type == "send_message":
                outgoing = OutgoingWire(
                    type="message",
                    room_id=room_id,
                    channel_id=channel_id,
                    message_id=incoming.message_id,
                    sender_id=str(user_id_int),
                    sender_name=sender_name,
                    envelope=incoming.envelope,
                    created_at_ms=int(time.time() * 1000),
                )
                from connex.models.message import Message

                db_msg = Message(
                    id=incoming.message_id,
                    room_id=room_id_int,
                    channel_id=channel_id_int,
                    sender_id=user_id_int,
                    content=incoming.envelope.model_dump_json(by_alias=True),
                    created_at=datetime.utcnow(),
                )
                session.add(db_msg)
                await session.commit()
                await manager.broadcast(room_id, channel_id, outgoing.model_dump_json(by_alias=True))
            elif incoming.type == "typing":
                outgoing = OutgoingWire(
                    type="typing",
                    room_id=room_id,
                    channel_id=channel_id,
                    sender_id=str(user_id_int),
                    sender_name=sender_name,
                    is_typing=incoming.is_typing,
                )
                await manager.broadcast(room_id, channel_id, outgoing.model_dump_json(by_alias=True))
    except WebSocketDisconnect:
        manager.disconnect(room_id, channel_id, websocket)
        return
    except Exception:
        manager.disconnect(room_id, channel_id, websocket)
        await websocket.send_text(
            OutgoingWire(type="system", code="WS_ERR", message="websocket error").model_dump_json(by_alias=True)
        )
