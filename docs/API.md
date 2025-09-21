# Allergen Intelligence Platform - API Documentation

**Status:** Planning Phase - API Not Yet Implemented

**Base URL:** `http://localhost:8080/api/v1` (planned)

---

## Overview

This document outlines the planned REST API for the Allergen Intelligence Platform. The API will analyze product ingredients for potential allergens and identify oxidation products that may cause allergic reactions.

**Current State:** Foundation phase - database and Spring Boot setup complete

---

## Implementation Progress

### ✅ Phase 0: Foundation (Complete)
- [x] Spring Boot application running
- [x] PostgreSQL 17 with pgvector configured
- [x] Database connection verified
- [x] Vector store schema created

### 🔄 Phase 1: In Progress
- [ ] PubChem service implementation
- [ ] Basic REST endpoints
- [ ] Data models and DTOs
- [ ] Repository layer

### 📋 Phases 2-7: Planned
- AI integration with OpenAI
- Caching layer in pgvector
- Analysis pipeline
- Image processing
- Frontend

---

## Planned API Endpoints

### Core Endpoints (Design Phase)

#### 1. Product Analysis
```
POST /api/v1/allergens/analyze
```

**Purpose:** Analyze product ingredients for allergens and oxidation products

**Planned Features:**
- PubChem chemical lookup
- Oxidation product identification
- OpenAI-powered research synthesis
- Response caching in pgvector (to reduce API costs)

**Request Structure (TBD):**
```json
{
  "productName": "string",
  "ingredients": ["string"],
  "category": "string (optional)"
}
```

**Design Decisions to Make:**
- Response format based on PubChem data structure
- How to represent oxidation products
- Cache hit/miss indication
- Source attribution format

---

#### 2. Chemical Information Lookup
```
GET /api/v1/chemical/{name}
```

**Purpose:** Get detailed chemical information from PubChem

**Status:** Research phase - understanding PubChem API

**Questions to Answer:**
- What PubChem data do we need?
- How to handle multiple CAS numbers?
- Caching strategy for chemical lookups?

---

#### 3. Image Analysis (Future)
```
POST /api/v1/allergens/analyze-image
```

**Purpose:** Extract ingredients from product photos

**Phase:** Not started (Phase 5)

---

### Health & Monitoring

#### Current Endpoints (Implemented)

```
GET /actuator/health
```
Returns application health status - **Working Now**

**Response:**
```json
{
  "status": "UP"
}
```

---

## Design Considerations

### 1. Caching Strategy

**Goal:** Reduce OpenAI API costs by 99%

**Planned Implementation:**
- Store OpenAI responses in pgvector
- Check cache before making API calls
- 30-day TTL for cached data
- Similarity threshold: 0.95 for cache hits

**Cost Impact:**
- First query: ~$0.002-0.065
- Cached queries: $0.00
- Expected savings: >95% during development

### 2. Response Format Philosophy

**Principles:**
- Include source URLs for all claims
- Show cache status in development mode
- Clear error messages
- Consistent JSON structure

**Example Response Structure (Draft):**
```json
{
  "data": { /* actual response */ },
  "_meta": {
    "cached": true/false,
    "timestamp": "ISO-8601",
    "apiCalls": 0,
    "sources": ["url1", "url2"]
  }
}
```

### 3. Error Handling

**Planned Approach:**
- Standard HTTP status codes
- Detailed error messages for debugging
- Graceful degradation when external APIs fail

---

## Current Working Endpoints

These endpoints exist for testing during development:

```
GET /api/test/health
```
Returns: "Allergen Intelligence Platform is running!"

```
GET /api/test/chemical/{name}
```
Tests PubChem lookup (not yet implemented)

---

## Next Steps

### Week 1: PubChem Integration
1. Implement PubChem service
2. Design response DTOs based on actual data
3. Create basic chemical lookup endpoint
4. Document data structures

### Week 2: Caching & AI
1. Implement pgvector caching layer
2. OpenAI client setup
3. Design analysis workflow
4. Test cache effectiveness

### Week 3+: Main Features
1. Build main analysis endpoint
2. Add oxidation product detection
3. Implement RAG pipeline
4. Finalize API contracts

---

## Development Notes

### Questions to Resolve

1. **Data Model:**
    - What PubChem fields are essential?
    - How to structure oxidation product relationships?
    - Best way to represent side effects?

2. **Caching:**
    - When to invalidate cache?
    - How to handle partial cache hits?
    - Cache warming strategy?

3. **Performance:**
    - Acceptable response time?
    - Rate limiting strategy?
    - Batch processing design?

---

## Testing Strategy

