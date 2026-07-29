# 03. Backend & Android Clean Architecture Blueprint

---

## 1. Backend Architecture Blueprint (Kotlin + Spring Boot / NestJS)

The backend follows **Clean Architecture** and **Domain-Driven Design (DDD)** principles configured as a scalable **Modular Monolith** initially, ready for extraction into microservices as traffic scales.

### 1.1 Core Backend Architectural Layers

```
      ┌─────────────────────────────────────────────────────────┐
      │            Presentation / REST / WS API Layer            │
      │        (Controllers, WebSockets, DTO Mappers)           │
      └───────────────────────────┬─────────────────────────────┘
                                  │
      ┌───────────────────────────┴─────────────────────────────┐
      │                   Application Layer                     │
      │       (Use Cases, Commands, Queries, Services)          │
      └───────────────────────────┬─────────────────────────────┘
                                  │
      ┌───────────────────────────┴─────────────────────────────┐
      │                     Domain Layer                        │
      │      (Entities, Value Objects, Domain Events)           │
      └───────────────────────────┬─────────────────────────────┘
                                  │
      ┌───────────────────────────┴─────────────────────────────┐
      │                 Infrastructure Layer                    │
      │   (PostgreSQL JPA Repositories, Redis, S3, FCM, LiveKit) │
      └─────────────────────────────────────────────────────────┘
```

### 1.2 Backend Folder Structure (Kotlin + Spring Boot Monorepo)

```
hobbyhub-backend/
├── build.gradle.kts
├── settings.gradle.kts
├── Dockerfile
├── docker-compose.yml
└── src/
    └── main/
        ├── kotlin/
        │   └── com/
        │       └── hobbyhub/
        │           ├── Application.kt
        │           ├── config/
        │           │   ├── SecurityConfig.kt
        │           │   ├── WebSocketConfig.kt
        │           │   ├── RedisConfig.kt
        │           │   └── OpenApiConfig.kt
        │           ├── domain/                      -- DDD Bounded Contexts
        │           │   ├── auth/
        │           │   │   ├── model/
        │           │   │   ├── repository/
        │           │   │   └── service/
        │           │   ├── community/
        │           │   ├── chat/
        │           │   ├── feed/
        │           │   ├── gamification/
        │           │   ├── event/
        │           │   ├── ai/
        │           │   └── moderation/
        │           ├── infrastructure/
        │           │   ├── persistence/
        │           │   │   ├── entity/              -- JPA Entities
        │           │   │   └── repository/          -- Spring Data JPA
        │           │   ├── realtime/                -- WebSocket Handlers & Redis PubSub
        │           │   ├── search/                  -- Elasticsearch Clients
        │           │   ├── storage/                 -- S3 File Storage
        │           │   └── ai/                      -- Gemini API Client
        │           └── presentation/
        │               ├── rest/                    -- REST Controllers
        │               │   ├── dto/
        │               │   └── controller/
        │               └── websocket/               -- STOMP/WS Endpoints
        └── resources/
            ├── application.yml
            ├── application-dev.yml
            ├── application-prod.yml
            └── db/
                └── migration/                       -- Flyway SQL Scripts
                    ├── V1__init_schema.sql
                    └── V2__add_gamification.sql
```

---

## 2. Android Architecture Blueprint (Kotlin + Jetpack Compose)

The Android application is architected using **Android Clean Architecture**, **MVVM/MVI Patterns**, **Jetpack Compose UI**, and a **Multi-Module Gradle Strategy**.

### 2.1 Multi-Module Gradle Layout Architecture

