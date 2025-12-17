@echo off
echo ========================================
echo Personnel Tracking System Deployment
echo ========================================
echo.

REM Check if .env exists
if not exist ".env" (
    echo WARNING: .env file not found. Creating from template...
    copy ".env.example" ".env"
    echo Please update .env file with production values and run this script again.
    pause
    exit /b 1
)

echo Step 1: Building Docker image...
docker build -t personeltakip:latest .
if %errorlevel% neq 0 (
    echo ERROR: Failed to build Docker image
    pause
    exit /b 1
)

echo Step 2: Saving Docker image...
docker save personeltakip:latest -o personeltakip.tar
if %errorlevel% neq 0 (
    echo ERROR: Failed to save Docker image
    pause
    exit /b 1
)

echo Step 3: Files ready for deployment
echo.
echo Manual deployment steps:
echo 1. Transfer these files to server 193.140.136.26:/opt/personeltakip/
echo    - personeltakip.tar
echo    - docker-compose.yml
echo    - .env
echo    - migrate_ip_tracking.ps1
echo.
echo 2. On the server, run:
echo    cd /opt/personeltakip
echo    docker load ^< personeltakip.tar
echo    docker-compose down
echo    docker-compose up -d
echo.
echo 3. Check health: http://193.140.136.26:8080/actuator/health
echo.

REM Try to use SCP if available
where scp >nul 2>nul
if %errorlevel% equ 0 (
    echo SCP found. Attempting to transfer files...
    scp personeltakip.tar docker-compose.yml .env migrate_ip_tracking.ps1 root@193.140.136.26:/opt/personeltakip/
    if %errorlevel% equ 0 (
        echo Files transferred successfully!
        echo Now SSH to the server and run the deployment commands above.
    ) else (
        echo SCP transfer failed. Please transfer files manually.
    )
) else (
    echo SCP not found. Please transfer files manually using WinSCP or similar tool.
)

echo.
echo Deployment package ready!
pause