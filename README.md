# Allergen Intelligence Platform

> Solving the chemical translation problem in allergen identification using RAG and vector search

**Status:** 🚧 Active Development - Phase 1 (Foundation)

## The Problem

Product ingredient labels list common chemical names like "Limonene" - but the actual allergen is often **"Limonene hydroperoxide"** (an oxidation product formed when the ingredient degrades). Consumers unknowingly search for side effects of the wrong compound, missing critical safety information about what's actually causing their reactions.

**No existing tools make this chemical translation automatically.**

## My Solution

A RAG-powered platform that bridges the gap between ingredient labels and actual allergen information:

- **Chemical Translation**: Uses PubChem API to identify correct chemical structures and CAS numbers
- **Oxidation Product Mapping**: Automatically identifies allergenic breakdown products (e.g., limonene → limonene hydroperoxide)
- **Clinical Research Synthesis**: Leverages OpenAI web search to find and synthesize peer-reviewed allergen data
- **Intelligent Retrieval**: Stores findings in pgvector for semantic search and context-aware responses
- **Source Verification**: Every claim includes URL attribution to original research

## Tech Stack

- **Backend**: Spring Boot 3.x, Java 21
- **AI/ML**: Spring AI, OpenAI GPT-4, text-embedding-3-small
- **Database**: PostgreSQL 17 + pgvector extension
- **APIs**: PubChem REST API, OpenAI API
- **Future**: React/Next.js frontend, Tesseract OCR

## Project Roadmap

### ✅ Completed (Phase 0)
- [x] Project architecture design
- [x] PostgreSQL 17 + pgvector installation and configuration
- [x] Spring Boot project initialization
- [x] Core data models (ChemicalIdentification, SideEffect)
- [x] Repository layer design
- [x] Configuration setup (application.yml, vector store)

### 🔄 In Progress (Phase 1 - Foundation)
- [ ] PubChem service implementation
- [ ] Chemical data extraction and parsing
- [ ] Basic REST API endpoints
- [ ] Repository implementations
- [ ] Unit test suite

### 📋 Upcoming Phases

**Phase 2: AI Integration** (Week 2)
- OpenAI client configuration
- Web search service with source attribution
- Vector store integration
- Embedding generation pipeline

**Phase 3: Chemical Intelligence** (Week 3)
- Allergen identification logic
- Oxidation product mapping algorithms
- Chemical family classification
- Knowledge base bootstrapping

**Phase 4: RAG Pipeline** (Week 4)
- Context retrieval from vector store
- Prompt engineering for comprehensive reports
- Response validation
- Citation and source tracking

**Phase 5: Image Processing** (Week 5)
- GPT-4 Vision integration for ingredient extraction
- OCR fallback with Tesseract
- Image preprocessing pipeline

**Phase 6: Polish & Deploy** (Week 6)
- Comprehensive error handling
- OpenAPI documentation
- Performance optimization
- Production deployment

**Phase 7: Frontend** (Week 7-8)
- Next.js application
- Image upload interface
- Results visualization
- Responsive design

[View detailed roadmap →](docs/ROADMAP.md)

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 15+ with pgvector extension
- OpenAI API key

### Installation

```bash
# Clone repository
git clone https://github.com/yourusername/allergen-intelligence.git
cd allergen-intelligence

# Setup database
createdb allergen_db
psql -d allergen_db -c "CREATE EXTENSION vector"

# Configure environment
export OPENAI_API_KEY="your-openai-api-key"
export DB_USERNAME="your-db-username"

# Build and run
mvn clean install
mvn spring-boot:run
```

### Verify Installation

```bash
# Test health endpoint
curl http://localhost:8080/api/test/health

# Test PubChem integration
curl http://localhost:8080/api/test/chemical/Limonene
```

[Detailed setup guide →](docs/GETTING_STARTED.md)

## Architecture Overview

```
User Input (Product/Ingredients)
    ↓
Ingredient Extraction
    ↓
PubChem API Lookup (structure, CAS, synonyms)
    ↓
Chemical Intelligence Layer (oxidation products, allergens)
    ↓
OpenAI Web Search (clinical effects, sources)
    ↓
pgvector Storage (embeddings + metadata)
    ↓
RAG-Enhanced Report Generation (with citations)
```

### Key Innovation: Automated Chemical Translation

**Traditional Approach**: User must manually know that limonene oxidizes to allergenic peroxides

**This System**:
1. User inputs "Limonene"
2. System identifies it oxidizes to "Limonene hydroperoxide"
3. Searches for side effects of BOTH compounds
4. Returns comprehensive, properly attributed allergen information

This leverages my chemistry background to solve a real-world problem that affects millions of consumers.

## API Preview

```bash
# Analyze product ingredients
curl -X POST http://localhost:8080/api/v1/allergens/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Citrus Body Lotion",
    "ingredients": ["Limonene", "Linalool", "Citral"]
  }'

# Response includes:
# - Chemical identifications with CAS numbers
# - Oxidation products
# - Side effects with severity ratings
# - Source URLs for verification
# - Overall risk assessment
```

[Full API documentation →](docs/API.md)

## Why This Project Matters

As someone with a background in chemistry, I recognized a critical gap: **ingredient labels don't reflect the actual chemicals that cause allergic reactions.** This platform bridges that knowledge gap using:

- Domain expertise (organic chemistry, allergen science)
- Modern AI techniques (RAG, embeddings, LLMs)
- Production-grade architecture (vector databases, REST APIs)

### What This Demonstrates

- **Full-stack development**: Backend APIs, future React frontend
- **AI/ML integration**: RAG pipelines, vector search, prompt engineering
- **Complex domain modeling**: Chemistry + healthcare data structures
- **API design**: RESTful services, OpenAPI documentation
- **Database expertise**: PostgreSQL, vector similarity search
- **Real-world problem solving**: Translating domain expertise into software

## Contributing

This is primarily a portfolio project, but I welcome:
- Bug reports and feature suggestions
- Code review feedback
- Chemistry/allergen domain knowledge
- Ideas for additional use cases

Feel free to open an issue or submit a pull request!

## Disclaimers

⚠️ **NOT MEDICAL ADVICE**: This tool is for educational and research purposes only. Always consult qualified healthcare professionals for medical decisions. Do not rely on this tool for allergy diagnosis or treatment.

⚠️ **NO WARRANTY**: This software is provided "as is" without warranty of any kind, express or implied. Use at your own risk.

⚠️ **ACCURACY NOT GUARANTEED**: Information may be incomplete, outdated, or incorrect. Chemical and medical data should always be verified with authoritative sources and medical professionals.

## License

MIT License - See [LICENSE](LICENSE) for details

---

**Built by Matthew Bixby**  
[LinkedIn](#) | [Portfolio](#) | [Email](mailto:your-email@example.com)

*Combining chemistry expertise with software engineering to solve real-world problems*