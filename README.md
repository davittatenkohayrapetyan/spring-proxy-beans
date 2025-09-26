# spring-proxy-beans

Demo Spring Boot web app showing how a singleton service interacts with a proxied @RequestScope bean. Includes endpoints to set and retrieve request-specific data, illustrating how Spring's IoC container manages scope proxies safely across concurrent requests.

## Features

- **Singleton Service**: `DataService` is a single instance that serves all requests
- **Request-Scoped Bean**: `RequestScopedDataHolder` with `@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)`
- **Proxy Mode**: Spring creates a proxy that routes method calls to the correct request-specific instance
- **Request Isolation**: Data set in one HTTP request is not visible in another HTTP request
- **Thread Safety**: The proxy ensures thread-safe access to request-specific data

## API Endpoints

- `POST /api/data` - Set data for the current request
  - Body: `{"data": "your-data-here"}`
  - Returns: Confirmation message
- `GET /api/data` - Get data from the current request
  - Returns: Data info with timestamp if set, or "No data set" message

## Running the Application

```bash
# Build and run
mvn spring-boot:run

# In another terminal, test the endpoints
curl -X GET http://localhost:8080/api/data
curl -X POST http://localhost:8080/api/data -H "Content-Type: application/json" -d '{"data":"test"}'
curl -X GET http://localhost:8080/api/data  # Will be empty (new request)
```

## Demo Script

Run the included demo script to see request isolation in action:

```bash
./demo.sh
```

## Testing

```bash
# Run unit and integration tests
mvn test
```

## How It Works

1. **Spring Proxy Creation**: When `DataService` is created, Spring injects a proxy instance of `RequestScopedDataHolder`
2. **Request Handling**: Each HTTP request creates a new instance of `RequestScopedDataHolder`
3. **Proxy Routing**: The proxy routes method calls to the correct request-specific instance
4. **Automatic Cleanup**: Request-scoped instances are automatically cleaned up after request completion
5. **Isolation**: Data from one request never leaks to another request, ensuring proper isolation

This pattern is useful for:
- Request-specific caching
- User session data
- Request correlation IDs
- Any data that should be isolated per HTTP request