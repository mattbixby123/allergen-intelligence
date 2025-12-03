# Allergen Intelligence Platform - System Architecture

## Table of Contents
- [Overview](#overview)
- [High-Level Architecture](#high-level-architecture)
- [Component Diagram](#component-diagram)
- [Product Analysis Flow](#product-analysis-flow)
- [Three-Tier Caching Strategy](#three-tier-caching-strategy)
- [Authentication Flow](#authentication-flow)
- [Database Schema](#database-schema)
- [Technology Stack](#technology-stack)
- [Performance Characteristics](#performance-characteristics)

## Overview

The Allergen Intelligence Platform is a production-grade REST API that translates product ingredient labels into comprehensive allergen safety reports. The system uses a sophisticated three-tier caching strategy to minimize API costs while maintaining fast response times.

**Key Innovation:** Automatic identification of allergenic oxidation products (e.g., Limonene → Limonene hydroperoxide) that traditional ingredient searches miss.

## High-Level Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        A[React Frontend<br/>Future Implementation]
    end
    
    subgraph "API Layer"
        B[Spring Boot REST API<br/>Port 8080]
        C[JWT Authentication<br/>Access + Refresh Tokens]
    end
    
    subgraph "Service Layer"
        D[AllergenSearch Service]
        E[PubChem Service]
        F[OpenAI Service]
        G[Vector Store Service]
        H[Usage Tracking Service]
    end
    
    subgraph "Data Layer"
        I[(PostgreSQL 17<br/>Primary Database)]
        J[pgvector Extension<br/>Semantic Cache]
    end
    
    subgraph "External APIs"
        K[PubChem REST API<br/>Chemical Data]
        L[OpenAI GPT-4o<br/>Web Search + LLM]
    end
    
    A -->|HTTPS/REST| B
    B --> C
    B --> D
    D --> E
    D --> F
    D --> G
    D --> H
    E --> K
    F --> L
    G --> J
    D --> I
    H --> I
    
    style A fill:#e1f5ff
    style B fill:#fff4e1
    style I fill:#f0f0f0
    style L fill:#ffe1e1
    style K fill:#ffe1e1
```

## Component Diagram

```mermaid
graph LR
    subgraph "Controllers"
        A1[AuthController<br/>/api/auth/*]
        A2[AllergenSearchController<br/>/api/allergen/*]
        A3[TestController<br/>/actuator/*]
    end
    
    subgraph "Services"
        B1[AuthService]
        B2[JwtService]
        B3[PubChemService]
        B4[OpenAISearchService]
        B5[VectorStoreService]
        B6[UsageTrackingService]
    end
    
    subgraph "Repositories"
        C1[UserRepository]
        C2[RefreshTokenRepository]
        C3[ChemicalRepository]
        C4[SideEffectRepository]
        C5[UsageTrackingRepository]
    end
    
    subgraph "Security"
        D1[JwtAuthenticationFilter]
        D2[SecurityConfig]
    end
    
    A1 --> B1
    A1 --> B2
    A2 --> B3
    A2 --> B4
    A2 --> B5
    A2 --> B6
    
    B1 --> C1
    B1 --> C2
    B3 --> C3
    B4 --> C4
    B6 --> C5
    
    D1 --> B2
    D2 --> D1
    
    style A1 fill:#e8f5e9
    style A2 fill:#e8f5e9
    style B4 fill:#fff3e0
    style B5 fill:#f3e5f5
```

## Product Analysis Flow

This is the complete end-to-end flow when a user requests product analysis:

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant Controller as AllergenSearchController
    participant Service as OpenAISearchService
    participant PubChem as PubChemService
    participant DBCache as Database Cache
    participant VectorCache as Vector Cache
    participant OpenAI as OpenAI API
    participant Tracking as UsageTrackingService
    
    User->>Frontend: "Analyze CeraVe Cream"
    Frontend->>Controller: POST /api/allergen/analyze-product<br/>{productName: "CeraVe Cream"}
    
    Note over Controller,Service: Step 1: Extract Ingredients
    Controller->>Service: analyzeProduct()
    Service->>OpenAI: Extract ingredient list
    OpenAI-->>Service: ["Cetearyl Alcohol", "Glycerin", ...]
    Service->>Tracking: Track tokens (100 tokens)
    
    Note over Service,OpenAI: Step 2: Analyze Each Ingredient
    loop For each ingredient
        Service->>DBCache: findByCommonName("Cetearyl Alcohol")
        
        alt Database Cache Hit
            DBCache-->>Service: Return Chemical + SideEffects
            Note over Service: ✅ <10ms, 0 tokens
        else Database Cache Miss
            Service->>VectorCache: semanticSearch("Cetearyl Alcohol")
            
            alt Vector Cache Hit
                VectorCache-->>Service: Return cached analysis
                Service->>DBCache: Save to database
                Note over Service: ✅ ~100ms, 0 tokens
            else Vector Cache Miss
                Service->>PubChem: searchChemical("Cetearyl Alcohol")
                PubChem-->>Service: CAS, IUPAC, SMILES
                Service->>DBCache: Save chemical data
                
                Service->>OpenAI: Search allergen effects
                OpenAI-->>Service: Side effects data
                Service->>Tracking: Track tokens (~200 tokens)
                
                Service->>OpenAI: Search oxidation products
                OpenAI-->>Service: Oxidation compounds
                Service->>Tracking: Track tokens (~200 tokens)
                
                Service->>DBCache: Save side effects
                Service->>VectorCache: Cache for future queries
                Note over Service: ⚠️ 5-10s, ~400 tokens
            end
        end
    end
    
    Service-->>Controller: ProductAnalysisResponse
    Controller-->>Frontend: JSON Response
    Frontend-->>User: Display safety report
    
    Note over User,Tracking: First Analysis: ~4,100 tokens<br/>Repeat Analysis: ~100 tokens<br/>97.5% cost savings! 🎉
```

## Three-Tier Caching Strategy

The platform uses a sophisticated three-tier caching approach to minimize costs:

```mermaid
graph TD
    A[Incoming Query:<br/>"Analyze Limonene"] --> B{Tier 1:<br/>Database Cache}
    
    B -->|✅ HIT| C[Return from PostgreSQL<br/>Response Time: <10ms<br/>Token Cost: 0]
    
    B -->|❌ MISS| D{Tier 2:<br/>Vector Cache<br/>pgvector}
    
    D -->|✅ HIT| E[Semantic Search Match<br/>Response Time: ~100ms<br/>Token Cost: 0]
    
    E --> F[Parse & Save to DB]
    F --> C
    
    D -->|❌ MISS| G[Tier 3:<br/>OpenAI API Call]
    
    G --> H[Search PubChem<br/>for Chemical Data]
    H --> I[Search Allergen<br/>Side Effects<br/>~200 tokens]
    I --> J[Search Oxidation<br/>Products<br/>~200 tokens]
    J --> K[Total: ~400 tokens<br/>Response Time: 5-10s]
    
    K --> L[Cache in Vector Store]
    L --> M[Save to Database]
    M --> C
    
    style C fill:#c8e6c9
    style E fill:#fff9c4
    style K fill:#ffccbc
    style B fill:#e1f5fe
    style D fill:#f3e5f5
```

### Cache Performance Metrics

| Metric | Database Cache | Vector Cache | OpenAI API |
|--------|---------------|--------------|------------|
| **Response Time** | <10ms | ~100ms | 5-10s |
| **Token Cost** | 0 | 0 | ~400 tokens |
| **Cost per Query** | $0 | $0 | ~$0.002 |
| **Hit Rate** | ~80% (mature) | ~15% | ~5% |

**Example Savings:**
- First product analysis: ~4,100 tokens ($0.021)
- Repeat analysis: ~100 tokens ($0.0005)
- **Cost reduction: 97.5%**

## Authentication Flow

```mermaid
sequenceDiagram
    participant User
    participant Controller as AuthController
    participant Service as AuthService
    participant JWT as JwtService
    participant DB as UserRepository
    participant TokenDB as RefreshTokenRepository
    
    Note over User,TokenDB: Registration Flow
    User->>Controller: POST /api/auth/register
    Controller->>Service: registerUser(email, password)
    Service->>DB: Check if user exists
    DB-->>Service: User not found
    Service->>Service: Hash password (BCrypt)
    Service->>DB: Save new user
    DB-->>Service: User created
    Service-->>Controller: Success
    Controller-->>User: 200 OK
    
    Note over User,TokenDB: Login Flow
    User->>Controller: POST /api/auth/login<br/>{email, password}
    Controller->>Service: authenticateUser()
    Service->>DB: findByEmail(email)
    DB-->>Service: User found
    Service->>Service: Verify password (BCrypt)
    
    alt Password Valid
        Service->>JWT: generateAccessToken(user)
        JWT-->>Service: Access Token (1 hour)
        Service->>JWT: generateRefreshToken(user)
        JWT-->>Service: Refresh Token (7 days)
        Service->>TokenDB: Save refresh token
        Service-->>Controller: {accessToken, refreshToken}
        Controller-->>User: 200 OK + Tokens
    else Password Invalid
        Service-->>Controller: Invalid credentials
        Controller-->>User: 401 Unauthorized
    end
    
    Note over User,TokenDB: Token Refresh Flow
    User->>Controller: POST /api/auth/refresh<br/>{refreshToken}
    Controller->>Service: refreshAccessToken()
    Service->>TokenDB: Validate refresh token
    
    alt Token Valid & Not Expired
        Service->>TokenDB: Revoke old token
        Service->>JWT: generateAccessToken(user)
        JWT-->>Service: New Access Token
        Service->>JWT: generateRefreshToken(user)
        JWT-->>Service: New Refresh Token
        Service->>TokenDB: Save new refresh token
        Service-->>Controller: {accessToken}
        Controller-->>User: 200 OK + New Token
    else Token Invalid/Expired
        Service-->>Controller: Invalid token
        Controller-->>User: 401 Unauthorized
    end
    
    Note over User,TokenDB: Protected Request
    User->>Controller: GET /api/allergen/analyze/Limonene<br/>Authorization: Bearer {token}
    Controller->>JWT: Validate JWT
    
    alt Token Valid
        JWT-->>Controller: User authenticated
        Controller->>Controller: Process request
        Controller-->>User: 200 OK + Data
    else Token Invalid
        JWT-->>Controller: Invalid/Expired
        Controller-->>User: 401 Unauthorized
    end
```

## Database Schema

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : has
    USERS ||--o{ USAGE_TRACKING : tracks
    CHEMICAL_IDENTIFICATION ||--o{ SIDE_EFFECT : has
    
    USERS {
        bigint id PK
        string email UK
        string password_hash
        string first_name
        string last_name
        string role
        timestamp created_at
        timestamp updated_at
    }
    
    REFRESH_TOKENS {
        bigint id PK
        bigint user_id FK
        string token UK
        timestamp expires_at
        timestamp created_at
        boolean revoked
    }
    
    CHEMICAL_IDENTIFICATION {
        bigint id PK
        string common_name UK
        string iupac_name
        string cas_number
        integer pubchem_cid
        string molecular_formula
        string smiles
        text[] oxidation_products
        timestamp created_at
        timestamp updated_at
    }
    
    SIDE_EFFECT {
        bigint id PK
        bigint chemical_id FK
        string effect_type
        string severity
        decimal prevalence_rate
        string population
        text[] affected_body_areas
        jsonb sources
        timestamp created_at
        timestamp updated_at
    }
    
    USAGE_TRACKING {
        bigint id PK
        bigint user_id FK
        string endpoint
        integer tokens_used
        decimal estimated_cost
        boolean cache_hit
        string cache_type
        timestamp created_at
    }
```

### Key Relationships

1. **Users ↔ Refresh Tokens**: One-to-many relationship for session management
2. **Chemical Identification ↔ Side Effects**: One-to-many relationship storing allergen data
3. **Users ↔ Usage Tracking**: One-to-many relationship for cost monitoring

### Indexes

- `users.email` - Unique index for login lookup
- `refresh_tokens.token` - Unique index for token validation
- `refresh_tokens.user_id` - Foreign key index
- `chemical_identification.common_name` - Unique index for cache lookup
- `side_effect.chemical_id` - Foreign key index
- `usage_tracking.user_id` - Foreign key index for user stats

## Technology Stack

### Backend Framework
- **Spring Boot 3.5.6** - Modern Java framework with virtual threads
- **Java 21** - Latest LTS with pattern matching, records
- **Maven** - Dependency management

### AI/ML Integration
- **Spring AI 1.0.2** - Native Spring integration for AI services
- **OpenAI GPT-4o** - Advanced LLM with web search capability
- **JTokkit 1.1.0** - Token counting (tiktoken port)

### Database
- **PostgreSQL 17** - Primary relational database
- **pgvector 0.5.1** - Vector similarity search extension
- **Spring Data JPA** - ORM and repository abstraction

### Security
- **Spring Security 6.5.5** - Authentication/Authorization framework
- **JJWT 0.12.3** - JWT token generation and validation
- **BCrypt** - Password hashing with adaptive cost

### External APIs
- **PubChem REST API** - Chemical structure and identifier lookup
- **OpenAI API** - LLM completions with web search

## Performance Characteristics

### Response Times

```mermaid
graph LR
    A[Query Type] --> B[Database Cache Hit]
    A --> C[Vector Cache Hit]
    A --> D[API Call Required]
    
    B --> E[<10ms]
    C --> F[~100ms]
    D --> G[5-10 seconds]
    
    style E fill:#c8e6c9
    style F fill:#fff9c4
    style G fill:#ffccbc
```

### Cost Analysis

**First Product Analysis (20 ingredients):**
- Ingredient list extraction: 100 tokens
- 20 ingredients × 200 tokens each: 4,000 tokens
- **Total: ~4,100 tokens (~$0.021)**

**Repeat Analysis (all cached):**
- Ingredient list extraction: 100 tokens
- All ingredients cached: 0 tokens
- **Total: ~100 tokens (~$0.0005)**

**Savings: 97.5%** 🎉

### Scalability Considerations

1. **Database Connection Pooling**: HikariCP for efficient connection management
2. **Stateless Authentication**: JWT tokens enable horizontal scaling
3. **Shared Cache**: Every user's query benefits everyone (network effect)
4. **Async Processing**: Virtual threads for concurrent API calls
5. **Rate Limiting**: Per-user token budgets (future implementation)

### Cache Strategy Evolution

```mermaid
graph LR
    A[System Launch<br/>0% Cache Hit] --> B[Week 1<br/>20% Cache Hit]
    B --> C[Month 1<br/>50% Cache Hit]
    C --> D[Month 3<br/>80% Cache Hit]
    D --> E[Mature System<br/>95% Cache Hit]
    
    style A fill:#ffccbc
    style B fill:#ffe082
    style C fill:#fff9c4
    style D fill:#dcedc8
    style E fill:#c8e6c9
```

**Key Insight:** The system becomes more cost-effective and faster as it's used, creating a positive feedback loop.

---

## Future Architecture Enhancements

### Planned Improvements (see ROADMAP.md)

1. **Redis Layer**: Add in-memory cache for ultra-fast repeated queries
2. **Elasticsearch**: Full-text search across ingredients
3. **GraphQL API**: Alternative to REST for flexible queries
4. **Rate Limiting**: Tiered user limits (free vs paid)
5. **WebSocket Support**: Real-time analysis streaming
6. **Microservices**: Split into separate services as load increases

### Deployment Architecture (Future)

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[NGINX / AWS ALB]
    end
    
    subgraph "Application Tier"
        APP1[Spring Boot Instance 1]
        APP2[Spring Boot Instance 2]
        APP3[Spring Boot Instance N]
    end
    
    subgraph "Cache Tier"
        REDIS[Redis Cluster]
    end
    
    subgraph "Database Tier"
        PRIMARY[(PostgreSQL Primary)]
        REPLICA1[(Read Replica 1)]
        REPLICA2[(Read Replica 2)]
    end
    
    LB --> APP1
    LB --> APP2
    LB --> APP3
    
    APP1 --> REDIS
    APP2 --> REDIS
    APP3 --> REDIS
    
    APP1 --> PRIMARY
    APP2 --> REPLICA1
    APP3 --> REPLICA2
    
    PRIMARY -.->|Replication| REPLICA1
    PRIMARY -.->|Replication| REPLICA2
```

---

**Last Updated:** December 2025  
**Author:** Matthew Bixby  
**Related Documents:** [README.md](../README.md) | [API.md](./API.md) | [ROADMAP.md](./ROADMAP.md)