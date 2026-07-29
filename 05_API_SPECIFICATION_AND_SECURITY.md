# 05. API Specification, Realtime Protocols & Security Architecture

---

## 1. REST API Endpoints Specification

All REST APIs follow strict OpenAPI 3.0 standards, JSON responses, standard HTTP status codes, and JWT Authentication via `Authorization: Bearer <token>`.

### 1.1 Authentication & Profile (`/api/v1/auth`, `/api/v1/users`)
- `POST /api/v1/auth/register` - Create email account.
- `POST /api/v1/auth/login` - Authenticate & obtain JWT Access + Refresh Token pair.
- `POST /api/v1/auth/refresh` - Rotate expired Access Token using Refresh Token.
- `GET /api/v1/users/me` - Fetch authenticated user profile, badges, and level state.
- `PUT /api/v1/users/me` - Update profile, bio, avatar URL, social handles, and interests.

### 1.2 Communities & Channels (`/api/v1/communities`)
- `GET /api/v1/communities` - List/Search communities with filters (category, trending, query).
- `POST /api/v1/communities` - Create a new community.
- `GET /api/v1/communities/{slug}` - Get community metadata, roles, and channel list.
- `POST /api/v1/communities/{id}/join` - Join community as a member.
- `POST /api/v1/communities/{id}/channels` - Create a channel inside a community.

### 1.3 Feed & Posts (`/api/v1/posts`)
- `GET /api/v1/posts` - Fetch personalized global/community feed (paginated via Cursor ID).
- `POST /api/v1/posts` - Create post (Text, Media, Poll, Code).
- `POST /api/v1/posts/{id}/upvote` - Upvote or toggle vote on post.
- `GET /api/v1/posts/{id}/comments` - Fetch nested comment tree.
- `POST /api/v1/posts/{id}/comments` - Create comment or nested reply.

### 1.4 Events & Gamification (`/api/v1/events`, `/api/v1/gamification`)
- `GET /api/v1/events` - List upcoming community events.
- `POST /api/v1/events/{id}/rsvp` - Submit RSVP status.
- `GET /api/v1/gamification/quests` - List available daily/weekly quests.
- `POST /api/v1/gamification/quests/{id}/claim` - Claim quest completion reward XP.

---

## 2. Realtime WebSocket Protocol Specifications

WebSockets connect over `wss://gateway.hobbyhub.app/ws/v1?token=<JWT_TOKEN>`.

### 2.1 Client-to-Server Events

#### Send Message Event (`CHAT_MESSAGE_SEND`)
```json
{
  "event": "CHAT_MESSAGE_SEND",
  "correlation_id": "req_123456",
  "payload": {
    "channel_id": "4b6c31a7-58b2-4d0d-b873-123456789abc",
    "reply_to_id": null,
    "content": "Hello everyone! Here is my Kotlin snippet.",
    "attachments": [
      {
        "type": "CODE_SNIPPET",
        "language": "kotlin",
        "code": "fun main() { println(\"HobbyHub Rocks!\") }"
      }
    ]
  }
}
```

#### Typing Indicator Event (`CHAT_TYPING`)
```json
{
  "event": "CHAT_TYPING",
  "payload": {
    "channel_id": "4b6c31a7-58b2-4d0d-b873-123456789abc",
    "is_typing": true
  }
}
```

### 2.2 Server-to-Client Events

#### Incoming Message Dispatch (`CHAT_MESSAGE_RECEIVE`)
```json
{
  "event": "CHAT_MESSAGE_RECEIVE",
  "timestamp": 1785239000000,
  "payload": {
    "id": "msg_987654321",
    "channel_id": "4b6c31a7-58b2-4d0d-b873-123456789abc",
    "sender": {
      "id": "usr_112233",
      "username": "alex_dev",
      "display_name": "Alex",
      "avatar_url": "https://cdn.hobbyhub.app/avatars/alex.png",
      "role_badge": { "name": "Verified Expert", "color": "#00CEC9" }
    },
    "content": "Hello everyone! Here is my Kotlin snippet.",
    "attachments": [...],
    "created_at": "2026-07-28T14:15:00Z"
  }
}
```

---

## 3. Security Architecture & Granular RBAC

### 3.1 Bitmask Permission Engine (64 Detailed Flags)
Permissions are stored as a 64-bit integer (`BIGINT`) bitmask for ultra-fast bitwise verification ($O(1)$ operations):

```kotlin
object Permissions {
    const val MANAGE_COMMUNITY: Long = 1L shl 0      // 1 (0x01)
    const val MANAGE_CHANNELS: Long  = 1L shl 1      // 2 (0x02)
    const val MANAGE_ROLES: Long     = 1L shl 2      // 4 (0x04)
    const val KICK_MEMBERS: Long     = 1L shl 3      // 8 (0x08)
    const val BAN_MEMBERS: Long      = 1L shl 4      // 16 (0x10)
    const val DELETE_MESSAGES: Long  = 1L shl 5      // 32 (0x20)
    const val PIN_MESSAGES: Long     = 1L shl 6      // 64 (0x40)
    const val MANAGE_EVENTS: Long    = 1L shl 7      // 128 (0x80)

    fun hasPermission(userBitmask: Long, requiredPermission: Long): Boolean {
        return (userBitmask and requiredPermission) == requiredPermission
    }
}
```

### 3.2 End-to-End Encryption (E2EE) for Direct Messages (Optional Module)
For private 1-on-1 messaging, HobbyHub implements the **Signal Protocol** (Double Ratchet + X3DH key agreement):
1. **Key Generation**: Client generates Identity Keys, Signed Prekeys, and One-Time Prekeys uploaded to server.
2. **Session Initiation**: Initiator fetches recipient's prekey bundle, establishes session locally.
3. **Payload Encryption**: Messages are encrypted client-side before transport. The server acts purely as a blind relay and cannot decrypt message content.

### 3.3 OWASP & Anti-Spam Security Checklist
- **Cloudflare WAF**: DDoS mitigation, IP Reputation filtering, Bot Management.
- **JWT Protection**: Short-lived Access Tokens (15 min) + Refresh Tokens stored in HTTP-Only SameSite Cookies (or Encrypted SharedPreferences on Android).
- **SQL Injection & XSS Shielding**: Prepared statements via Spring Data JPA / Room, strict HTML output sanitization.
- **Rate Limiting**: Sliding-window Redis rate limiter capping requests at 60 requests/min per IP for public APIs.
