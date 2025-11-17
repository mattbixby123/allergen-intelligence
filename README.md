# 🧬 Allergen Intelligence Platform - Backend

> Solving the chemical translation problem in allergen identification using RAG, vector caching, and intelligent product analysis

**Transform:** "Is this product safe for me?" → **Comprehensive ingredient safety analysis with oxidation product tracking**

---

## 🎯 The Problem I'm Solving

Product ingredient labels list common chemical names like **"Limonene"** - but the actual allergen is often **"Limonene hydroperoxide"** (an oxidation product formed when the ingredient degrades in air). Consumers search for side effects of the wrong compound, missing critical safety information about what's actually causing their allergic reactions.

**No existing consumer tools make this chemical translation automatically.**

### Real-World Example:
- **User searches:** "Limonene side effects"
- **What they find:** "Generally safe, used in citrus fragrances"
- **What they miss:** Limonene oxidizes to allergenic peroxides causing contact dermatitis
- **This platform identifies both** ✅

---

## 💡 My Solution

A production-grade REST API that bridges the gap between ingredient labels and actual allergen information:

### Core Features

#### 🔍 Intelligent Product Analysis
- **Input:** Product name (e.g., "CeraVe Moisturizing Cream")
- **Output:** Complete ingredient safety report
- Uses OpenAI web search to extract official ingredient lists
- Analyzes every ingredient for allergen risk
- Identifies high-risk compounds automatically
- Generates actionable safety recommendations

#### 🧪 Chemical Translation Engine
- Queries PubChem API for accurate chemical structures
- Maps common names → IUPAC names → CAS numbers
- Identifies allergenic oxidation products (e.g., limonene → limonene hydroperoxide)
- Tracks chemical synonyms for comprehensive coverage

#### 📊 Clinical Research Synthesis
- Leverages OpenAI GPT-4o with web search to find peer-reviewed allergen data
- Extracts side effects with severity classifications (MILD/MODERATE/SEVERE)
- Documents prevalence rates and affected populations
- Includes source attribution for verification

#### ⚡ Three-Tier Caching System (MAJOR INNOVATION)
- **Tier 1:** PostgreSQL database (fastest - <10ms response)
- **Tier 2:** pgvector semantic cache (fast - ~100ms, no API costs)
- **Tier 3:** OpenAI API (slowest - ~5-10s, costs tokens)
- **Result:** 94.6% cost reduction on cached queries (567→30 tokens per analysis)
- Shared cache: one user's analysis benefits everyone

#### 🔐 Enterprise Authentication
- JWT access tokens (1 hour) + refresh tokens (7 days)
- Secure token rotation on every login
- BCrypt password hashing
- Stateless session management with Spring Security

#### 📈 Usage Tracking & Cost Management
- Real-time token usage tracking with tiktoken integration
- Per-user cost estimation (display tokens, analyses, estimated cost)
- Transparent cache hit/miss logging
- Helps users understand and optimize API costs

---

## 🚀 Key Metrics

| Metric | Value |
|--------|-------|
| **Cost Reduction** | 94.6% on cached analyses |
| **Token Usage** | 567→30 tokens per cached query |
| **Response Time** | 10s → 100ms (vector) → <10ms (database) |
| **Product Analysis** | First run ~4,100 tokens, repeat ~100 tokens (97.5% savings) |
| **Cache Efficiency** | System self-optimizes with usage |

---

## 🛠️ Tech Stack

### Backend Framework
- **Spring Boot 3.5.6** - Modern Java backend framework
- **Java 21** - Latest LTS with virtual threads support
- **Maven** - Dependency management

### AI & Machine Learning
- **Spring AI 1.0.2** - Spring's native AI integration layer
- **OpenAI GPT-4o** - Advanced language model with web search
- **JTokkit 1.1.0** - Accurate token counting (tiktoken for Java)

### Data & Storage
- **PostgreSQL 17** - Robust relational database
- **pgvector 0.5.1** - Vector similarity search for semantic caching
- **Spring Data JPA** - Data persistence layer

### Security
- **Spring Security 6.5.5** - Comprehensive security framework
- **JJWT 0.12.3** - JWT token generation and validation
- **BCrypt** - Password hashing

### External APIs
- **PubChem REST API** - Chemical structure and identifier lookup
- **OpenAI API** - LLM completions with web search

---

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Client (Future: React)                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ HTTPS/REST
                         │
