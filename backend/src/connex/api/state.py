from __future__ import annotations

from dataclasses import dataclass, field
from itertools import count
from typing import Dict, List, Optional, Tuple


@dataclass
class UserRecord:
    id: int
    username: str
    password: str
    display_name: str
    public_key_pem: Optional[str] = None


@dataclass
class ChannelRecord:
    id: int
    room_id: int
    name: str


@dataclass
class RoomRecord:
    id: int
    title: Optional[str]
    owner_id: Optional[int]
    channels: Dict[int, ChannelRecord] = field(default_factory=dict)


@dataclass
class RoleRecord:
    id: int
    name: str
    permissions: List[str]


@dataclass
class MembershipRecord:
    user_id: int
    room_id: int
    role: Optional[str]


class InMemoryState:
    def __init__(self) -> None:
        self._user_ids = count(1)
        self._room_ids = count(1)
        self._channel_ids = count(1)
        self._role_ids = count(1)
        self.users: Dict[int, UserRecord] = {}
        self.users_by_name: Dict[str, int] = {}
        self.rooms: Dict[int, RoomRecord] = {}
        self.memberships: Dict[Tuple[int, int], MembershipRecord] = {}
        self.roles: Dict[int, Dict[int, RoleRecord]] = {}
        self.keys: Dict[Tuple[int, int, str], str] = {}
        self.last_user_id: Optional[int] = None

    def create_user(self, username: str, password: str, display_name: str) -> UserRecord:
        user_id = next(self._user_ids)
        record = UserRecord(
            id=user_id,
            username=username,
            password=password,
            display_name=display_name,
        )
        self.users[user_id] = record
        self.users_by_name[username] = user_id
        self.last_user_id = user_id
        return record

    def authenticate(self, username: str, password: str) -> Optional[UserRecord]:
        user_id = self.users_by_name.get(username)
        if not user_id:
            return None
        record = self.users.get(user_id)
        if record and record.password == password:
            self.last_user_id = record.id
            return record
        return None

    def get_user(self, user_id: Optional[int]) -> Optional[UserRecord]:
        if user_id is None:
            return None
        return self.users.get(user_id)

    def upsert_public_key(self, user_id: Optional[int], public_key_pem: str) -> bool:
        record = self.get_user(user_id)
        if not record:
            return False
        record.public_key_pem = public_key_pem
        return True

    def create_room(self, title: Optional[str], owner_id: Optional[int]) -> RoomRecord:
        room_id = next(self._room_ids)
        record = RoomRecord(id=room_id, title=title, owner_id=owner_id)
        self.rooms[room_id] = record
        return record

    def create_channel(self, room_id: int, name: str) -> ChannelRecord:
        channel_id = next(self._channel_ids)
        channel = ChannelRecord(id=channel_id, room_id=room_id, name=name)
        room = self.rooms[room_id]
        room.channels[channel_id] = channel
        return channel

    def ensure_membership(self, user_id: int, room_id: int, role: Optional[str] = None) -> None:
        self.memberships[(user_id, room_id)] = MembershipRecord(
            user_id=user_id,
            room_id=room_id,
            role=role,
        )

    def remove_membership(self, user_id: int, room_id: int) -> None:
        self.memberships.pop((user_id, room_id), None)

    def list_members(self, room_id: int) -> List[MembershipRecord]:
        return [m for m in self.memberships.values() if m.room_id == room_id]

    def create_role(self, room_id: int, name: str, permissions: List[str]) -> RoleRecord:
        role_id = next(self._role_ids)
        role = RoleRecord(id=role_id, name=name, permissions=permissions)
        self.roles.setdefault(room_id, {})[role_id] = role
        return role

    def list_roles(self, room_id: int) -> List[RoleRecord]:
        return list(self.roles.get(room_id, {}).values())

    def set_member_role(self, room_id: int, user_id: int, role_name: Optional[str]) -> None:
        key = (user_id, room_id)
        if key in self.memberships:
            self.memberships[key].role = role_name


state = InMemoryState()
