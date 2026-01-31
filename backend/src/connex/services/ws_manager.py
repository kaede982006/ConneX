from collections import defaultdict
from typing import DefaultDict, Dict, Set, Tuple
from fastapi import WebSocket
import json


class ConnectionManager:
    def __init__(self) -> None:
        self._rooms: DefaultDict[Tuple[str, str], Set[WebSocket]] = defaultdict(set)
        self._users: Dict[WebSocket, int] = {}

    async def connect(self, room_id: str, channel_id: str, websocket: WebSocket, user_id: int) -> None:
        self._rooms[(room_id, channel_id)].add(websocket)
        self._users[websocket] = user_id

    def disconnect(self, room_id: str, channel_id: str, websocket: WebSocket) -> None:
        self._rooms[(room_id, channel_id)].discard(websocket)
        if not self._rooms[(room_id, channel_id)]:
            del self._rooms[(room_id, channel_id)]
        self._users.pop(websocket, None)

    async def broadcast(self, room_id: str, channel_id: str, message: str) -> None:
        for ws in list(self._rooms.get((room_id, channel_id), set())):
            await ws.send_text(message)

    async def disconnect_user(self, room_id: int, user_id: int) -> None:
        room_id_str = str(room_id)
        sockets: Set[WebSocket] = set()
        for (rid, _), members in self._rooms.items():
            if rid == room_id_str:
                sockets.update(members)
        if not sockets:
            return
        payload = json.dumps({"type": "banned", "roomId": room_id_str, "userId": str(user_id)})
        for ws in list(sockets):
            if self._users.get(ws) != user_id:
                continue
            await ws.send_text(payload)
            await ws.close(code=1008)
            self._users.pop(ws, None)
            for key, members in list(self._rooms.items()):
                if ws in members:
                    members.discard(ws)
                    if not members:
                        del self._rooms[key]


manager = ConnectionManager()
