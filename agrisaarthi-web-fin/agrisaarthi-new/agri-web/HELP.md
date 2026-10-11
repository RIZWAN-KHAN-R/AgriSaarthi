# AgriSaarthi

AgriSaarthi is a Spring Boot application serving its web interface and JSON APIs from the same origin. Market-rate pages, APIs, and provider integration have been removed.

## Run locally

Requirements: Java 25.

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8080`. Set `PORT` to use a different HTTP port; the default is `8080`.

## Backend API

The browser uses relative `/api/...` URLs, so UI and APIs can be deployed together without configuring CORS.

| Method | Path | Request | Success response |
| --- | --- | --- | --- |
| `GET` | `/api/health` | None | `{"status":"UP"}` |
| `POST` | `/api/chat` | `{"message":"How should I manage irrigation?","language":"en"}` | `{"answer":"...","category":"Water management"}` |
| `POST` | `/api/crops/recommend` | `{"nitrogen":90,"phosphorus":45,"potassium":45,"ph":6.5,"temperature":28,"humidity":70,"rainfall":180,"location":"Salem"}` | `{"recommendations":[{"crop":"Rice","fit":"...","reason":"..."}],"note":"..."}` |
| `POST` | `/api/diseases/check` | `{"crop":"Tomato","symptoms":"White coating on leaves"}` | `{"crop":"Tomato","possibleCause":"...","nextSteps":["..."],"note":"..."}` |
| `GET` | `/api/weather/current?location=Salem` | None | Current conditions, advisory, source, and observation time |

JSON validation errors use HTTP 400 and an `error` field. Not-found locations use HTTP 404; weather provider failures use HTTP 502. The chat endpoint accepts `en` and `ta`.

The health endpoint reports application-process status; it does not check the weather provider. Weather lookups call Open-Meteo from the server and require outbound network access. Crop and chat guidance are rule-based. Plant image preview does not run a model; `/api/diseases/check` provides text symptom triage.

## Build and deploy

Build and test the executable JAR:

```powershell
.\mvnw.cmd -B test package
```

Run it with the port supplied by a hosting platform:

```powershell
$env:PORT = "8080"
java -jar target\agri-web-0.0.1-SNAPSHOT.jar
```

The included multi-stage Dockerfile builds a non-root runtime image:

```powershell
docker build -t agrisaarthi .
docker run --rm -p 8080:8080 -e PORT=8080 agrisaarthi
```

Check readiness at `http://localhost:8080/api/health`.

## Integration notes

- UI and API are served by the same Spring Boot process, so no external backend URL or CORS configuration is needed for the included APIs.
- Activity and the language preference are stored in each user's browser; no database is currently configured.
- For a production system that needs persistent data or trained recommendations, connect a database and replace the current rule-based services with the chosen backend integrations.