Once implemented, APIs will be tested with:

```bash
# Chemical lookup
curl http://localhost:8080/api/v1/chemical/Limonene

# Product analysis
curl -X POST http://localhost:8080/api/v1/allergens/analyze \
  -H "Content-Type: application/json" \
  -d '{"productName":"Test","ingredients":["Limonene"]}'
```

---

## Documentation Updates

This document will be updated as:
- [ ] PubChem integration completes (Week 1)
- [ ] First endpoints are implemented (Week 1)
- [ ] Caching layer is built (Week 2)
- [ ] Analysis pipeline is complete (Week 4)
- [ ] API stabilizes for production (Week 6)

---

## References

- [ROADMAP.md](ROADMAP.md) - Detailed development plan
- [GETTING_STARTED.md](GETTING_STARTED.md) - Setup instructions
- [PubChem API Docs](https://pubchem.ncbi.nlm.nih.gov/docs/pug-rest)
- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/)

---

**Last Updated:** September 21, 2025  
**Current Phase:** Foundation complete, starting PubChem integration  
**Next Milestone:** Basic chemical lookup endpoint

**POST** `/allergens/analyze`

Analyze a product's ingredients for potential allergens and side effects.

**Request Body:**
```json
{
  "productName": "Citrus Body Lotion",
  "ingredients": [
    "Limonene",
    "Linalool",
    "Citral"
  ],
  "category": "skincare"
}
```

**Response:**
```json
{
  "productName": "Citrus Body Lotion",
  "overallRisk": "MODERATE",
  "ingredients": [
    {
      "originalName": "Limonene",
      "chemical": {
        "id": 1,
        "commonName": "Limonene",
        "scientificName": "D-Limonene",
        "casNumber": "5989-27-5",
        "iupacName": "1-Methyl-4-(1-methylethenyl)cyclohexene",
        "chemicalFamily": "Terpene",
        "isOxidationProduct": false,
        "oxidationProducts": [
          "Limonene hydroperoxide",
          "Limonene oxide"
        ]
      },
      "sideEffects": [
        {
          "effectType": "Contact Dermatitis",
          "severity": "MODERATE",
          "description": "Can cause allergic skin reactions",
          "prevalenceRate": 0.15,
          "affectedBodyAreas": ["skin"],
          "source": "Journal of Clinical Dermatology",
          "sourceUrl": "https://example.com/study",
          "studyEvidence": "15% prevalence in patch test studies"
        }
      ],
      "oxidationProductEffects": [
        {
          "effectType": "Allergic Contact Dermatitis",
          "severity": "HIGH",
          "description": "Limonene hydroperoxide is a known sensitizer",
          "prevalenceRate": 0.35,
          "affectedBodyAreas": ["skin"],
          "source": "Contact Dermatitis Journal",
          "sourceUrl": "https://example.com/study2"
        }
      ],
      "riskLevel": "MODERATE"
    }
  ],
  "report": "**Executive Summary**\n\nThis product contains 3 ingredients...",
  "recommendations": [
    "Avoid if sensitive to citrus terpenes",
    "Use fresh products (oxidation increases allergenicity)",
    "Patch test before full application"
  ],
  "generatedAt": "2025-09-21T15:30:00"
}
```

**Status Codes:**
- `200 OK` - Analysis successful
- `400 Bad Request` - Invalid input
- `500 Internal Server Error` - Processing error

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/allergens/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Citrus Body Lotion",
    "ingredients": ["Limonene", "Linalool"],
    "category": "skincare"
  }'
