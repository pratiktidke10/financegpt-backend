# FinanceGPT Backend

> AI-powered financial assistant backend built with Spring Boot, Gemini AI, and Alpha Vantage API.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-blue)
![Docker](https://img.shields.io/badge/Docker-Containerized-blue)
![Render](https://img.shields.io/badge/Deployed-Render-purple)

## 🔗 Live Demo

- **Backend API:** https://financegpt-backend-h10j.onrender.com
- **Frontend:** https://financegpt-frontend.vercel.app
---

## 📌 Project Overview

FinanceGPT Backend is a RESTful API built with Spring Boot that powers an AI-driven financial assistant. It uses Google Gemini AI for natural language intent recognition and Alpha Vantage API for real-time stock market data. Users can query stock prices, analyze performance, compare stocks, and manage a virtual portfolio through conversational prompts.
 
---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Core programming language |
| Spring Boot 3.2.5 | REST API framework |
| Spring Data JPA | Database ORM layer |
| Hibernate | JPA implementation |
| PostgreSQL (Supabase) | Cloud database |
| Google Gemini AI | Natural language intent recognition |
| Alpha Vantage API | Real-time stock market data |
| Docker | Containerization for deployment |
| Render | Cloud deployment platform |
| Maven | Dependency management |
 
---

## 🏗️ Architecture

```
React Frontend
      ↓ POST /api/chat
Spring Boot Backend
      ↓                    ↓
Gemini AI            Alpha Vantage API
(Intent Recognition) (Stock Market Data)
      ↓
PostgreSQL (Supabase)
(Virtual Portfolio Storage)
```

### 3-Layer Architecture

```
Controller Layer  → Receives HTTP requests, sends responses
Service Layer     → Business logic, external API calls
Repository Layer  → Database operations (JPA)
```
 
---

## ✨ Features

- 💬 **Natural Language Processing** — Gemini AI understands user queries and extracts intent
- 📈 **Real-time Stock Prices** — Current price, change, and percentage via Alpha Vantage
- 📊 **Stock Performance Analysis** — Weekly performance data for any stock
- 🔀 **Stock Comparison** — Compare multiple stocks side by side
- 🛒 **Virtual Portfolio — Buy** — Purchase shares tracked in PostgreSQL
- 💰 **Virtual Portfolio — Sell** — Sell shares with quantity validation
- 💼 **Portfolio View** — See all holdings with buy price and quantity
- 🤖 **General Financial Q&A** — Gemini answers general finance questions directly
---

## 📁 Project Structure

```
src/main/java/com/pratik/financegpt/
├── controller/
│   └── ChatController.java        # REST endpoints
├── service/
│   ├── ChatService.java           # Core logic, Gemini integration, intent routing
│   ├── StockService.java          # Alpha Vantage API calls
│   └── PortfolioService.java      # Buy/sell/view portfolio logic
├── repository/
│   └── PortfolioRepository.java   # JPA repository for portfolio
├── entity/
│   └── Portfolio.java             # Database entity (maps to portfolio table)
├── model/
│   ├── ChatRequest.java           # Incoming request DTO
│   └── ChatResponse.java          # Outgoing response DTO
├── config/
│   └── CorsConfig.java            # CORS configuration
└── FinancegptBackendApplication.java  # Entry point
```
 
---

## 🔌 API Endpoints

### POST /api/chat
Main endpoint — accepts natural language messages and returns AI-powered responses.

**Request:**
```json
{
  "message": "What is the current price of Apple?"
}
```

**Response:**
```json
{
  "response": "### 📈 AAPL Stock Price\n\n- **Current Price:** $283.78\n- **Change:** 8.63\n- **Change %:** 3.14%"
}
```

### Supported Intents

| User Query | Intent | Action |
|---|---|---|
| "Price of Apple?" | STOCK_PRICE | Fetch current price from Alpha Vantage |
| "How has Tesla performed?" | STOCK_PERFORMANCE | Fetch 4-week history |
| "Compare Apple and Google" | STOCK_COMPARISON | Fetch both stocks |
| "Buy 5 shares of Apple" | BUY_STOCK | Save to PostgreSQL portfolio |
| "Sell 2 shares of Apple" | SELL_STOCK | Update/delete from portfolio |
| "Show my portfolio" | VIEW_PORTFOLIO | Fetch all holdings from DB |
| "What is a stock market?" | GENERAL | Gemini answers directly |
 
---

## 🤖 How Intent Recognition Works

```
User: "Buy 5 shares of Apple"
            ↓
Gemini AI receives system prompt + user message
            ↓
Returns structured JSON:
{ "intent": "BUY_STOCK", "symbol": "AAPL", "quantity": 5 }
            ↓
Spring Boot routes to PortfolioService.buyStock()
            ↓
Alpha Vantage fetches current price
            ↓
Saves to PostgreSQL
            ↓
Returns: "✅ Successfully bought 5 shares of AAPL at $283.78"
```
 
---

## ⚙️ Setup & Installation

### Prerequisites
- Java 17
- Maven
- PostgreSQL database (Supabase recommended)
- Gemini API key
- Alpha Vantage API key
### Steps

1. **Clone the repository**
```bash
git clone https://github.com/pratiktidke10/financegpt-backend.git
cd financegpt-backend
```

2. **Create `application.properties`**
   Create `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://your-supabase-url:5432/postgres?sslmode=require
spring.datasource.username=your-username
spring.datasource.password=your-password
spring.datasource.driver-class-name=org.postgresql.Driver
 
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.open-in-view=false
 
gemini.api.key=your-gemini-api-key
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent
 
alphavantage.api.key=your-alphavantage-api-key
alphavantage.api.url=https://www.alphavantage.co/query
 
server.port=${PORT:8080}
```

3. **Run the application**
```bash
./mvnw spring-boot:run
```

4. **Test the API**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is the price of Apple?"}'
```
 
---

## 🔐 Environment Variables

> ⚠️ `application.properties` is excluded from GitHub via `.gitignore`. Never commit API keys!

| Variable | Description |
|---|---|
| `spring.datasource.url` | PostgreSQL connection URL |
| `spring.datasource.username` | Database username |
| `spring.datasource.password` | Database password |
| `gemini.api.key` | Google Gemini API key |
| `gemini.api.url` | Gemini API endpoint URL |
| `alphavantage.api.key` | Alpha Vantage API key |
| `alphavantage.api.url` | Alpha Vantage base URL |
 
---

## 🐳 Docker

The project includes a multi-stage Dockerfile for production deployment:

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
 
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/financegpt-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Stage 1** — Builds the JAR using JDK
**Stage 2** — Runs the JAR using lightweight JRE (smaller final image)
 
---

## 🚀 Deployment

Deployed on **Render** using Docker:

1. Push code to GitHub
2. Render auto-detects Dockerfile
3. Builds and deploys automatically
4. Environment variables configured in Render dashboard
> **Note:** Free tier on Render spins down after inactivity. First request may take ~30 seconds to wake up.
 
---

## 🗄️ Database Schema

**Table: `portfolio`**

| Column | Type | Description |
|---|---|---|
| id | BIGINT (PK) | Auto-generated primary key |
| username | VARCHAR | User identifier |
| symbol | VARCHAR | Stock ticker symbol |
| quantity | INTEGER | Number of shares owned |
| buy_price | DOUBLE | Price at time of purchase |
| created_at | TIMESTAMP | When the position was opened |

> Table is auto-created by Hibernate on first run (`ddl-auto=update`)
 
---

## 🔑 Key Concepts Used

- **Dependency Injection** — Spring manages object creation and lifecycle
- **RESTful API Design** — Clean endpoint structure with proper HTTP methods
- **3-Layer Architecture** — Controller → Service → Repository separation
- **JPA/Hibernate ORM** — Java objects mapped to database tables automatically
- **Prompt Engineering** — System prompt instructs Gemini to return structured JSON
- **Intent Recognition** — Gemini classifies user queries into predefined intents
- **DTO Pattern** — ChatRequest and ChatResponse for clean data transfer
- **Environment Variables** — Sensitive config kept out of codebase
---

## 👨‍💻 Author

**Pratik Tidke**
- GitHub: [@pratiktidke10](https://github.com/pratiktidke10)
- LinkedIn: [Pratik Tidke](https://linkedin.com/in/pratiktidke10)