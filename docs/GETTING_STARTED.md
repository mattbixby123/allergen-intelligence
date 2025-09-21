# Getting Started - Allergen Intelligence Platform

Complete setup guide for local development environment.

## Prerequisites

### Required Software

**Java Development Kit 21+**
```bash
# macOS (using Homebrew)
brew install openjdk@21

# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk

# Windows - Download from:
# https://adoptium.net/

# Verify installation
java -version
# Should output: openjdk version "21.x.x"
```

**Maven 3.8+**
```bash
# macOS
brew install maven

# Ubuntu/Debian
sudo apt install maven

# Windows - Download from:
# https://maven.apache.org/download.cgi

# Verify
mvn -version
```

**PostgreSQL 15+**
```bash
# macOS
brew install postgresql@17
brew services start postgresql@17

# Ubuntu/Debian
sudo apt install postgresql-17 postgresql-contrib
sudo systemctl start postgresql

# Windows - Download from:
# https://www.postgresql.org/download/windows/

# Verify
psql --version
```

**Git**
```bash
# macOS
brew install git

# Ubuntu/Debian
sudo apt install git

# Windows - Download from:
# https://git-scm.com/download/win

# Verify
git --version
```

**Docker (Optional)**
```bash
# macOS
brew install docker

# Ubuntu/Debian
sudo apt install docker.io

# Windows - Download Docker Desktop from:
# https://www.docker.com/products/docker-desktop/
```

---

## Database Setup

### Option A: Local PostgreSQL (Recommended for Development)

**1. Create Database**
```bash
# macOS (using peer authentication)
createdb allergen_db

# Linux/Windows (with password)
sudo -u postgres createdb allergen_db
```

**2. Install pgvector Extension**
```bash
# macOS
psql -d allergen_db -c "CREATE EXTENSION vector"

# Linux/Windows
sudo -u postgres psql -d allergen_db -c "CREATE EXTENSION vector"

# Verify installation
psql -d allergen_db -c "SELECT * FROM pg_extension WHERE extname = 'vector'"
```

**3. Configure Connection**
```bash
# macOS (peer authentication - no password needed)
export DB_USERNAME=$(whoami)
# DB_PASSWORD not needed

# Linux/Windows
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

### Option B: Docker PostgreSQL with pgvector

```bash
# Pull and run PostgreSQL with pgvector pre-installed
docker run -d \
  --name allergen-postgres \
  -e POSTGRES_DB=allergen_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=your_password \
  -p 5432:5432 \
  ankane/pgvector

# Verify container is running
docker ps | grep allergen-postgres

# Connect to database
docker exec -it allergen-postgres psql -U postgres -d allergen_db

# In psql, verify vector extension
\dx
\q
```

---

## API Keys Setup

### OpenAI API Key (Required)

1. **Create Account**: Go to https://platform.openai.com/signup
2. **Navigate to API Keys**: https://platform.openai.com/api-keys
3. **Create Key**: Click "Create new secret key"
4. **Name It**: e.g., "Allergen Intelligence Dev"
5. **Copy Key**: Save it immediately (you won't see it again!)

**Set Environment Variable:**
```bash
# Add to ~/.bashrc, ~/.zshrc, or ~/.bash_profile
export OPENAI_API_KEY="sk-proj-your-actual-key-here"

# Reload shell
source ~/.zshrc  # or ~/.bashrc
```

**Verify:**
```bash
echo $OPENAI_API_KEY
# Should output your key
```

### NCBI API Key (Optional - for PubChem rate limit improvements)

1. **Register**: https://www.ncbi.nlm.nih.gov/account/
2. **Get API Key**: Settings → API Key Management
3. **Set Variable**:
```bash
export NCBI_API_KEY="your-ncbi-key"
```

---

## Project Setup

### Clone Repository

```bash
# Clone the repository
git clone https://github.com/yourusername/allergen-intelligence.git
cd allergen-intelligence