```

---

### 2. Analyze Product from Image

**POST** `/allergens/analyze-image`

Extract ingredients from a product image and analyze for allergens.

**Request:**
- **Method:** POST
- **Content-Type:** `multipart/form-data`

**Parameters:**
- `image` (file, required): Product ingredient label photo
- `productName` (string, optional): Product name

**Response:**
```json
{
  "extractedIngredients": [
    "Aqua",
    "Limonene",
    "Linalool",
    "Citric Acid"
  ],
  "productName": "Citrus Body Lotion",
  "overallRisk": "MODERATE",
  "ingredients": [...],
  "report": "...",
  "recommendations": [...]
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/allergens/analyze-image \
  -F "image=@product_label.jpg" \
  -F "productName=My Product"
```

---

### 3. Get Chemical Information

**GET** `/allergens/chemical/{ingredient}`

Retrieve detailed information about a specific chemical/ingredient.

**Path Parameters:**
- `ingredient` (string): Chemical name (e.g., "Limonene")

**Response:**
```json
{
  "commonName": "Limonene",
  "scientificName": "D-Limonene",
  "casNumber": "5989-27-5",
  "iupacName": "1-Methyl-4-(1-methylethenyl)cyclohexene",
  "molecularFormula": "C10H16",
  "chemicalFamily": "Terpene",
  "isKnownAllergen": true,
  "oxidationProducts": [
    {
      "name": "Limonene hydroperoxide",
      "allergenicity": "HIGH",
      "formationConditions": "Air exposure, UV light"
    }
  ],
  "synonyms": [
    "D-Limonene",
    "Dipentene",
    "Cinene"
  ],
  "regulatoryStatus": {
    "eu": "Required labeling as allergen",
    "fda": "GRAS (Generally Recognized as Safe)"
  }
}
```

**Example:**
```bash
curl http://localhost:8080/api/v1/allergens/chemical/Limonene
```

---

### 4. Semantic Search

**GET** `/allergens/search`

Search for chemicals similar to a query using semantic similarity.

**Query Parameters:**
- `query` (string, required): Search term
- `limit` (integer, optional): Maximum results (default: 10)
- `threshold` (float, optional): Similarity threshold 0-1 (default: 0.7)

**Response:**
```json
{
  "query": "citrus allergen",
  "results": [
    {
      "chemical": "Limonene",
      "similarityScore": 0.92,
      "casNumber": "5989-27-5",
      "isAllergen": true,
      "summary": "Primary citrus terpene allergen"
    },
    {
      "chemical": "Linalool",
      "similarityScore": 0.87,
      "casNumber": "78-70-6",
      "isAllergen": true,
      "summary": "Common fragrance allergen"
    }
  ]
}
```

**Example:**
```bash
curl "http://localhost:8080/api/v1/allergens/search?query=citrus%20allergen&limit=5"
```

---

### 5. Batch Analysis

**POST** `/allergens/batch`

Analyze multiple products in a single request.

**Request Body:**
```json
{
  "products": [
    {
      "productName": "Product A",
      "ingredients": ["Limonene", "Linalool"]
    },
    {
      "productName": "Product B",
      "ingredients": ["Citral", "Geraniol"]
    }
  ]
}
```

**Response:**
```json
{
  "batchId": "batch_abc123",
  "totalProducts": 2,
  "results": [
    {
      "productName": "Product A",
      "overallRisk": "MODERATE",
      "summary": "Contains 2 potential allergens"
    },
    {
      "productName": "Product B",
      "overallRisk": "LOW",
      "summary": "Contains 1 mild allergen"
    }
  ],
  "processedAt": "2025-09-21T15:30:00"
}
```

---

### 6. Get Oxidation Products

**GET** `/allergens/chemical/{ingredient}/oxidation-products`

Get oxidation products for a specific chemical.

**Response:**
```json
{
  "parentChemical": "Limonene",
  "oxidationProducts": [
    {
      "name": "Limonene hydroperoxide",
      "casNumber": "5489-17-0",
      "allergenicity": "HIGH",
      "prevalence": 0.35,
      "formationFactors": [
        "Air exposure",
        "UV light",
        "Temperature",
        "Time"
      ]
    }
  ],
  "preventionTips": [
    "Store in airtight containers",
    "Avoid heat and light",
    "Use within 6 months of opening"
  ]
}
```

---

## Data Models

### ChemicalIdentification

```typescript
interface ChemicalIdentification {
  id: number;
  commonName: string;
  scientificName: string;
  iupacName?: string;
  casNumber: string;
  chemicalFamily: string;
  isOxidationProduct: boolean;
  synonyms: string[];
  oxidationProducts: string[];
  smiles?: string;
  createdAt: string;
}
```

### SideEffect

```typescript
interface SideEffect {
  id: number;
  effectType: string;
  severity: "LOW" | "MODERATE" | "HIGH" | "SEVERE";
  description: string;
  prevalenceRate?: number;
  affectedBodyAreas: string[];
  source: string;
  sourceUrl: string;
  studyEvidence?: string;
  createdAt: string;
}
```

### RiskLevel

```typescript
type RiskLevel = "NONE" | "LOW" | "MODERATE" | "HIGH" | "SEVERE";
```

---

## Error Handling

All errors follow this format:

```json
{
  "timestamp": "2025-09-21T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid ingredient name",
  "path": "/api/v1/allergens/chemical/InvalidName"
}
```

### Common Error Codes

| Code | Meaning |
|------|---------|
| 400 | Invalid request parameters |
| 404 | Chemical/resource not found |
| 429 | Rate limit exceeded |
| 500 | Internal server error |
| 503 | External API unavailable (PubChem, OpenAI) |

---

## Rate Limiting

**Current:** No rate limiting (development)

**Future Production Limits:**
- Free tier: 100 requests/hour
- Basic tier: 1,000 requests/hour
- Premium tier: 10,000 requests/hour

---

## Examples

### Complete Product Analysis Workflow

```bash
# 1. Analyze product
curl -X POST http://localhost:8080/api/v1/allergens/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Lavender Hand Cream",
    "ingredients": ["Linalool", "Limonene", "Geraniol"]
  }' | jq

