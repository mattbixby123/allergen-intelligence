# Getting Started - Allergen Intelligence Platform

Complete setup guide for the production-ready Allergen Intelligence Platform with JWT authentication, three-tier caching, and AI-powered allergen analysis.

---

## Quick Start

```bash
# 1. Clone repository
git clone https://github.com/mattbixby123/allergen-intelligence.git
cd allergen-intelligence

# 2. Set up database
createdb allergen_db
psql -d allergen_db -c "CREATE EXTENSION vector"

# 3. Configure API key
export OPENAI_API_KEY="sk-proj-your-key-here"

# 4. Generate JWT secret
export JWT_SECRET=$(openssl rand -base64 64)

# 5. Run application
mvn spring-boot:run

# 6. Test it works
curl http://localhost:8080/actuator/health
```

**Expected result:** Application running on port 8080 with health check responding.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [API Keys & Configuration](#api-keys--configuration)
- [Build and Run](#build-and-run)
- [First API Request](#first-api-request)
- [Testing](#testing)
- [Development Tools](#development-tools)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| **Java JDK** | 21+ | Application runtime |
| **Maven** | 3.8+ | Build tool & dependency management |
| **PostgreSQL** | 15+ | Primary database |
| **Git** | Latest | Version control |

### Installation

<details>
<summary><b>macOS (using Homebrew)</b></summary>

```bash
# Install all prerequisites
brew install openjdk@21 maven postgresql@17 git

# Start PostgreSQL
brew services start postgresql@17

# Verify installations
java -version    # Should show: openjdk version "21.x.x"
mvn -version     # Should show: Apache Maven 3.x.x
psql --version   # Should show: psql (PostgreSQL) 17.x
git --version    # Should show: git version 2.x.x
```
</details>

<details>
<summary><b>Ubuntu/Debian</b></summary>

```bash
# Update package list
sudo apt update

# Install Java 21
sudo apt install openjdk-21-jdk

# Install Maven
sudo apt install maven

# Install PostgreSQL 17
sudo apt install postgresql-17 postgresql-contrib

# Install Git
sudo apt install git

# Start PostgreSQL
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Verify installations
java -version
mvn -version
psql --version
git --version
```
</details>

<details>
<summary><b>Windows</b></summary>

**Java 21:**
1. Download from [Adoptium](https://adoptium.net/)
2. Run installer and add to PATH

**Maven:**
1. Download from [Apache Maven](https://maven.apache.org/download.cgi)
2. Extract and add `bin` directory to PATH

**PostgreSQL:**
1. Download from [PostgreSQL.org](https://www.postgresql.org/download/windows/)
2. Run installer and remember your password

**Git:**
1. Download from [git-scm.com](https://git-scm.com/download/win)
2. Run installer with default settings

**Verify in PowerShell:**
```powershell
java -version
mvn -version
psql --version
git --version
```
</details>

---

## Database Setup

### Step 1: Create Database

```bash
# Create database
createdb allergen_db

# If permission issues (Linux/Windows):
sudo -u postgres createdb allergen_db
```

### Step 2: Install pgvector Extension

The platform uses pgvector for semantic caching (97.5% cost reduction):

```bash
# Install extension
psql -d allergen_db -c "CREATE EXTENSION vector"

# Verify installation
psql -d allergen_db -c "SELECT * FROM pg_extension WHERE extname = 'vector'"
```

**Expected output:**
```
 oid  | extname | extowner | extnamespace | extrelocatable | extversion
------+---------+----------+--------------+----------------+------------
 xxxxx| vector  |       10 |         2200 | f              | 0.5.1
```

### Step 3: Verify Connection

```bash
# Test database connection
psql -d allergen_db

# Inside psql:
\dt                # List tables (should be empty initially)
\dx                # List extensions (should show 'vector')
\q                 # Exit
```

### Alternative: Docker Setup

If you prefer Docker:

```bash
# Run PostgreSQL with pgvector pre-installed
docker run -d \
  --name allergen-postgres \
  -e POSTGRES_DB=allergen_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  ankane/pgvector

# Verify it's running
docker ps | grep allergen-postgres
```

---

## API Keys & Configuration

### 1. OpenAI API Key (Required)

The platform uses OpenAI GPT-4o for allergen research.

**Get your API key:**
1. Visit [OpenAI Platform](https://platform.openai.com/signup)
2. Navigate to [API Keys](https://platform.openai.com/api-keys)
3. Click "Create new secret key"
4. Name it: "Allergen Intelligence Dev"
5. **Copy and save immediately** (you won't see it again!)

**Set environment variable:**

```bash
# Add to ~/.bashrc, ~/.zshrc, or ~/.bash_profile
export OPENAI_API_KEY="sk-proj-your-actual-key-here"

# Reload shell configuration
source ~/.zshrc  # or source ~/.bashrc

# Verify
echo $OPENAI_API_KEY
```

**Windows (PowerShell):**
```powershell
[System.Environment]::SetEnvironmentVariable('OPENAI_API_KEY', 'sk-proj-your-key', 'User')
```

### 2. JWT Secret Key (Required)

Generate a secure secret for JWT token signing:

```bash
# Generate 64-byte random key
export JWT_SECRET=$(openssl rand -base64 64)

# Make it permanent (add to ~/.zshrc or ~/.bashrc)
echo "export JWT_SECRET=\"$(openssl rand -base64 64)\"" >> ~/.zshrc
source ~/.zshrc

# Verify
echo $JWT_SECRET
```

**Windows (PowerShell):**
```powershell
$secret = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 64 | % {[char]$_})
[System.Environment]::SetEnvironmentVariable('JWT_SECRET', $secret, 'User')
```

### 3. Database Configuration (Optional)

If using non-default database settings:

```bash
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
export DATABASE_URL=jdbc:postgresql://localhost:5432/allergen_db
```

### 4. Application Configuration File

Create `src/main/resources/application-local.yml` for local development:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/allergen_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  
ai:
  openai:
    api-key: ${OPENAI_API_KEY}
    chat:
      options:
        model: gpt-4o

jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000          # 1 hour in milliseconds
  refresh-expiration: 604800000 # 7 days in milliseconds

logging:
  level:
    com.matthewbixby.allergen: DEBUG
```

---

## Build and Run

### Clone Repository

```bash
# Clone the repository
git clone https://github.com/mattbixby123/allergen-intelligence.git
cd allergen-intelligence

# Verify project structure
ls -la
# Should see: pom.xml, src/, README.md, docs/, etc.
```

### Build Project

```bash
# Install dependencies and build
mvn clean install

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Total time: 30-60 seconds
```

**If build fails:**
```bash
# Clear Maven cache and retry
mvn clean
mvn dependency:purge-local-repository
mvn clean install -U
```

### Run Application

**Option 1: Maven (Recommended for Development)**
```bash
mvn spring-boot:run

# With specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Option 2: JAR File**
```bash
# Build JAR
mvn clean package -DskipTests

# Run JAR
java -jar target/intelligence-0.0.1-SNAPSHOT.jar
```

**Option 3: IDE**

<details>
<summary><b>IntelliJ IDEA</b></summary>

1. Open project: **File → Open** → Select `allergen-intelligence` folder
2. Wait for Maven import to complete
3. Install Lombok plugin: **File → Settings → Plugins → Search "Lombok"**
4. Enable annotation processing: **Settings → Build, Execution, Deployment → Compiler → Annotation Processors** → Check "Enable annotation processing"
5. Find `AllergenIntelligenceApplication.java` in `src/main/java`
6. Right-click → **Run 'AllergenIntelligenceApplication'**
</details>

<details>
<summary><b>VS Code</b></summary>

1. Install extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Lombok Annotations Support
2. Open project folder
3. Wait for Java extension to load
4. Press **F5** or click **Run → Start Debugging**
</details>

### Verify Application Started

Look for this in console output:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v3.5.6)

2025-01-15 10:30:00.000  INFO --- [main] AllergenIntelligenceApplication : Started AllergenIntelligenceApplication in 5.234 seconds
```

**Application is now running on:** `http://localhost:8080`

---

## First API Request

Let's verify everything works by making your first API requests!

### Step 1: Check Health

```bash
curl http://localhost:8080/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

### Step 2: Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Expected Response:**
```json
{
  "message": "User registered successfully"
}
```

### Step 3: Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!"
  }'
```

**Expected Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Save your access token:**
```bash
export TOKEN="your-access-token-here"
```

### Step 4: Analyze Your First Ingredient

```bash
curl http://localhost:8080/api/allergen/analyze/Limonene \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**
```json
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
      "affectedBodyAreas": ["Skin"]
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
    "⚠️ OXIDATION ALERT: This chemical forms allergenic oxidation products when exposed to air or light."
  ]
}
```

🎉 **Success!** Your first allergen analysis is complete!

**Note:** First analysis takes ~10 seconds (OpenAI API call). Repeat analysis takes <1 second (cache hit)!

### Step 5: Analyze a Complete Product

```bash
curl -X POST http://localhost:8080/api/allergen/analyze-product \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Vaseline Original"
  }'
```

**Expected Response:**
```json
{
  "productName": "Vaseline Original",
  "totalIngredients": 1,
  "highRiskIngredients": 0,
  "overallRiskLevel": "LOW",
  "ingredients": ["Petrolatum"],
  "detailedAnalysis": {
    "Petrolatum": {
      "chemical": { ... },
      "sideEffects": [ ... ],
      "riskLevel": "LOW"
    }
  },
  "recommendations": [
    "This product contains 0 high-risk allergen(s)",
    "Generally well-tolerated for most users"
  ]
}
```

### Step 6: Check Your Usage Stats

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**
```json
{
  "email": "test@example.com",
  "firstName": "Test",
  "lastName": "User",
  "role": "USER",
  "createdAt": "2025-01-15T10:30:00Z",
  "usage": {
    "totalTokensUsed": 4500,
    "estimatedCost": 0.0225,
    "analysesRun": 2
  }
}
```

**Congratulations!** 🎉 You've successfully:
- ✅ Set up the application
- ✅ Registered a user
- ✅ Authenticated with JWT
- ✅ Analyzed ingredients
- ✅ Monitored API costs

---

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
# Test authentication
mvn test -Dtest=AuthServiceTest

# Test allergen service
mvn test -Dtest=OpenAISearchServiceTest

# Test PubChem integration
mvn test -Dtest=PubChemServiceTest
```

### Run with Coverage Report

```bash
mvn clean test jacoco:report

# View report in browser
open target/site/jacoco/index.html  # macOS
xdg-open target/site/jacoco/index.html  # Linux
start target/site/jacoco/index.html  # Windows
```

### Integration Tests

```bash
# Run only integration tests
mvn test -Dtest=*IntegrationTest

# Run all tests including integration
mvn verify
```

---

## Development Tools

### Recommended IDEs

**IntelliJ IDEA (Recommended)**
- Best Spring Boot support
- Excellent debugging tools
- Built-in database tools
- Free Community Edition available

**VS Code**
- Lightweight and fast
- Good Java extensions
- Great for frontend + backend work
- Completely free

### Essential IDE Plugins

**IntelliJ IDEA:**
- Lombok
- Spring Assistant
- Database Navigator
- GitToolBox

**VS Code:**
- Extension Pack for Java
- Spring Boot Extension Pack
- Lombok Annotations Support
- REST Client

### API Testing Tools

**cURL (Command Line)**
- Included in macOS/Linux
- Windows: Download from curl.se
- Best for quick tests

**Postman (GUI)**
- Download: [postman.com](https://www.postman.com/downloads/)
- Import collection from `docs/API.md`
- Visual request builder
- Save requests and collections

**HTTPie (Modern CLI)**
```bash
# Install
brew install httpie  # macOS
pip install httpie   # Any OS

# Usage (prettier than cURL)
http POST localhost:8080/api/auth/login email=test@example.com password=Test123!
```

### Database Management

**psql (Command Line)**
```bash
# Connect to database
psql -d allergen_db

# Useful commands
\dt                    # List tables
\d table_name         # Describe table
\dx                   # List extensions
SELECT count(*) FROM chemical_identification;
```

**pgAdmin (GUI)**
- Download: [pgadmin.org](https://www.pgadmin.org/download/)
- Visual database management
- Query builder

**DBeaver (Recommended)**
- Download: [dbeaver.io](https://dbeaver.io/download/)
- Free and lightweight
- Multi-database support
- Visual query builder

---

## Project Structure

```
allergen-intelligence/
├── src/
│   ├── main/
│   │   ├── java/com/matthewbixby/allergen/intelligence/
│   │   │   ├── AllergenIntelligenceApplication.java  # Main entry
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java              # JWT + CORS
│   │   │   │   ├── ChatClientConfig.java            # OpenAI client
│   │   │   │   └── VectorStoreConfig.java           # pgvector setup
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java              # Auth endpoints
│   │   │   │   └── AllergenSearchController.java    # Analysis endpoints
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java                 # User management
│   │   │   │   ├── JwtService.java                  # Token handling
│   │   │   │   ├── PubChemService.java              # Chemical data
│   │   │   │   ├── OpenAISearchService.java         # AI research
│   │   │   │   ├── VectorStoreService.java          # Caching
│   │   │   │   └── UsageTrackingService.java        # Cost monitoring
│   │   │   ├── model/
│   │   │   │   ├── User.java                        # User entity
│   │   │   │   ├── RefreshToken.java               # Token entity
│   │   │   │   ├── ChemicalIdentification.java     # Chemical data
│   │   │   │   ├── SideEffect.java                 # Allergen info
│   │   │   │   └── UsageTracking.java              # Usage stats
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── ChemicalRepository.java
│   │   │   │   ├── SideEffectRepository.java
│   │   │   │   └── UsageTrackingRepository.java
│   │   │   └── dto/
│   │   │       ├── LoginRequest.java
│   │   │       ├── ProductAnalysisResponse.java
│   │   │       └── IngredientAnalysis.java
│   │   └── resources/
│   │       ├── application.properties               # Main config
│   │       └── application-local.yml               # Local overrides
│   └── test/                                        # Test files
├── docs/
│   ├── API.md                                       # API documentation
│   ├── ARCHITECTURE.md                              # System design
│   ├── ROADMAP.md                                   # Development plan
│   └── GETTING_STARTED.md                           # This file
├── pom.xml                                          # Maven dependencies
├── README.md                                        # Project overview
└── LICENSE                                          # MIT License
```

---

## Troubleshooting

### Database Issues

**Problem:** `FATAL: database "allergen_db" does not exist`

```bash
# Create the database
createdb allergen_db

# Or with sudo (Linux)
sudo -u postgres createdb allergen_db
```

**Problem:** `ERROR: extension "vector" does not exist`

```bash
# Install pgvector extension
psql -d allergen_db -c "CREATE EXTENSION vector"
```

**Problem:** `Connection refused to localhost:5432`

```bash
# Check if PostgreSQL is running
pg_isready

# Start PostgreSQL
brew services start postgresql@17  # macOS
sudo systemctl start postgresql    # Linux
docker start allergen-postgres     # Docker

# Check status
brew services list                 # macOS
sudo systemctl status postgresql   # Linux
```

### Build Issues

**Problem:** `BUILD FAILURE - Could not resolve dependencies`

```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Rebuild
mvn clean install -U
```

**Problem:** `Error: JAVA_HOME is not defined correctly`

```bash
# Set JAVA_HOME (macOS/Linux)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk     # Linux

# Add to ~/.zshrc or ~/.bashrc to make permanent
```

**Problem:** `Lombok annotations not working`

1. Install Lombok plugin in IDE
2. Enable annotation processing in IDE settings
3. Restart IDE
4. Rebuild project: `mvn clean install`

### API Issues

**Problem:** `401 Unauthorized` on protected endpoints

```bash
# Verify you're using a valid token
echo $TOKEN

# If empty, login again
LOGIN_RESPONSE=$(curl -s -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')
export TOKEN

# Retry request
curl localhost:8080/api/allergen/analyze/Limonene \
  -H "Authorization: Bearer $TOKEN"
```

**Problem:** `OpenAI API key invalid`

```bash
# Check if key is set
echo $OPENAI_API_KEY

# Should start with "sk-proj-" or "sk-"
# If wrong or empty, set it correctly
export OPENAI_API_KEY="sk-proj-your-actual-key"

# Restart application
```

**Problem:** `Port 8080 already in use`

```bash
# Find process using port 8080
lsof -i :8080             # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill the process (replace PID)
kill -9 <PID>             # macOS/Linux
taskkill /PID <PID> /F    # Windows

# Or change port in application.properties
server.port=8081
```

**Problem:** `JWT_SECRET not configured`

```bash
# Generate and set JWT secret
export JWT_SECRET=$(openssl rand -base64 64)

# Make permanent
echo "export JWT_SECRET=\"$(openssl rand -base64 64)\"" >> ~/.zshrc
source ~/.zshrc
```

### Performance Issues

**Problem:** First analysis takes very long (>30 seconds)

This is normal for the first request:
- OpenAI API calls take 5-10 seconds
- PubChem lookups add 1-2 seconds
- Embedding generation adds 1-2 seconds

**Solution:** Be patient on first request. Subsequent requests will be <1 second due to caching!

**Problem:** Database queries slow

```sql
-- Check table sizes
SELECT 
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Create missing indexes if needed
CREATE INDEX IF NOT EXISTS idx_chemical_common_name 
ON chemical_identification(common_name);
```

---

## Useful Commands

### Development Workflow

```bash
# Daily startup
cd allergen-intelligence
git pull origin main
mvn spring-boot:run

# Run tests before committing
mvn clean test

# Check code style
mvn checkstyle:check

# View dependency tree
mvn dependency:tree

# Update dependencies
mvn versions:display-dependency-updates
```

### Database Queries

```sql
-- See all chemicals in database
SELECT common_name, cas_number FROM chemical_identification;

-- View side effects
SELECT ce.common_name, se.effect_type, se.severity
FROM chemical_identification ce
JOIN side_effect se ON ce.id = se.chemical_id;

-- Check usage stats
SELECT u.email, SUM(ut.tokens_used) as total_tokens
FROM users u
JOIN usage_tracking ut ON u.id = ut.user_id
GROUP BY u.email;

-- View cache effectiveness
SELECT 
    cache_type,
    COUNT(*) as hits,
    AVG(tokens_used) as avg_tokens
FROM usage_tracking
GROUP BY cache_type;
```

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/your-feature-name

# Make changes, then stage and commit
git add .
git commit -m "feat: add new feature"

# Push to remote
git push -u origin feature/your-feature-name

# After PR approval, merge to main
git checkout main
git pull origin main
```

---

## Next Steps

### You're Ready For:

1. **Frontend Development** 🎨
   - Start building React frontend
   - Integrate with API endpoints
   - See [docs/ROADMAP.md](ROADMAP.md) Phase 5

2. **Add Features** ✨
   - Implement image upload (GPT-4 Vision)
   - Add batch analysis
   - Build comparison tools

3. **Deploy to Production** 🚀
   - Set up Railway or AWS
   - Configure production database
   - Set up monitoring

### Learning Resources

**Backend Development:**
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [JWT Authentication Guide](https://www.baeldung.com/spring-security-oauth-jwt)

**Database & Caching:**
- [pgvector Documentation](https://github.com/pgvector/pgvector)
- [PostgreSQL Performance Tuning](https://wiki.postgresql.org/wiki/Performance_Optimization)

**AI Integration:**
- [OpenAI API Docs](https://platform.openai.com/docs/)
- [RAG Patterns](https://www.anthropic.com/research)

### Explore the Codebase

```bash
# Look at key files
cat src/main/java/.../AllergenIntelligenceApplication.java
cat src/main/java/.../controller/AllergenSearchController.java
cat src/main/java/.../service/OpenAISearchService.java
cat src/main/resources/application.properties

# Review tests
ls src/test/java/.../service/
```

---

## Getting Help

### Check These First

1. **Console Logs:** Look for errors in terminal output
2. **Application Logs:** Check `logs/` directory
3. **Database:** Verify with `psql -d allergen_db`
4. **Environment:** Confirm all env vars are set

### Documentation

- **API Reference:** [docs/API.md](API.md)
- **Architecture:** [docs/ARCHITECTURE.md](ARCHITECTURE.md)
- **Project Overview:** [README.md](../README.md)
- **Roadmap:** [docs/ROADMAP.md](ROADMAP.md)

### Community Resources

- **GitHub Issues:** Report bugs or ask questions
- **Stack Overflow:** Use tags `spring-boot`, `spring-ai`, `pgvector`
- **Spring Community:** [spring.io/community](https://spring.io/community)

---

## Success Checklist

Before considering setup complete:

- [ ] PostgreSQL installed and running
- [ ] `allergen_db` database created
- [ ] pgvector extension enabled
- [ ] Java 21 installed and verified
- [ ] Maven installed and working
- [ ] OPENAI_API_KEY environment variable set
- [ ] JWT_SECRET environment variable set
- [ ] Application builds successfully (`mvn clean install`)
- [ ] Application starts without errors
- [ ] Health endpoint responds: `curl localhost:8080/actuator/health`
- [ ] User registration works
- [ ] Login returns JWT tokens
- [ ] Ingredient analysis works
- [ ] Usage tracking shows correct token counts

---

## Conclusion

🎉 **Congratulations!** You now have a fully functional, production-ready allergen analysis platform running locally.

The platform features:
- ✅ JWT authentication with secure token rotation
- ✅ Three-tier caching (97.5% cost savings)
- ✅ Real-time usage and cost tracking
- ✅ Automated oxidation product detection
- ✅ AI-powered allergen research
- ✅ Comprehensive API documentation

**You're ready to:**
- Build the frontend interface
- Add new features
- Deploy to production
- Start analyzing products!

Happy coding! 🚀

---

**Last Updated:** December 2025  
**Application Version:** 1.0.0  
**Author:** Matthew Bixby