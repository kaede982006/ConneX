import time
from fastapi import APIRouter, Depends, WebSocket, WebSocketDisconnect
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from connex.core.deps import get_current_user
from connex.db.session import get_session
from connex.models.room_member import RoomMember
from connex.schemas.ws import IncomingWire, OutgoingWire
from connex.security.auth import decode_access_token
from connex.services.ws_manager import ConnectionManager

router = APIRouter()
manager = ConnectionManager()

@router.websocket("/chat")
async def chat_ws(websocket: WebSocket, session: AsyncSession = Depends(get_session)):
    token = websocket.query_params.get("token")
    room_id = websocket.query_params.get("room_id")
    channel_id = websocket.query_params.get("channel_id")
    user_id = decode_access_token(token) if token else None
    if user_id is None:
        await websocket.close(code=1008)
        return

    membership = await session.execute(
        select(RoomMember).where(RoomMember.room_id == int(room_id), RoomMember.user_id == int(user_id))
    )
    if membership.scalar_one_or_none() is None:
        await websocket.close(code=1008)
        return

    await manager.connect(room_id, channel_id, websocket)
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
                    sender_id=str(user_id),
                    envelope=incoming.envelope,
                    created_at_ms=int(time.time() * 1000),
                )
                await manager.broadcast(room_id, channel_id, outgoing.model_dump_json(by_alias=True))
            elif incoming.type == "typing":
                outgoing = OutgoingWire(
                    type="typing",
                    room_id=room_id,
                    channel_id=channel_id,
                    sender_id=str(user_id),
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