# 2. Get details on specific allergen
curl http://localhost:8080/api/v1/allergens/chemical/Linalool | jq

# 3. Check oxidation products
curl http://localhost:8080/api/v1/allergens/chemical/Linalool/oxidation-products | jq

# 4. Search for similar allergens
curl "http://localhost:8080/api/v1/allergens/search?query=lavender%20allergen" | jq
```

### Image Analysis

```bash
# Upload product photo
curl -X POST http://localhost:8080/api/v1/allergens/analyze-image \
  -F "image=@hand_cream_label.jpg" \
  -F "productName=Lavender Hand Cream" | jq
```

---

## SDK Examples

### JavaScript/TypeScript

```typescript
// Using fetch
async function analyzeProduct(productName: string, ingredients: string[]) {
  const response = await fetch('http://localhost:8080/api/v1/allergens/analyze', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      productName,
      ingredients,
      category: 'skincare'
    })
  });
  
  return await response.json();
}

// Usage
const result = await analyzeProduct('My Product', ['Limonene', 'Linalool']);
console.log(result.overallRisk);
```

### Python

```python
import requests

def analyze_product(product_name: str, ingredients: list):
    url = "http://localhost:8080/api/v1/allergens/analyze"
    payload = {
        "productName": product_name,
        "ingredients": ingredients,
        "category": "skincare"
    }
    
    response = requests.post(url, json=payload)
    return response.json()

# Usage
result = analyze_product("My Product", ["Limonene", "Linalool"])
print(f"Risk Level: {result['overallRisk']}")
```

### cURL

```bash
#!/bin/bash

# Function to analyze product
analyze_product() {
  local product_name="$1"
  shift
  local ingredients="$@"
  
  curl -X POST http://localhost:8080/api/v1/allergens/analyze \
    -H "Content-Type: application/json" \
    -d "{
      \"productName\": \"$product_name\",
      \"ingredients\": [\"$(echo $ingredients | sed 's/ /","/g')\"]
    }"
}

# Usage
analyze_product "My Product" "Limonene" "Linalool"
```

---

## Testing

### Postman Collection

Import this collection to test the API:

```json
{
  "info": {
    "name": "Allergen Intelligence Platform",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Analyze Product",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/allergens/analyze",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"productName\": \"Test Product\",\n  \"ingredients\": [\"Limonene\"]\n}"
        }
      }
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080/api/v1"
    }
  ]
}
```

### Test Data

```json
{
  "testProducts": [
    {
      "name": "Simple Test",
      "ingredients": ["Limonene"],
      "expectedRisk": "LOW"
    },
    {
      "name": "Multiple Allergens",
      "ingredients": ["Limonene", "Linalool", "Citral"],
      "expectedRisk": "MODERATE"
    },
    {
      "name": "High Risk",
      "ingredients": ["Limonene hydroperoxide", "Linalool oxide"],
      "expectedRisk": "HIGH"
    }
  ]
}
```

---

## Changelog

### Version 1.0.0 (Current - Development)
- Initial API design
- Basic product analysis
- Chemical lookup
- Image processing (planned)
- Semantic search (planned)

### Planned Features (v1.1.0)
- User authentication
- Rate limiting
- Batch processing
- Caching improvements
- Real-time notifications

---

## Support

### Documentation
- **Full Docs:** [README.md](README.md)
- **Setup Guide:** [GETTING_STARTED.md](GETTING_STARTED.md)
- **Roadmap:** [ROADMAP.md](ROADMAP.md)

### Contact
- **Issues:** GitHub Issues
- **Email:** support@allergen-intelligence.com (planned)
- **Discord:** Coming soon

---

## Disclaimer

⚠️ **NOT MEDICAL ADVICE**: This API is for educational and research purposes only. Always consult qualified healthcare professionals for medical decisions. Do not rely on this API for allergy diagnosis or treatment.

The information provided may be incomplete, outdated, or incorrect. Always verify with authoritative medical sources and professionals.

---

**Last Updated:** September 21, 2025  
**API Version:** 1.0.0-dev  
**Spring Boot Version:** 3.5.6  
**Spring AI Version:** 1.0.2