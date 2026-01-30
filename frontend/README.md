ConneX Android

목표
- ConneX는 디스코드와 유사한 오픈채팅 기반 메신저 프론트엔드(Android) 프로젝트이다.
- 전송 구간은 TLS(HTTPS/WSS) 전제이며, 메시지 내용은 AES-256-GCM(클라이언트 측)으로 암호화한 Envelope 형태로 서버에 전달/저장/중계되는 구조를 기본으로 한다.
- 방(=Room) 내부에 채널(Channel)이 있으며, 채팅은 채널 단위로 동작한다.
- 파일 첨부는 업로드 후 첨부 메타데이터를 메시지 payload에 포함하여 동일하게 암호화한다.

실행
1) Android Studio로 ConneX-Android 폴더를 Open 한다.
2) app/build.gradle.kts 의 BuildConfig.API_BASE_URL / WS_BASE_URL을 서버 주소로 변경한다.
3) backend는 다음을 제공해야 한다(기본 가정):
   - POST /api/v1/auth/register
   - POST /api/v1/auth/login
   - POST /api/v1/crypto/public-key
   - GET  /api/v1/rooms
   - POST /api/v1/rooms
   - POST /api/v1/rooms/{roomId}/join
   - POST /api/v1/rooms/{roomId}/leave
   - GET  /api/v1/rooms/{roomId}/channels
   - POST /api/v1/rooms/{roomId}/channels
   - GET  /api/v1/rooms/{roomId}/keys/pull?channelId=...
   - POST /api/v1/rooms/keys/push
   - POST /api/v1/uploads (multipart)
   - WSS /ws/chat?room_id=...&channel_id=...&token=...

주의
- 서버 계약(API path/필드명)이 다르면 data/remote 쪽 DTO 및 Api 인터페이스만 맞춰서 수정하면 된다.
