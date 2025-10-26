# Mock API Server - End-to-End Flow

## Flow Overview

1. **POST** to `/mock/record/api/clients` - Records a stub in WireMock
2. **WireMock** saves the stub mapping
3. **Your service** calls `external.mymsd.base-url/api/clients` and gets the stubbed response

## Running the Application

```bash
mvn spring-boot:run
```

## Testing the Flow

1. **Record a stub:**
```bash
curl -X POST http://localhost:8080/mock/record/api/clients \
  -H "Content-Type: application/json" \
  -d '[{"id":1,"name":"John Doe","email":"john@example.com"}]'
```

2. **Test the stubbed response:**
```bash
curl http://localhost:8080/mock/test/clients
```

## Architecture

- **Port 8080**: Your Spring Boot application
- **Port 8089**: Embedded WireMock server (configured as external.mymsd.base-url)
- **./wiremock**: Directory where WireMock mappings and response files are stored
- **ClientService**: Makes calls to the external API
- **MockController**: Manages stub recording and testing
- **RequestLoggingFilter**: Logs incoming requests with basic network info