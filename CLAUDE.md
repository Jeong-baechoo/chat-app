# Chat Application - Claude Code Development Guide

## 🚀 Project Overview

This is a **Spring Boot 3.4.3** based real-time chat application implementing **Domain-Driven Design (DDD)** and **Clean Architecture** principles. The application features multi-user chat rooms, real-time messaging, and user authentication with immutable domain objects and proper dependency inversion.

## 🛠 Technology Stack

### Core Framework
- **Java 21** with modern features
- **Spring Boot 3.4.3** (Web, Data JPA, WebSocket, Validation, Actuator)
- **MySQL** with H2 for testing
- **Spring Kafka** for event-driven messaging
- **Redis** for session management and caching

### Testing & Quality
- **JUnit 5** with comprehensive test coverage
- **Mockito** for unit testing
- **TestContainers** for integration testing
- **p6spy** for SQL query analysis and performance monitoring

### Architecture & Design
- **Domain-Driven Design (DDD)** principles
- **Clean Architecture** with proper dependency inversion
- **Immutable domain objects** with static factory methods
- **Value Objects** for type safety (e.g., ChatRoomName)
- **Domain Services** for complex business logic

## 🏗 Project Structure

```
src/main/java/com/example/chatapp/
├── ChatAppApplication.java                    # Spring Boot main class
├── config/                                   # Configuration classes
│   ├── KafkaConfig.java                  # Message queue configuration
│   ├── WebConfig.java                       # Web MVC configuration
│   ├── WebFilterConfig.java                 # Filter chain configuration
│   └── WebSocketConfig.java                 # WebSocket configuration
├── controller/                              # REST API controllers
│   ├── AuthController.java                 # Authentication endpoints
│   ├── ChatRoomController.java             # Chat room management
│   ├── MessageController.java              # Message operations
│   ├── UserController.java                 # User management
│   └── WebSocketController.java            # WebSocket message handling
├── domain/                                  # Domain layer (DDD core)
│   ├── ChatRoom.java                       # Chat room aggregate root
│   ├── ChatRoomParticipant.java           # Participant entity
│   ├── Message.java                        # Message entity
│   ├── User.java                          # User aggregate root
│   ├── [ValueObjects & Enums]             # ChatRoomType, MessageStatus, etc.
│   └── service/                           # Domain services
│       ├── ChatRoomDomainService.java     # Chat room business logic
│       └── MessageDomainService.java      # Message business logic
├── dto/                                    # Data Transfer Objects
│   ├── request/                           # API request DTOs
│   └── response/                          # API response DTOs
├── event/                                 # Domain events
│   ├── ChatEventListener.java            # Event handling
│   └── MessageCreatedEvent.java          # Message creation event
├── exception/                             # Exception handling
│   └── GlobalExceptionHandler.java       # Centralized error handling
├── infrastructure/                        # Infrastructure layer
│   ├── auth/                             # Authentication infrastructure
│   ├── filter/                           # Servlet filters
│   └── session/                          # Session management
├── mapper/                               # Entity-DTO mapping
├── repository/                           # Data access layer
└── service/                             # Application services
    └── impl/                            # Service implementations
```

## 🔧 Development Commands

### Build & Test
```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.example.chatapp.service.unit.message.MessageServiceTest"

# Run tests with verbose output
./gradlew test --info
```

### Application Startup
```bash
# Run with development profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run with test profile
./gradlew bootRun --args='--spring.profiles.active=test'
```

### Database Management
```bash
# Access H2 console (test profile)
# URL: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:testdb
# Username: sa
# Password: (empty)
```

## 🎯 Key Domain Concepts

### Domain Entities (Immutable)
- **ChatRoom**: Aggregate root managing participants and room settings
- **User**: User aggregate with authentication and profile data
- **Message**: Immutable message entity with content and metadata
- **ChatRoomParticipant**: Association entity with role management

### Value Objects
- **ChatRoomName**: Type-safe chat room naming
- **ChatRoomType**: Enum for PRIVATE/PUBLIC room types
- **ParticipantRole**: ADMIN/MEMBER role management
- **MessageStatus**: SENT/READ status tracking

### Domain Services
- **ChatRoomDomainService**: Complex chat room operations (invite, join, leave)
- **MessageDomainService**: Message validation and sending logic

## 🔍 Testing Strategy

### Unit Tests
- **Service Layer**: `src/test/java/com/example/chatapp/service/unit/`
- **Domain Layer**: `src/test/java/com/example/chatapp/domain/`
- **Repository Layer**: `src/test/java/com/example/chatapp/repository/`

### Integration Tests
- **Controller Tests**: `src/test/java/com/example/chatapp/controller/`
- **Full Integration**: `src/test/java/com/example/chatapp/service/integration/`

### Test Configuration
- **Profiles**: Use `test` profile for H2 database
- **Slicing**: `@WebMvcTest` for controller tests without security filters
- **Mocking**: Comprehensive Mockito setup for service layer isolation

## ⚡ Performance Optimizations

### Database Optimization
- **FETCH JOIN** queries to prevent N+1 problems
- **EntityGraph** for complex object loading
- **p6spy** integration for query performance monitoring
- **HikariCP** connection pool tuning

### Application Performance
- **EntityFinderService** pattern for batch entity lookups
- **Stream API** for efficient data processing
- **Immutable objects** for thread safety and performance

## 🛡 Architecture Principles

### Clean Architecture Compliance
1. **Domain Layer**: No external dependencies, pure business logic
2. **Application Layer**: Orchestrates domain objects and infrastructure
3. **Infrastructure Layer**: Implements technical concerns (DB, web, etc.)
4. **Dependency Inversion**: Dependencies point inward toward domain

### DDD Implementation
- **Aggregates**: ChatRoom and User as aggregate roots
- **Domain Services**: Complex business logic spanning multiple entities
- **Value Objects**: Type safety and domain modeling
- **Domain Events**: Decoupled communication between bounded contexts

## 🚨 Important Notes

### Recent Architectural Changes
- **Domain Immutability**: All entities use static factory methods (`create()`)
- **Internal Methods**: Domain modification methods are `*Internal()` and package-private
- **Domain Services**: All complex operations go through domain services
- **Clean Boundaries**: Strict dependency direction enforcement

### Testing Considerations
- **Mock Strategy**: Use `@Mock` with careful stubbing to avoid `UnnecessaryStubbingException`
- **Domain Testing**: Test domain services, not internal entity methods directly
- **Integration Testing**: Full stack testing with TestContainers when needed

### Performance Monitoring
- **p6spy**: Enabled in development for SQL query analysis
- **Actuator**: Health checks and metrics endpoints available
- **Logging**: Comprehensive logging with Logback configuration

## 📚 Documentation

Detailed documentation for major improvements and architectural decisions can be found in:
- `portfolio-docs/` - Portfolio documentation of improvements
- Each improvement includes before/after analysis and performance metrics

## 🔄 Development Workflow

1. **Domain First**: Start with domain modeling and business rules
2. **Test Driven**: Write tests before implementation
3. **Clean Architecture**: Respect dependency boundaries
4. **Performance**: Monitor with p6spy and optimize queries
5. **Documentation**: Update this guide when making architectural changes

---

**Note**: This application demonstrates enterprise-level Java development with modern Spring Boot, DDD principles, and comprehensive testing strategies. The codebase serves as a reference for building scalable, maintainable chat applications.