# Verify you're in the right directory
ls -la
# Should see: pom.xml, src/, README.md, etc.
```

### Configure Application

**Create `.env` file** (for local development):
```bash
# In project root directory
cat > .env << EOF
# Database Configuration
DB_USERNAME=$(whoami)
# DB_PASSWORD= (leave empty for macOS peer auth)
DATABASE_URL=jdbc:postgresql://localhost:5432/allergen_db

# OpenAI Configuration
OPENAI_API_KEY=sk-proj-your-actual-key-here

# Application Settings
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
EOF
```

**Update application.properties** (already configured in repository):

Located at: `src/main/resources/application.properties`

Key settings to verify:
```properties
# Database connection (adjust if using different setup)
spring.datasource.url=jdbc:postgresql://localhost:5432/allergen_db
spring.datasource.username=${DB_USERNAME:your-username}

# OpenAI API key
spring.ai.openai.api-key=${OPENAI_API_KEY}
```

---

## Build and Run

### First Time Setup

```bash
# Install dependencies and build
mvn clean install

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Total time: 20-30 seconds
```

### Run Application

**Option 1: Using Maven (Recommended for Development)**
```bash
mvn spring-boot:run

# With specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Option 2: Using JAR File**
```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/intelligence-0.0.1-SNAPSHOT.jar
```

**Option 3: Using IDE**

**IntelliJ IDEA:**
1. Import project as Maven project
2. Install Lombok plugin (File → Settings → Plugins)
3. Enable annotation processing (Settings → Build → Compiler → Annotation Processors)
4. Right-click `AllergenIntelligenceApplication.java`
5. Select "Run" or "Debug"

**VS Code:**
1. Install "Java Extension Pack"
2. Install "Spring Boot Extension Pack"
3. Open project folder
4. Press F5 or use Run → Start Debugging

---

## Verify Installation

### 1. Check Application Started

Look for this in console output:
```
Started AllergenIntelligenceApplication in X.XXX seconds
```

### 2. Test Health Endpoint

```bash
curl http://localhost:8080/api/test/health

# Expected response:
# "Allergen Intelligence Platform is running!"
```

### 3. Test PubChem Integration

```bash
curl http://localhost:8080/api/test/chemical/Limonene

# Expected response (JSON):
# {
#   "commonName": "Limonene",
#   "casNumber": "5989-27-5",
#   "iupacName": "1-Methyl-4-(1-methylethenyl)cyclohexene"
# }
```

### 4. Verify Database Connection

```bash
# Connect to database
psql -d allergen_db

# List tables
\dt

# Should see:
# - chemical_identifications
# - side_effects
# - allergen.vector_store
# (and related junction tables)

# Exit
\q
```

### 5. Check API Documentation

Open browser to:
```
http://localhost:8080/swagger-ui.html
```

Should see interactive API documentation.

---

## Development Workflow

### Daily Development Routine

```bash
# 1. Start PostgreSQL (if not using Docker)
brew services start postgresql@17  # macOS
sudo systemctl start postgresql    # Linux

# 2. Pull latest changes
git pull origin main

# 3. Run application
mvn spring-boot:run

# 4. Make changes, test, commit
git add .
git commit -m "feat: add new feature"
git push
```

### Hot Reload Development

**Enable Spring Boot DevTools** (already in pom.xml):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

Changes to Java files will automatically reload the application.

---

## Project Structure

```
allergen-intelligence/
├── src/
│   ├── main/
│   │   ├── java/com/allergen/intelligence/
│   │   │   ├── AllergenIntelligenceApplication.java  # Main entry point
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── controller/       # REST endpoints
│   │   │   ├── service/          # Business logic
│   │   │   ├── model/            # JPA entities
│   │   │   ├── repository/       # Database access
│   │   │   └── dto/              # Request/response objects
│   │   └── resources/
│   │       ├── application.properties  # Main config
│   │       └── application-dev.properties  # Dev config
│   └── test/
│       └── java/com/allergen/intelligence/
│           ├── service/          # Service tests
│           ├── controller/       # API tests
│           └── integration/      # Integration tests
├── docs/                         # Documentation
├── pom.xml                       # Maven dependencies
├── .gitignore                    # Git ignore rules
├── README.md                     # Project overview
└── LICENSE                       # MIT License
```

