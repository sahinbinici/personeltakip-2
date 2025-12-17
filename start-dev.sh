#!/bin/bash

echo "Starting Personnel Tracking System Backend..."

# Function to check if port is in use
check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null ; then
        echo "Port $1 is already in use"
        return 1
    else
        return 0
    fi
}

# Start backend
echo ""
echo "Starting Backend (Spring Boot)..."
if check_port 8080; then
    ./gradlew bootRun &
    BACKEND_PID=$!
    echo "Backend started with PID: $BACKEND_PID"
else
    echo "Backend may already be running on port 8080"
fi

echo ""
echo "Backend started!"
echo "- Backend: http://localhost:8080"
echo "- Admin Panel: http://localhost:8080/admin"
echo "- API Documentation: http://localhost:8080/swagger-ui.html"
echo "- API Endpoints: http://localhost:8080/api/*"
echo ""
echo "Note: Mobile app is in separate workspace"
echo "Connect mobile app to: http://localhost:8080/api"
echo ""
echo "Press Ctrl+C to stop backend service"

# Wait for interrupt
trap 'echo "Stopping backend..."; kill $BACKEND_PID 2>/dev/null; exit' INT
wait