```
                               ┌───────────────┐
                               │     :app      │
                               └───────┬───────┘
                                       │
            ┌──────────────────────────┼──────────────────────────┐
            │                          │                          │
   ┌────────┴────────┐        ┌────────┴────────┐        ┌────────┴────────┐
   │  :feature:chat  │        │  :feature:feed  │        │:feature:comm... │
   └────────┬────────┘        └────────┬────────┘        └────────┬────────┘
            │                          │                          │
            └──────────────────────────┼──────────────────────────┘
                                       │
            ┌──────────────────────────┼──────────────────────────┐
            │                          │                          │
   ┌────────┴────────┐        ┌────────┴────────┐        ┌────────┴────────┐
   │   :core:model   │        │  :core:network  │        │ :core:database  │
   └─────────────────┘        └─────────────────┘        └─────────────────┘
```

### 2.2 Offline-First Data Synchronization Architecture

The app uses an **Offline-First Room DB Pattern**:
1. UI components observe a `StateFlow` backed by Room Database queries.
2. Network repositories execute background sync via Coroutines.
3. Incoming WebSocket events directly update the Room Local Cache, triggering automatic UI re-renders without full-screen reloads.

---

## 3. Android Project Folder Structure

```
hobbyhub-android/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml                          -- Version Catalog
├── app/
│   └── src/main/java/com/hobbyhub/
│       ├── HobbyHubApp.kt                          -- Application Class & Hilt Setup
│       └── MainActivity.kt                         -- Root Navigation Activity
├── core/
│   ├── model/                                      -- Pure Kotlin Domain Models
│   │   └── src/main/java/com/hobbyhub/core/model/
│   │       ├── User.kt
│   │       ├── Community.kt
│   │       ├── ChatMessage.kt
│   │       └── Post.kt
│   ├── network/                                    -- Ktor / Retrofit & WS Clients
│   │   └── src/main/java/com/hobbyhub/core/network/
│   │       ├── api/
│   │       ├── websocket/
│   │       └── dto/
│   ├── database/                                   -- Room Local Storage Engine
│   │   └── src/main/java/com/hobbyhub/core/database/
│   │       ├── HobbyHubDatabase.kt
│   │       ├── dao/
│   │       └── entity/
│   ├── designsystem/                               -- Material 3 Theme & Tokens
│   │   └── src/main/java/com/hobbyhub/core/designsystem/
│   │       ├── Theme.kt
│   │       ├── Color.kt
│   │       ├── Type.kt
│   │       └── component/                          -- Buttons, Cards, Inputs
│   └── common/                                     -- Dispatchers, Extensions
├── feature/
│   ├── auth/                                       -- Login / Register / Onboarding
│   ├── home/                                       -- Main Dashboard & Explore Tab
│   ├── community/                                  -- Community Details & Channel List
│   ├── chat/                                       -- Realtime Chat & Threads Screen
│   ├── feed/                                       -- Reddit-style Feed & Post Details
│   ├── gamification/                               -- Profile, Badges, Level & Quests
│   ├── event/                                      -- Event Hub & RSVP Management
│   └── voice/                                      -- WebRTC Voice Room Overlay
```

---

## 4. Jetpack Compose State Management Example (MVI)

```kotlin
// ChannelChatViewModel.kt
@HiltViewModel
class ChannelChatViewModel @Inject constructor(
    private val getChannelMessagesUseCase: GetChannelMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChannelChatUiState>(ChannelChatUiState.Loading)
    val uiState: StateFlow<ChannelChatUiState> = _uiState.asStateFlow()

    fun handleIntent(intent: ChannelChatIntent) {
        when (intent) {
            is ChannelChatIntent.LoadMessages -> loadMessages(intent.channelId)
            is ChannelChatIntent.SendMessage -> sendMessage(intent.channelId, intent.text)
            is ChannelChatIntent.AddReaction -> addReaction(intent.messageId, intent.emoji)
        }
    }

    private fun loadMessages(channelId: String) {
        viewModelScope.launch {
            getChannelMessagesUseCase(channelId)
                .catch { e -> _uiState.value = ChannelChatUiState.Error(e.message ?: "Unknown error") }
                .collect { messages ->
                    _uiState.value = ChannelChatUiState.Success(messages = messages)
                }
        }
    }
}
```
