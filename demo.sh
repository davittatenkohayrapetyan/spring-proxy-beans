#!/bin/bash

# Demo script for Spring Proxy Beans application
# This script demonstrates how request-scoped proxied beans provide isolation across HTTP requests

echo "=== Spring Proxy Beans Demo ==="
echo "Starting application in background..."

# Start the Spring Boot application in background
mvn spring-boot:run > app.log 2>&1 &
APP_PID=$!

# Wait for application to start
echo "Waiting for application to start..."
sleep 15

echo ""
echo "=== Testing Request Isolation ==="

echo ""
echo "1. GET /api/data (first request - should be empty):"
curl -s http://localhost:8080/api/data
echo ""

echo ""
echo "2. POST /api/data (set data in request-scoped bean):"
curl -s -X POST http://localhost:8080/api/data \
  -H "Content-Type: application/json" \
  -d '{"data":"Hello World from Request 1"}'
echo ""

echo ""
echo "3. GET /api/data (new request - should be empty again due to request isolation):"
curl -s http://localhost:8080/api/data
echo ""

echo ""
echo "4. POST and GET in the same session (different data):"
curl -s -X POST http://localhost:8080/api/data \
  -H "Content-Type: application/json" \
  -d '{"data":"Different data from Request 2"}'
echo ""

echo ""
echo "5. GET /api/data (another new request - still empty):"
curl -s http://localhost:8080/api/data
echo ""

echo ""
echo "=== Demo Complete ==="
echo "This demonstrates that:"
echo "- Each HTTP request gets its own instance of the RequestScopedDataHolder"
echo "- Data set in one request is not visible in another request"
echo "- The singleton DataService safely uses the proxied request-scoped bean"
echo "- Spring's proxy mechanism ensures thread-safety and request isolation"

echo ""
echo "Stopping application..."
kill $APP_PID
wait $APP_PID 2>/dev/null

echo "Demo finished."