┌────────────────────────▼────────────────────────────────────┐
│                  Spring Boot Backend                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           JWT Authentication Layer                    │   │
│  │     (Access Tokens + Refresh Token Rotation)         │   │
│  └──────────────────────────────────────────────────────┘   │
│                         │                                    │
│  ┌──────────────────────▼────────────────────────────────┐  │
│  │              REST Controllers                          │  │
│  │  • /api/auth/* - Authentication                       │  │
│  │  • /api/allergen/analyze/{ingredient} - Single        │  │
│  │  • /api/allergen/analyze-batch - Multiple             │  │
│  │  • /api/allergen/analyze-product - Full Product       │  │
│  └──────────────────────────────────────────────────────┬┘  │
│                         │                                │   │
│  ┌──────────────────────▼──────────────────┐             │   │
│  │         Service Layer                   │             │   │
│  │  • PubChemService                       │             │   │
│  │  • OpenAISearchService ◄────────────────┼─────────┐   │   │
│  │  • UsageTrackingService                 │         │   │   │
│  │  • VectorStoreService                   │         │   │   │
│  └────────┬────────────────────────────────┘         │   │   │
│           │                                           │   │   │
│  ┌────────▼────────────────────────────────┐         │   │   │
│  │    THREE-TIER CACHE STRATEGY            │         │   │   │
│  │                                          │         │   │   │
│  │  1. Database Cache (PostgreSQL)         │         │   │   │
│  │     └─> <10ms response time             │         │   │   │
│  │                                          │         │   │   │
│  │  2. Vector Cache (pgvector)             │         │   │   │
│  │     └─> ~100ms response, NO TOKENS      │         │   │   │
│  │                                          │         │   │   │
│  │  3. OpenAI API (with web search)        │         │   │   │
│  │     └─> 5-10s response, USES TOKENS ────┼─────────┘   │   │
│  └─────────────────────────────────────────┘             │   │
│                         │                                 │   │
│  ┌──────────────────────▼──────────────────────────────┐ │   │
│  │           PostgreSQL 17 Database                     │ │   │
│  │  • users (authentication)                            │ │   │
│  │  • refresh_tokens (session management)               │ │   │
│  │  • chemical_identification (PubChem data)            │ │   │
│  │  • side_effect (allergen research)                   │ │   │
│  │  • usage_tracking (token/cost monitoring)            │ │   │
│  │  • pgvector extension (semantic search)              │ │   │
│  └──────────────────────────────────────────────────────┘ │   │
└─────────────────────────────────────────────────────────────┘
```

### Request Flow Example: Product Analysis

```
User: "Analyze CeraVe Moisturizing Cream"
    │
    ├─> Extract ingredient list (OpenAI web search) [100 tokens]
    │
    └─> For each ingredient (e.g., "Cetearyl Alcohol"):
           │
           ├─> Check Database Cache
           │   └─> HIT? Return immediately [0 tokens] ✅
           │
           ├─> Check Vector Cache (if database miss)
           │   └─> HIT? Parse & save to DB [0 tokens] ✅
           │
           └─> Call OpenAI API (if both miss)
               ├─> Search PubChem for chemical data
               ├─> Search for side effects [~200 tokens]
               ├─> Search for oxidation products [~200 tokens]
               └─> Cache results in vector store & database
```

**First Run:** ~4,100 tokens (20 ingredients × 200 tokens + 100)  
**Second Run:** ~100 tokens (all ingredients cached, only ingredient list lookup)  
**Savings:** 97.5% 🎉

---

## 🔑 Key Innovations

### 1. Automated Chemical Translation
**Problem:** Users don't know about oxidation products  
**Solution:** System automatically identifies that "Limonene" oxidizes to "Limonene hydroperoxide" and searches for allergen data on both compounds

### 2. Intelligent Cost Optimization
**Problem:** Calling OpenAI API for every ingredient is expensive  
**Solution:** Three-tier caching reduces repeat query costs by 94.6%

**Implementation:**
```java
// TIER 1: Database (fastest)
List effects = sideEffectRepository.findByChemical_Id(chemical.getId());
if (!effects.isEmpty()) return effects; // <10ms, 0 tokens ✅

// TIER 2: Vector Cache (fast)
Optional cached = vectorStoreService.getCachedAllergenEffects(chemicalName);
if (cached.isPresent()) {
    effects = parseAndSaveToDatabase(cached.get()); // ~100ms, 0 tokens ✅
    return effects;
}

// TIER 3: OpenAI API (slow, expensive)
effects = openAISearchService.searchAllergenEffects(chemical); // 5-10s, ~200 tokens
cacheInVectorStore(effects); // Help future users
return effects;
```

### 3. Shared Knowledge Base
**Innovation:** Every user's analysis populates the cache for everyone else  
**Result:** System gets smarter and cheaper with usage (network effect)

### 4. Production-Grade Architecture
- Proper separation of concerns (Controller → Service → Repository)
- Comprehensive error handling
- Security best practices (JWT, BCrypt, stateless sessions)
- Detailed logging with cache hit/miss indicators
- Usage tracking for cost transparency

---

## 📚 API Documentation

### Authentication Endpoints

#### Register New User
```bash
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}

Response: 200 OK
{
  "message": "User registered successfully"
}
```

#### Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}

Response: 200 OK
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

#### Refresh Access Token
```bash
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}

Response: 200 OK
{
  "accessToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

#### Get User Info
```bash
GET /api/auth/me
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "createdAt": "2025-01-15T10:30:00",
  "usage": {
    "totalTokensUsed": 26460,
    "estimatedCost": 0.1323,
    "analysesRun": 58
  }
}
```

### Allergen Analysis Endpoints

#### Analyze Single Ingredient
```bash
GET /api/allergen/analyze/{ingredientName}
Authorization: Bearer {accessToken}

Example: GET /api/allergen/analyze/Limonene

Response: 200 OK
{
  "chemical": {
    "commonName": "Limonene",
    "iupacName": "1-Methyl-4-(1-methylethenyl)cyclohexene",
    "casNumber": "5989-27-5",
    "pubchemCid": 22311,
    "molecularFormula": "C10H16",
    "smiles": "CC1=CCC(CC1)C(=C)C"
  },
  "sideEffects": [
    {
      "effectType": "Allergic Contact Dermatitis",
      "severity": "MODERATE",
      "prevalenceRate": 0.05,
      "population": "Individuals with fragrance sensitivity",
      "affectedBodyAreas": ["Skin"],
      "sources": [...]
    }
  ],
  "oxidationProducts": [
    "Limonene hydroperoxide",
    "Limonene oxide"
  ],
  "riskAssessment": {
    "riskLevel": "MODERATE",
    "totalReactionsFound": 3
  },
  "warnings": [
    "OXIDATION ALERT: This chemical forms allergenic oxidation products when exposed to air or light."
  ],
  "disclaimer": "MEDICAL DISCLAIMER: ..."
}
```

#### Analyze Complete Product
```bash
POST /api/allergen/analyze-product
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "productName": "CeraVe Moisturizing Cream"
}

Response: 200 OK
{
  "productName": "CeraVe Moisturizing Cream",
  "totalIngredients": 20,
  "highRiskIngredients": 2,
  "overallRiskLevel": "MODERATE",
  "ingredients": [
    "Cetearyl Alcohol",
    "Glycerin",
    "Petrolatum",
    "..." 
  ],
  "detailedAnalysis": {
    "Cetearyl Alcohol": {
      "chemical": {...},
      "sideEffects": [...],
      "oxidationProducts": [...],
      "riskLevel": "LOW"
    },
    "Fragrance": {
      "chemical": {...},
      "sideEffects": [...],
      "oxidationProducts": [...],
      "riskLevel": "HIGH"
    }
  },
  "recommendations": [
    "⚠ This product contains 2 high-risk allergen(s)",
    "Consult with a dermatologist before use if you have known allergies",
    "Perform a patch test on inner arm for 48 hours before facial application"
  ],
  "disclaimer": "MEDICAL DISCLAIMER: ..."
}
```

#### Batch Analyze Multiple Ingredients
```bash
POST /api/allergen/analyze-batch
Authorization: Bearer {accessToken}
Content-Type: application/json

[
  "Limonene",
  "Linalool",
  "Citral"
]

Response: 200 OK
{
  "Limonene": { ... },
  "Linalool": { ... },
  "Citral": { ... },
  "summary": {
    "totalIngredients": 3,
    "highRiskIngredients": 1,
    "overallRiskLevel": "MODERATE"
  },
  "disclaimer": "MEDICAL DISCLAIMER: ..."
}
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 21+** - [Download](https://adoptium.net/)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **PostgreSQL 15+** - [Download](https://www.postgresql.org/download/)
- **OpenAI API Key** - [Get Key](https://platform.openai.com/api-keys)

### Installation

#### 1. Clone Repository
```bash
git clone https://github.com/yourusername/allergen-intelligence-backend.git
cd allergen-intelligence-backend
```

#### 2. Setup Database
```bash
# Create database
createdb allergen_db

# Enable pgvector extension
psql -d allergen_db -c "CREATE EXTENSION vector"
```

#### 3. Configure Environment
Create `src/main/resources/application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/allergen_db
    username: your_db_username
    password: your_db_password
  
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
  
jwt:
  secret: ${JWT_SECRET} # Generate with: openssl rand -base64 64
  expiration: 3600000  # 1 hour
  refresh-expiration: 604800000  # 7 days
```

Set environment variables:
```bash
export OPENAI_API_KEY="sk-..."
export JWT_SECRET="your-secret-key"
```

#### 4. Build and Run
```bash
# Install dependencies
mvn clean install

# Run application
mvn spring-boot:run

# Or run with local profile
mvn spring-boot:run -Dspring.profiles.active=local
```

#### 5. Verify Installation
```bash
# Test health endpoint
curl http://localhost:8080/actuator/health

# Register test user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!"
  }'

# Use the accessToken from login response
export TOKEN="eyJhbGc..."

# Test ingredient analysis
curl http://localhost:8080/api/allergen/analyze/Limonene \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🧪 Testing

### Manual Testing Flow

1. **Register & Login**
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test123!","firstName":"Test","lastName":"User"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test123!"}'
```

2. **Analyze a Product (First Time)**
```bash
curl -X POST http://localhost:8080/api/allergen/analyze-product \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productName":"Vaseline Original"}'

# Check usage stats
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"

# Note the token count (should be ~4,000-5,000 tokens)
```

3. **Analyze Same Product Again**
```bash
# Run exact same request
curl -X POST http://localhost:8080/api/allergen/analyze-product \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productName":"Vaseline Original"}'

# Check usage stats again
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"

# Token increase should be only ~100 tokens (97.5% savings!)
```

4. **Check Cache Effectiveness**
```bash
# Analyze a single ingredient that was in the product
curl http://localhost:8080/api/allergen/analyze/Petrolatum \
  -H "Authorization: Bearer $TOKEN"

# Check logs for:
# ✅ DATABASE CACHE HIT (means it's working!)
```

---

## 📊 Project Structure

```
src/main/java/com/matthewbixby/allergen/intelligence/
├── config/
│   ├── SecurityConfig.java           # Spring Security configuration
│   ├── ChatClientConfig.java         # OpenAI client bean
│   └── VectorStoreConfig.java        # pgvector setup
│
├── controller/
│   ├── AuthController.java           # Authentication endpoints
│   ├── AllergenSearchController.java # Main allergen analysis API
│   └── TestController.java           # Health checks
│
├── dto/
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── ProductAnalysisRequest.java
│   ├── ProductAnalysisResponse.java
│   └── IngredientAnalysis.java
│
├── model/
│   ├── User.java                     # User entity
│   ├── RefreshToken.java             # Refresh token entity
│   ├── ChemicalIdentification.java   # Chemical data from PubChem
│   ├── SideEffect.java               # Allergen side effects
│   └── UsageTracking.java            # Token usage tracking
│
├── repository/
│   ├── UserRepository.java
│   ├── RefreshTokenRepository.java
│   ├── ChemicalRepository.java
│   ├── SideEffectRepository.java
│   └── UsageTrackingRepository.java
│
├── service/
│   ├── AuthService.java              # Authentication logic
│   ├── JwtService.java               # JWT generation/validation
│   ├── PubChemService.java           # PubChem API integration
│   ├── OpenAISearchService.java      # OpenAI web search + parsing
│   ├── VectorStoreService.java       # pgvector caching
│   └── UsageTrackingService.java     # Token tracking
│
└── AllergenIntelligenceApplication.java
```

---

## 🔮 Future Enhancements

### Phase 2: Frontend (In Progress)
- [ ] React + Vite frontend with Tailwind CSS
- [ ] Product search interface
- [ ] Ingredient list visualization
- [ ] Risk assessment dashboard
- [ ] User profile with usage statistics

### Phase 3: Advanced Features
- [ ] Image upload for ingredient extraction (OCR + GPT-4 Vision)
- [ ] Batch product CSV import
- [ ] Export reports to PDF
- [ ] Email alerts for high-risk products
- [ ] Compare multiple products side-by-side

### Phase 4: Scale & Optimize
- [ ] Cache pre-warming for top 1000 products
- [ ] Redis layer for ultra-fast repeated queries
- [ ] Elasticsearch for full-text ingredient search
- [ ] GraphQL API as alternative to REST
- [ ] Rate limiting per user tier (free vs paid)

### Phase 5: Community Features
- [ ] User-contributed product reviews
- [ ] Ingredient discussion forums
- [ ] Share analysis reports via link
- [ ] Mobile app (React Native)

---

## 🤝 Contributing

This is primarily a portfolio project to demonstrate my full-stack development capabilities. However, I welcome:

- 🐛 Bug reports and feature suggestions
- 💡 Ideas for additional use cases
- 🧪 Chemistry/allergen domain knowledge
- 📖 Documentation improvements
- ⭐ Stars if you find this interesting!

**To contribute:**
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## ⚠️ Important Disclaimers

### Medical Disclaimer
**THIS TOOL IS FOR EDUCATIONAL AND RESEARCH PURPOSES ONLY.**

- ❌ This is NOT medical advice
- ❌ Do NOT use for self-diagnosis
- ❌ Do NOT use for treatment decisions
- ✅ Always consult qualified healthcare professionals for medical decisions
- ✅ Verify all information with authoritative sources

### Accuracy Disclaimer
**INFORMATION MAY BE INCOMPLETE, OUTDATED, OR INCORRECT.**

- Chemical and medical data should be verified with professional sources
- AI-generated content can contain errors or hallucinations
- Ingredient lists may not reflect current product formulations
- Allergen research is constantly evolving

### No Warranty
This software is provided "as is" without warranty of any kind, express or implied.  
Use at your own risk.

---

## 📝 License

MIT License - See [LICENSE](LICENSE) file for details

**TL;DR:** You can use this code, modify it, and distribute it, but:
- Provide attribution to the original author
- Include the license and copyright notice
- No warranty is provided

---

## 👨‍💻 About the Developer

**Matthew Bixby**  
Full-Stack Developer | Chemistry Background | AI/ML Enthusiast

I built this platform to solve a real-world problem I encountered: consumers lack tools to understand the true allergen risks in everyday products. By combining my chemistry background with modern AI techniques, I created a system that automatically translates ingredient labels into comprehensive safety information.

**What This Project Demonstrates:**
- 🏗️ Production-grade backend architecture (Spring Boot, PostgreSQL, pgvector)
- 🤖 AI/ML integration (RAG pipelines, vector search, LLM prompt engineering)
- 🔐 Enterprise security (JWT auth, BCrypt, stateless sessions)
- ⚡ Performance optimization (three-tier caching, 94.6% cost reduction)
- 🧪 Domain expertise (organic chemistry, allergen science, clinical research)
- 📊 System design (REST APIs, database modeling, caching strategies)
- 📝 Technical communication (documentation, architecture diagrams)
- 🚀 Execution (actually finishing projects, not just starting them)

**Connect with me:**
- 💼 [LinkedIn](https://www.linkedin.com/in/matthew-bixby/)
- 🌐 [Portfolio](https://www.matthewbixby.com)
- 📧 [Email](mailto:matthew.bixby1@gmail.com)
- 🐙 [GitHub](https://github.com/yourusername)

---

## 🙏 Acknowledgments

- **PubChem** for providing free chemical data via their API
- **OpenAI** for GPT-4o and web search capabilities
- **Spring AI Team** for making AI integration seamless in Spring Boot
- **pgvector Contributors** for bringing vector search to PostgreSQL
- The open-source community for all the amazing libraries that made this possible

---

## 📈 Project Stats

![GitHub stars](https://img.shields.io/github/stars/yourusername/allergen-intelligence-backend?style=social)
![GitHub forks](https://img.shields.io/github/forks/yourusername/allergen-intelligence-backend?style=social)
![GitHub issues](https://img.shields.io/github/issues/yourusername/allergen-intelligence-backend)
![GitHub last commit](https://img.shields.io/github/last-commit/yourusername/allergen-intelligence-backend)

---

**Built with ❤️ and a lot of ☕**

*Combining domain expertise with software engineering to solve real-world problems*
