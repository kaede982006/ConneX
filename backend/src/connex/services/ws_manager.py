from collections import defaultdict
from typing import DefaultDict, Set, Tuple
from fastapi import WebSocket


class ConnectionManager:
    def __init__(self) -> None:
        self._rooms: DefaultDict[Tuple[str, str], Set[WebSocket]] = defaultdict(set)

    async def connect(self, room_id: str, channel_id: str, websocket: WebSocket) -> None:
        self._rooms[(room_id, channel_id)].add(websocket)

    def disconnect(self, room_id: str, channel_id: str, websocket: WebSocket) -> None:
        self._rooms[(room_id, channel_id)].discard(websocket)
        if not self._rooms[(room_id, channel_id)]:
            del self._rooms[(room_id, channel_id)]

    async def broadcast(self, room_id: str, channel_id: str, message: str) -> None:
        for ws in list(self._rooms.get((room_id, channel_id), set())):
            await ws.send_text(message)
