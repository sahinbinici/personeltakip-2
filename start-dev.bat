@echo off
echo Starting Personnel Tracking System Backend...

echo.
echo Starting Backend (Spring Boot)...
start "Backend" cmd /k "gradlew.bat bootRun"

echo.
echo Backend started!
echo - Backend: http://localhost:8080
echo - Admin Panel: http://localhost:8080/admin
echo - API Documentation: http://localhost:8080/swagger-ui.html
echo - API Endpoints: http://localhost:8080/api/*
echo.
echo Note: Mobile app is in separate workspace
echo Connect mobile app to: http://localhost:8080/api
echo.
pause