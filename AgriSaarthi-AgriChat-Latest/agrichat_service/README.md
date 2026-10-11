# AgriSaarthi AGRI-CHAT-SERVICE

Spring Boot 3.5 / Java 21 backend for multilingual agricultural chat. It includes:
- `POST /api/chat` (accepts `message` or `query`)
- Tamil Unicode detection and lightweight agricultural topic classification
- REST retrieval from `AGRI-KNOWLEDGE-SERVICE`
- Optional Gemini API generation with retrieved evidence and Google Search grounding
- SQL interaction storage (H2 file database by default; MySQL profile available)
- `GET /api/chat/history?conversationId=...`
- `GET /actuator/health`
- Swagger UI at `/swagger-ui.html`

## Requirements
- Java 21
- Maven 3.9+ recommended
- Internet access for the first Maven dependency download
- Knowledge service is optional during local startup but required for retrieved evidence

## Run locally
```bash
mvn clean test
mvn spring-boot:run
```
Default port is `8081`. H2 stores data under `./data/agrichat`.

## Smoke test
```bash
curl -i http://localhost:8081/actuator/health
curl -s http://localhost:8081/api/info
curl -s -X POST http://localhost:8081/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"How can I manage pests in tomato?","language":"en","crop":"tomato"}'
```
This strict integration version does not return canned agricultural answers when live integrations fail: it returns HTTP 503. Start the teammate's knowledge service and configure `GEMINI_API_KEY` before testing a successful end-to-end response. Gemini Google Search grounding can return current web references in addition to the sources returned by the knowledge service.

Tamil test:
```bash
curl -s -X POST http://localhost:8081/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"தக்காளி பயிரில் பூச்சியை எப்படி கட்டுப்படுத்துவது?","crop":"தக்காளி"}'
```

## Knowledge-service contract for your teammate
Base URL is `KNOWLEDGE_SERVICE_URL`, default `http://localhost:8082`.
This service sends:
```json
{
  "query": "How can I manage pests in tomato?",
  "language": "en",
  "latitude": null,
  "longitude": null,
  "crop": "tomato",
  "category": "pest_disease"
}
```
to `POST /api/knowledge/retrieve`.

Expected response:
```json
{
  "evidence": [
    {
      "text": "Evidence text from a trusted agricultural source.",
      "title": "Source title",
      "url": "https://example.org/article"
    }
  ],
  "weather": {},
  "sources": [
    {
      "title": "Source title",
      "url": "https://example.org/article",
      "type": "extension_publication"
    }
  ],
  "retrievalStatus": "SUCCESS"
}
```
Use real source URLs from the knowledge service, not the illustrative URL above. If your teammate's field names differ, align both services' DTOs or add Jackson aliases.

## Frontend contract
`POST http://localhost:8081/api/chat`
```json
{
  "message": "How should I grow groundnut?",
  "conversationId": "optional-stable-id-from-frontend",
  "language": "en",
  "crop": "groundnut",
  "latitude": 12.9,
  "longitude": 79.1
}
```
`message` may be replaced by `query`. `conversationId` can be omitted for a new conversation; the API returns one. Keep reusing it for subsequent turns.

## Gemini setup
Set an environment variable before starting:
```bash
export GEMINI_API_KEY='your-key'
mvn spring-boot:run
```
Never commit API keys to Git. Without the key, `/api/chat` returns HTTP 503; this is intentional so the system cannot be mistaken for a live AI implementation.

## Use MySQL instead of H2
Install/start MySQL and create a database/user, then run:
```bash
export SPRING_PROFILES_ACTIVE=mysql
export DB_URL='jdbc:mysql://localhost:3306/agrisaarthi?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USERNAME='agriuser'
export DB_PASSWORD='your-local-password'
mvn spring-boot:run
```
The app uses Hibernate `ddl-auto=update` for hackathon development. For production, use schema migrations and proper authentication/authorization before exposing history endpoints.

## Ports
- Frontend: 3000 (configurable)
- AGRI-CHAT-SERVICE: 8081
- AGRI-KNOWLEDGE-SERVICE: 8082
- SQL: H2 by default, or MySQL 3306

## Important integration notes
- The classifier and language detection are intentionally lightweight baselines; improve them with a proper language-identification model and validated taxonomy as the project grows.
- The answer generator is instructed not to invent sources. Verify retrieved evidence and citations in end-to-end testing.
- The history endpoint is suitable for a demo; add user authentication and ownership checks before handling real farmer data.