---

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=PubChemServiceTest
```

### Run with Coverage Report

```bash
mvn clean test jacoco:report

# View report at:
# target/site/jacoco/index.html
```

### Write Your First Test

Create: `src/test/java/com/allergen/intelligence/service/PubChemServiceTest.java`

```java
package com.allergen.intelligence.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PubChemServiceTest {
    
    @Autowired
    private PubChemService pubChemService;
    
    @Test
    void shouldRetrieveChemicalData() {
        Map<String, String> result = pubChemService.getChemicalData("Limonene");
        
        assertNotNull(result);
        assertTrue(result.containsKey("casNumber"));
        assertEquals("5989-27-5", result.get("casNumber"));
    }
}
```

---

## Troubleshooting

### Database Connection Issues

**Problem:** `Connection refused`
```bash
# Check PostgreSQL is running
pg_isready

# Start PostgreSQL
brew services start postgresql@17  # macOS
sudo systemctl start postgresql    # Linux
docker start allergen-postgres     # Docker
```

**Problem:** `pgvector extension not found`
```bash
# Install pgvector
psql -d allergen_db -c "CREATE EXTENSION vector"

# If permission denied
sudo -u postgres psql -d allergen_db -c "CREATE EXTENSION vector"
```

### Build Issues

**Problem:** `BUILD FAILURE` - Dependencies not downloading
```bash
# Clear Maven cache
mvn clean
mvn dependency:purge-local-repository
mvn clean install
```

**Problem:** `Lombok not working`
1. Install Lombok plugin in IDE
2. Enable annotation processing
3. Restart IDE

### API Issues

**Problem:** `OpenAI API key invalid`
```bash
# Verify key is set
echo $OPENAI_API_KEY

# If empty, set it
export OPENAI_API_KEY="sk-your-key"

# Restart application
```

**Problem:** `Port 8080 already in use`
```yaml
# In application.properties
server.port=8081
```

Or kill existing process:
```bash
# Find process
lsof -i :8080

# Kill process (replace PID)
kill -9 <PID>
```

---

## Useful Commands

### Maven Commands
```bash
mvn clean                    # Remove build artifacts
mvn compile                  # Compile source code
mvn test                     # Run tests
mvn package                  # Create JAR file
mvn spring-boot:run         # Run application
mvn dependency:tree         # View dependency tree
mvn versions:display-dependency-updates  # Check for updates
```

### PostgreSQL Commands
```bash
psql -l                      # List all databases
psql -d allergen_db         # Connect to database
\dt                         # List tables in current schema
\dt allergen.*              # List tables in allergen schema
\d table_name               # Describe table structure
\dx                         # List extensions
\q                          # Quit psql
```

### Docker Commands
```bash
docker ps                           # List running containers
docker logs allergen-postgres       # View logs
docker exec -it allergen-postgres bash  # Enter container
docker stop allergen-postgres       # Stop container
docker start allergen-postgres      # Start container
docker rm allergen-postgres         # Remove container
```

### Git Commands
```bash
git status                   # Check status
git add .                    # Stage all changes
git commit -m "message"      # Commit changes
git push                     # Push to remote
git pull                     # Pull latest changes
git log --oneline           # View commit history
git branch                   # List branches
```

---

## Development Best Practices

### Code Style

**Use Conventional Commits:**
```bash
feat: add new feature
fix: resolve bug
docs: update documentation
test: add test cases
refactor: improve code structure
chore: update dependencies
```

**Follow Java Naming Conventions:**
- Classes: `PascalCase`
- Methods/Variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase`

### Testing Strategy

1. **Unit Tests**: Test services in isolation
2. **Integration Tests**: Test with real database
3. **API Tests**: Test REST endpoints
4. **Coverage Goal**: Maintain >80%

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/chemical-intelligence

# Make changes and commit
git add .
git commit -m "feat: implement oxidation product detection"

# Push branch
git push -u origin feature/chemical-intelligence

