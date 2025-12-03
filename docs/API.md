# Allergen Intelligence Platform - API Documentation

**Version:** 1.0.0  
**Status:** Production Ready  
**Base URL:** `http://localhost:8080/api`

---

## Table of Contents
- [Overview](#overview)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
   - [Authentication Endpoints](#authentication-endpoints)
   - [Allergen Analysis Endpoints](#allergen-analysis-endpoints)
- [Data Models](#data-models)
- [Error Handling](#error-handling)
- [Rate Limiting & Usage Tracking](#rate-limiting--usage-tracking)
- [Code Examples](#code-examples)
- [Testing](#testing)

---

## Overview

The Allergen Intelligence Platform provides a REST API for analyzing product ingredients for potential allergens and their oxidation products. The platform features:

- 🔐 **JWT Authentication** with token rotation
- 💰 **Three-tier caching** (97.5% cost reduction)
- 📊 **Real-time usage tracking** (tokens, cost, analyses)
- 🧪 **Automatic oxidation product detection**
- 🔬 **PubChem chemical data integration**
- 🤖 **OpenAI-powered allergen research**

### Key Features

- **Database Cache:** <10ms response, 0 tokens
- **Vector Cache (pgvector):** ~100ms response, 0 tokens
- **OpenAI API:** 5-10s response, ~400 tokens per new ingredient
- **Shared Cache:** Every user's query benefits everyone (network effect)

---

## Authentication

The API uses **JWT (JSON Web Tokens)** with a two-token system:
- **Access Token:** 1 hour expiration, used for API requests
- **Refresh Token:** 7 days expiration, used to obtain new access tokens

### Security Features
- BCrypt password hashing
- Stateless session management
- Token rotation on refresh
- Secure token storage in PostgreSQL

### Authorization Header

All protected endpoints require:
```
Authorization: Bearer {accessToken}
```

---

## API Endpoints

### Authentication Endpoints

#### 1. Register User

Create a new user account.

**Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response:** `200 OK`
```json
{
  "message": "User registered successfully"
}
```

**Status Codes:**
- `200 OK` - Registration successful
- `400 Bad Request` - Invalid input or email already exists
- `500 Internal Server Error` - Server error

**Example:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

---

#### 2. Login

Authenticate and receive access and refresh tokens.

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Response Fields:**
- `accessToken` - JWT token for API requests (1 hour)
- `refreshToken` - Token for refreshing access token (7 days)
- `tokenType` - Always "Bearer"
- `expiresIn` - Access token expiration in seconds

**Status Codes:**
- `200 OK` - Login successful
- `401 Unauthorized` - Invalid credentials
- `500 Internal Server Error` - Server error

**Example:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

---

#### 3. Refresh Token

Obtain a new access token using a refresh token.

**Endpoint:** `POST /api/auth/refresh`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Behavior:**
- Old refresh token is automatically revoked
- New access token is issued
- New refresh token is issued (token rotation)

**Status Codes:**
- `200 OK` - Token refreshed successfully
- `401 Unauthorized` - Invalid or expired refresh token
- `500 Internal Server Error` - Server error

**Example:**
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGc..."
  }'
```

---

#### 4. Get Current User

Get authenticated user information and usage statistics.

**Endpoint:** `GET /api/auth/me`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response:** `200 OK`
```json
{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "createdAt": "2025-01-15T10:30:00Z",
  "usage": {
    "totalTokensUsed": 26460,
    "estimatedCost": 0.1323,
    "analysesRun": 58
  }
}
```

**Response Fields:**
- `usage.totalTokensUsed` - Cumulative OpenAI tokens consumed
- `usage.estimatedCost` - Estimated cost in USD based on GPT-4o pricing
- `usage.analysesRun` - Total number of analyses performed

**Status Codes:**
- `200 OK` - User data retrieved
- `401 Unauthorized` - Invalid or expired token
- `500 Internal Server Error` - Server error

**Example:**
```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGc..."
```

---

### Allergen Analysis Endpoints

#### 5. Analyze Single Ingredient

Analyze a single ingredient for allergen risks and oxidation products.

**Endpoint:** `GET /api/allergen/analyze/{ingredientName}`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Path Parameters:**
- `ingredientName` - Name of the ingredient (e.g., "Limonene")

**Response:** `200 OK`
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
      "affectedBodyAreas": ["Skin"],
      "sources": [
        {
          "title": "Contact Dermatitis Study",
          "url": "https://example.com/study",
          "year": 2023
        }
      ]
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
  ],
  "disclaimer": "MEDICAL DISCLAIMER: This information is for educational purposes only. Always consult qualified healthcare professionals for medical decisions."
}
```

**Caching Behavior:**
- ✅ **Database Hit:** <10ms, 0 tokens
- ✅ **Vector Hit:** ~100ms, 0 tokens (semantic match)
- ⚠️ **API Call:** 5-10s, ~400 tokens (new ingredient)

**Status Codes:**
- `200 OK` - Analysis successful
- `401 Unauthorized` - Invalid/expired token
- `404 Not Found` - Chemical not found in PubChem
- `500 Internal Server Error` - Server error

**Example:**
```bash
curl http://localhost:8080/api/allergen/analyze/Limonene \
  -H "Authorization: Bearer eyJhbGc..."
```

---

#### 6. Analyze Product

Analyze a complete product by name, automatically extracting ingredients.

**Endpoint:** `POST /api/allergen/analyze-product`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body:**
```json
{
  "productName": "CeraVe Moisturizing Cream"
}
```

**Response:** `200 OK`
```json
{
  "productName": "CeraVe Moisturizing Cream",
  "totalIngredients": 20,
  "highRiskIngredients": 2,
  "overallRiskLevel": "MODERATE",
  "ingredients": [
    "Cetearyl Alcohol",
    "Glycerin",
    "Petrolatum",
    "Ceramide NP",
    "..."
  ],
  "detailedAnalysis": {
    "Cetearyl Alcohol": {
      "chemical": {
        "commonName": "Cetearyl Alcohol",
        "iupacName": "Hexadecan-1-ol",
        "casNumber": "36653-82-4",
        "molecularFormula": "C16H34O"
      },
      "sideEffects": [
        {
          "effectType": "Mild Skin Irritation",
          "severity": "LOW",
          "prevalenceRate": 0.01
        }
      ],
      "oxidationProducts": [],
      "riskLevel": "LOW"
    },
    "Fragrance": {
      "chemical": {
        "commonName": "Fragrance",
        "iupacName": "Mixed fragrance compounds"
      },
      "sideEffects": [
        {
          "effectType": "Allergic Contact Dermatitis",
          "severity": "HIGH",
          "prevalenceRate": 0.15
        }
      ],
      "oxidationProducts": [
        "Various oxidation products"
      ],
      "riskLevel": "HIGH"
    }
  },
  "recommendations": [
    "⚠️ This product contains 2 high-risk allergen(s)",
    "Consult with a dermatologist before use if you have known allergies",
    "Perform a patch test on inner arm for 48 hours before facial application"
  ],
  "disclaimer": "MEDICAL DISCLAIMER: This information is for educational purposes only..."
}
```

**Performance:**
- **First Analysis:** ~4,100 tokens (~$0.021)
- **Repeat Analysis:** ~100 tokens (~$0.0005)
- **Cost Savings:** 97.5%

**Status Codes:**
- `200 OK` - Analysis successful
- `400 Bad Request` - Invalid product name
- `401 Unauthorized` - Invalid/expired token
- `500 Internal Server Error` - Server error or OpenAI API failure

**Example:**
```bash
curl -X POST http://localhost:8080/api/allergen/analyze-product \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "CeraVe Moisturizing Cream"
  }'
```

---

#### 7. Batch Ingredient Analysis

Analyze multiple ingredients in a single request.

**Endpoint:** `POST /api/allergen/analyze-batch`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body:**
```json
[
  "Limonene",
  "Linalool",
  "Citral"
]
```

**Response:** `200 OK`
```json
{
  "Limonene": {
    "chemical": { ... },
    "sideEffects": [ ... ],
    "oxidationProducts": [ ... ],
    "riskLevel": "MODERATE"
  },
  "Linalool": {
    "chemical": { ... },
    "sideEffects": [ ... ],
    "oxidationProducts": [ ... ],
    "riskLevel": "MODERATE"
  },
  "Citral": {
    "chemical": { ... },
    "sideEffects": [ ... ],
    "oxidationProducts": [ ... ],
    "riskLevel": "LOW"
  },
  "summary": {
    "totalIngredients": 3,
    "highRiskIngredients": 0,
    "moderateRiskIngredients": 2,
    "lowRiskIngredients": 1,
    "overallRiskLevel": "MODERATE"
  },
  "disclaimer": "MEDICAL DISCLAIMER: ..."
}
```

**Status Codes:**
- `200 OK` - Batch analysis successful
- `400 Bad Request` - Invalid ingredient list
- `401 Unauthorized` - Invalid/expired token
- `500 Internal Server Error` - Server error

**Example:**
```bash
curl -X POST http://localhost:8080/api/allergen/analyze-batch \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '["Limonene", "Linalool", "Citral"]'
```

---

## Data Models

### ChemicalIdentification

```typescript
interface ChemicalIdentification {
  commonName: string;          // Common name (e.g., "Limonene")
  iupacName: string;           // IUPAC systematic name
  casNumber: string;           // CAS Registry Number
  pubchemCid: number;          // PubChem Compound ID
  molecularFormula: string;    // Chemical formula (e.g., "C10H16")
  smiles: string;              // SMILES notation
}
```

### SideEffect

```typescript
interface SideEffect {
  effectType: string;                // Type of reaction (e.g., "Contact Dermatitis")
  severity: "LOW" | "MODERATE" | "HIGH" | "SEVERE";
  prevalenceRate: number;            // Rate as decimal (0.05 = 5%)
  population: string;                // Affected population description
  affectedBodyAreas: string[];       // Body areas affected
  sources: Source[];                 // Research sources
}

interface Source {
  title: string;
  url: string;
  year: number;
}
```

### RiskAssessment

```typescript
interface RiskAssessment {
  riskLevel: "NONE" | "LOW" | "MODERATE" | "HIGH" | "SEVERE";
  totalReactionsFound: number;
}
```

### ProductAnalysisResponse

```typescript
interface ProductAnalysisResponse {
  productName: string;
  totalIngredients: number;
  highRiskIngredients: number;
  overallRiskLevel: "LOW" | "MODERATE" | "HIGH" | "SEVERE";
  ingredients: string[];
  detailedAnalysis: {
    [ingredientName: string]: IngredientAnalysis;
  };
  recommendations: string[];
  disclaimer: string;
}
```

### UsageStats

```typescript
interface UsageStats {
  totalTokensUsed: number;      // Cumulative OpenAI tokens
  estimatedCost: number;        // Cost in USD
  analysesRun: number;          // Total analyses performed
}
```

---

## Error Handling

All errors follow this standard format:

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid product name",
  "path": "/api/allergen/analyze-product"
}
```

### Common Error Codes

| Code | Meaning | Common Causes |
|------|---------|---------------|
| `400` | Bad Request | Invalid input, missing required fields |
| `401` | Unauthorized | Missing/invalid/expired JWT token |
| `404` | Not Found | Chemical not found in PubChem |
| `429` | Too Many Requests | Rate limit exceeded (future) |
| `500` | Internal Server Error | Database error, OpenAI API failure |
| `503` | Service Unavailable | External API (PubChem/OpenAI) unavailable |

### Error Response Examples

**Invalid Token:**
```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token has expired",
  "path": "/api/allergen/analyze/Limonene"
}
```

**Chemical Not Found:**
```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Chemical 'InvalidName' not found in PubChem database",
  "path": "/api/allergen/analyze/InvalidName"
}
```

---

## Rate Limiting & Usage Tracking

### Current Implementation

- ✅ **Token Usage Tracking:** Real-time tracking of OpenAI API consumption
- ✅ **Cost Estimation:** Automatic cost calculation based on GPT-4o pricing
- ✅ **Per-User Stats:** Individual usage metrics accessible via `/api/auth/me`

### Future Rate Limits (Planned)

| Tier | Requests/Hour | Monthly Token Limit | Cost |
|------|---------------|---------------------|------|
| Free | 100 | 100,000 tokens | $0 |
| Basic | 1,000 | 1,000,000 tokens | $9.99/mo |
| Premium | 10,000 | Unlimited | $49.99/mo |

### Cache Benefits

The three-tier caching strategy dramatically reduces costs:

| Scenario | Token Cost | USD Cost | Response Time |
|----------|-----------|----------|---------------|
| Database Hit | 0 | $0.000 | <10ms |
| Vector Hit | 0 | $0.000 | ~100ms |
| API Call (New) | ~400 | ~$0.002 | 5-10s |

**Example:** Analyzing "CeraVe Cream" (20 ingredients)
- **First time:** 4,100 tokens ($0.021)
- **Second time:** 100 tokens ($0.0005)
- **Savings:** 97.5%

---

## Code Examples

### JavaScript/TypeScript

```typescript
// API Client Class
class AllergenAPI {
  private baseUrl = 'http://localhost:8080/api';
  private accessToken: string | null = null;

  // Login
  async login(email: string, password: string) {
    const response = await fetch(`${this.baseUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    
    const data = await response.json();
    this.accessToken = data.accessToken;
    return data;
  }

  // Analyze Product
  async analyzeProduct(productName: string) {
    const response = await fetch(`${this.baseUrl}/allergen/analyze-product`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.accessToken}`
      },
      body: JSON.stringify({ productName })
    });
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    return await response.json();
  }

  // Get Usage Stats
  async getUsage() {
    const response = await fetch(`${this.baseUrl}/auth/me`, {
      headers: {
        'Authorization': `Bearer ${this.accessToken}`
      }
    });
    
    const data = await response.json();
    return data.usage;
  }
}

// Usage
const api = new AllergenAPI();

await api.login('user@example.com', 'password123');
const analysis = await api.analyzeProduct('CeraVe Cream');
console.log(`Risk Level: ${analysis.overallRiskLevel}`);

const usage = await api.getUsage();
console.log(`Tokens Used: ${usage.totalTokensUsed}`);
console.log(`Estimated Cost: $${usage.estimatedCost}`);
```

### Python

```python
import requests
from typing import Optional

class AllergenAPI:
    def __init__(self, base_url: str = "http://localhost:8080/api"):
        self.base_url = base_url
        self.access_token: Optional[str] = None
    
    def login(self, email: str, password: str):
        """Authenticate and store access token"""
        response = requests.post(
            f"{self.base_url}/auth/login",
            json={"email": email, "password": password}
        )
        response.raise_for_status()
        
        data = response.json()
        self.access_token = data["accessToken"]
        return data
    
    def analyze_product(self, product_name: str):
        """Analyze a product for allergens"""
        response = requests.post(
            f"{self.base_url}/allergen/analyze-product",
            headers={
                "Authorization": f"Bearer {self.access_token}",
                "Content-Type": "application/json"
            },
            json={"productName": product_name}
        )
        response.raise_for_status()
        return response.json()
    
    def get_usage(self):
        """Get usage statistics"""
        response = requests.get(
            f"{self.base_url}/auth/me",
            headers={"Authorization": f"Bearer {self.access_token}"}
        )
        response.raise_for_status()
        return response.json()["usage"]

# Usage Example
api = AllergenAPI()

# Login
api.login("user@example.com", "password123")

# Analyze product
analysis = api.analyze_product("CeraVe Moisturizing Cream")
print(f"Product: {analysis['productName']}")
print(f"Risk Level: {analysis['overallRiskLevel']}")
print(f"High Risk Ingredients: {analysis['highRiskIngredients']}")

# Check usage
usage = api.get_usage()
print(f"Tokens Used: {usage['totalTokensUsed']}")
print(f"Cost: ${usage['estimatedCost']:.4f}")
```

### cURL Complete Workflow

```bash
#!/bin/bash

BASE_URL="http://localhost:8080/api"

# 1. Register
echo "=== Registering User ==="
curl -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!",
    "firstName": "Test",
    "lastName": "User"
  }'

echo -e "\n"

# 2. Login
echo "=== Logging In ==="
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')
echo "Token: ${TOKEN:0:20}..."

echo -e "\n"

# 3. Analyze Product
echo "=== Analyzing Product ==="
curl -X POST "$BASE_URL/allergen/analyze-product" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productName": "Vaseline Original"}' \
  | jq '.overallRiskLevel, .totalIngredients, .highRiskIngredients'

echo -e "\n"

# 4. Check Usage Stats
echo "=== Usage Statistics ==="
curl "$BASE_URL/auth/me" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.usage'

echo -e "\n"

# 5. Analyze Same Product Again (Test Caching)
echo "=== Re-analyzing (Cache Test) ==="
curl -X POST "$BASE_URL/allergen/analyze-product" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productName": "Vaseline Original"}' \
  | jq '.overallRiskLevel'

echo -e "\n"

# 6. Check Usage Again (Should see minimal token increase)
echo "=== Usage After Cache Hit ==="
curl "$BASE_URL/auth/me" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.usage'
```

---

## Testing

### Postman Collection

Save this as `AllergenAPI.postman_collection.json`:

```json
{
  "info": {
    "name": "Allergen Intelligence Platform API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080/api"
    },
    {
      "key": "accessToken",
      "value": ""
    }
  ],
  "item": [
    {
      "name": "Authentication",
      "item": [
        {
          "name": "Register",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/auth/register",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"email\": \"test@example.com\",\n  \"password\": \"Test123!\",\n  \"firstName\": \"Test\",\n  \"lastName\": \"User\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            }
          }
        },
        {
          "name": "Login",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.collectionVariables.set('accessToken', pm.response.json().accessToken);"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/auth/login",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"email\": \"test@example.com\",\n  \"password\": \"Test123!\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            }
          }
        },
        {
          "name": "Get Current User",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/auth/me",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{accessToken}}"
              }
            ]
          }
        }
      ]
    },
    {
      "name": "Allergen Analysis",
      "item": [
        {
          "name": "Analyze Single Ingredient",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/allergen/analyze/Limonene",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{accessToken}}"
              }
            ]
          }
        },
        {
          "name": "Analyze Product",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/allergen/analyze-product",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{accessToken}}"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"productName\": \"CeraVe Moisturizing Cream\"\n}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            }
          }
        },
        {
          "name": "Batch Analysis",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/allergen/analyze-batch",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{accessToken}}"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "[\"Limonene\", \"Linalool\", \"Citral\"]",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            }
          }
        }
      ]
    }
  ]
}
```

### Test Scenarios

#### 1. Cache Effectiveness Test

```bash
# First analysis (no cache)
time curl -X POST http://localhost:8080/api/allergen/analyze-product \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productName":"Vaseline Original"}'
# Expected: ~10-15 seconds, ~4,000 tokens