# Create Pull Request on GitHub
# After review, merge to main
```

---

## IDE Configuration

### IntelliJ IDEA Setup

**1. Install Required Plugins:**
- Lombok
- Spring Boot
- Database Navigator (optional)

**2. Configure Project:**
- File → Project Structure → Project SDK → 21
- File → Settings → Build → Compiler → Annotation Processors → Enable

**3. Run Configuration:**
- Run → Edit Configurations
- Add → Spring Boot
- Main class: `AllergenIntelligenceApplication`
- Active profiles: `dev`

### VS Code Setup

**1. Install Extensions:**
- Extension Pack for Java
- Spring Boot Extension Pack
- Lombok Annotations Support

**2. Configure settings.json:**
```json
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.compile.nullAnalysis.mode": "automatic"
}
```

---

## Database Management Tools

### Command Line (psql)

Best for quick queries and admin tasks.

### pgAdmin

**Install:**
```bash
# macOS
brew install --cask pgadmin4

# Windows/Linux - Download from:
# https://www.pgadmin.org/download/
```

**Connect:**
1. Launch pgAdmin
2. Create new server connection
3. Host: localhost, Port: 5432
4. Database: allergen_db

### DBeaver (Free, Multi-Platform)

**Download:** https://dbeaver.io/download/

Recommended for visual database exploration and query building.

---

## Performance Monitoring

### Application Metrics

Access at: `http://localhost:8080/actuator/metrics`

**View Specific Metrics:**
```bash
# HTTP requests
curl http://localhost:8080/actuator/metrics/http.server.requests

# JVM memory
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Database connections
curl http://localhost:8080/actuator/metrics/hikari.connections
```

### Database Performance

```sql
-- View active queries
SELECT * FROM pg_stat_activity;

-- View table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- View index usage
SELECT * FROM pg_stat_user_indexes;
```

---

## Resources

### Official Documentation
- **Spring Boot**: https://spring.io/projects/spring-boot
- **Spring AI**: https://docs.spring.io/spring-ai/reference/
- **pgvector**: https://github.com/pgvector/pgvector
- **OpenAI**: https://platform.openai.com/docs/

### Learning Resources
- **Spring Boot Tutorial**: https://www.baeldung.com/spring-boot
- **REST API Design**: https://restfulapi.net/
- **Vector Databases**: https://www.pinecone.io/learn/vector-database/
- **RAG Architecture**: https://www.anthropic.com/research/retrieval-augmented-generation

### Community
- **Stack Overflow**: Use tags `spring-boot`, `spring-ai`, `pgvector`
- **Spring AI GitHub**: https://github.com/spring-projects/spring-ai
- **Discord/Slack**: Join Spring community channels

---

## Next Steps

### Week 1 Goals

1. **Complete PubChem Service**
    - Implement all API methods
    - Add error handling
    - Write tests

2. **Build Repository Layer**
    - Create custom queries
    - Test database operations

3. **Create REST Endpoints**
    - Implement CRUD operations
    - Add validation
    - Document with Swagger

### Development Checklist

- [ ] Environment variables configured
- [ ] Database connection verified
- [ ] Application runs successfully
- [ ] First test passes
- [ ] Health endpoint responds
- [ ] PubChem integration works
- [ ] Git repository initialized
- [ ] IDE configured properly

---

## Getting Help

### Debug Checklist

1. **Check Logs**: Look in console output for errors
2. **Verify Environment**: Ensure all env variables are set
3. **Test Database**: Confirm PostgreSQL is running and accessible
4. **Check Dependencies**: Run `mvn dependency:tree`
5. **Review Configuration**: Verify application.properties settings

### Common Solutions

**Application won't start:**
1. Check Java version: `java -version`
2. Verify Maven build: `mvn clean install`
3. Check port availability: `lsof -i :8080`

**Tests failing:**
1. Ensure test database exists
2. Check H2 in-memory DB configuration (for unit tests)
3. Verify test resources

**API not responding:**
1. Check application logs
2. Verify endpoint mapping
3. Test with Postman/curl

---

## Conclusion

You now have a complete local development environment for the Allergen Intelligence Platform. The application should be running with database connectivity, API endpoints responding, and tests passing.

**Next:** Review the [ROADMAP.md](ROADMAP.md) for detailed development phases and start implementing Phase 1 features.

Happy coding! 🚀