# Second analysis (fully cached)
time curl -X POST http://localhost:8080/api/allergen/analyze-product \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productName":"Vaseline Original"}'
# Expected: <1 second, ~100 tokens
```

#### 2. Token Tracking Test

```bash
# Get initial usage
BEFORE=$(curl -s http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq '.usage.totalTokensUsed')

# Perform analysis
curl -s -X POST http://localhost:8080/api/allergen/analyze-product \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productName":"Test Product"}' > /dev/null

# Get new usage
AFTER=$(curl -s http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq '.usage.totalTokensUsed')

echo "Tokens used: $((AFTER - BEFORE))"
```

---

## Performance Benchmarks

### Response Time Distribution

| Endpoint | Cache Hit | Cache Miss | Avg |
|----------|-----------|------------|-----|
| `/analyze/{ingredient}` | <10ms | 5-10s | 500ms |
| `/analyze-product` | 100-500ms | 30-60s | 5s |
| `/analyze-batch` (3) | 30-100ms | 15-30s | 3s |

### Cost Analysis

**Scenario:** Analyzing 100 products with 20 ingredients each

**Without Caching:**
- Token cost: 100 × 4,100 = 410,000 tokens
- USD cost: ~$2.05

**With Caching (80% hit rate):**
- Token cost: (20 × 4,100) + (80 × 100) = 90,000 tokens
- USD cost: ~$0.45
- **Savings: 78%**

---

## Changelog

### Version 1.0.0 (Current)
- ✅ JWT authentication with token rotation
- ✅ Three-tier caching (database, vector, API)
- ✅ Real-time usage tracking
- ✅ PubChem integration
- ✅ OpenAI GPT-4o integration with web search
- ✅ Oxidation product detection
- ✅ Batch analysis support
- ✅ Product name-based analysis

### Planned (v1.1.0)
- 🔄 Rate limiting per user tier
- 🔄 Image upload and OCR for ingredient extraction
- 🔄 Elasticsearch for full-text ingredient search
- 🔄 GraphQL API alternative
- 🔄 WebSocket support for real-time updates

---

## Support & Resources

### Documentation
- **Architecture:** [docs/ARCHITECTURE.md](ARCHITECTURE.md)
- **Setup Guide:** [docs/GETTING_STARTED.md](GETTING_STARTED.md)
- **Roadmap:** [docs/ROADMAP.md](ROADMAP.md)
- **Main README:** [README.md](../README.md)

### External APIs
- **PubChem API:** https://pubchem.ncbi.nlm.nih.gov/docs/pug-rest
- **OpenAI API:** https://platform.openai.com/docs/api-reference

### Contact
- **GitHub:** https://github.com/mattbixby123/allergen-intelligence
- **Issues:** https://github.com/mattbixby123/allergen-intelligence/issues
- **Author:** Matthew Bixby

---

## Medical Disclaimer

⚠️ **THIS TOOL IS FOR EDUCATIONAL AND RESEARCH PURPOSES ONLY.**

**Do NOT use for:**
- ❌ Self-diagnosis
- ❌ Medical treatment decisions
- ❌ Replacing professional medical advice

**Always:**
- ✅ Consult qualified healthcare professionals
- ✅ Verify information with authoritative sources
- ✅ Perform proper allergy testing with medical supervision

**Limitations:**
- Information may be incomplete, outdated, or incorrect
- AI-generated content can contain errors
- Ingredient lists may not reflect current product formulations
- Allergen research is constantly evolving

**This software is provided "as is" without warranty of any kind.**

---

**Last Updated:** December 2025  
**API Version:** 1.0.0  
**Spring Boot:** 3.5.6  
**Spring AI:** 1.0.2  
**Author:** Matthew Bixby