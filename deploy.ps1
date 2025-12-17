# Personnel Tracking System - Docker Deployment Script for Windows
# This script deploys the application to the production server from Windows

param(
    [string]$ServerIP = "193.140.136.26",
    [string]$AppName = "personeltakip",
    [string]$ContainerName = "personeltakip-app",
    [string]$ImageName = "personeltakip:latest",
    [string]$DeployDir = "/opt/personeltakip"
)

# Colors for output
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Cyan"

function Write-Status {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor $Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor $Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor $Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor $Red
}

Write-Host "🚀 Starting Personnel Tracking System Deployment..." -ForegroundColor $Green

# Check if .env file exists
if (-not (Test-Path ".env")) {
    Write-Warning ".env file not found. Creating from .env.example..."
    Copy-Item ".env.example" ".env"
    Write-Warning "Please update .env file with your production values before continuing."
    Read-Host "Press Enter to continue after updating .env file"
}

# Check if Docker is running
try {
    docker version | Out-Null
    Write-Success "Docker is running"
} catch {
    Write-Error "Docker is not running or not installed. Please start Docker Desktop."
    exit 1
}

# Build the Docker image
Write-Status "Building Docker image..."
try {
    docker build -t $ImageName .
    Write-Success "Docker image built successfully"
} catch {
    Write-Error "Failed to build Docker image"
    exit 1
}

# Save the image to a tar file for transfer
Write-Status "Saving Docker image to tar file..."
try {
    docker save $ImageName -o "$AppName.tar"
    Write-Success "Docker image saved to $AppName.tar"
} catch {
    Write-Error "Failed to save Docker image"
    exit 1
}

# Create deployment package
Write-Status "Creating deployment package..."
try {
    $files = @(
        "$AppName.tar",
        "docker-compose.yml",
        ".env",
        "migrate_ip_tracking.ps1",
        "DEPLOYMENT_NOTES.md",
        "IP_TRACKING_VALIDATION_REPORT.md",
        "DOCKER_DEPLOYMENT_GUIDE.md"
    )
    
    # Create temporary directory for packaging
    $tempDir = "temp_deployment"
    if (Test-Path $tempDir) {
        Remove-Item $tempDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $tempDir | Out-Null
    
    # Copy files to temp directory
    foreach ($file in $files) {
        if (Test-Path $file) {
            Copy-Item $file $tempDir
        }
    }
    
    # Create tar.gz package (requires tar command or 7-zip)
    if (Get-Command tar -ErrorAction SilentlyContinue) {
        tar -czf "$AppName-deployment.tar.gz" -C $tempDir .
    } else {
        # Fallback to zip if tar is not available
        Compress-Archive -Path "$tempDir\*" -DestinationPath "$AppName-deployment.zip" -Force
        $packageFile = "$AppName-deployment.zip"
    }
    
    if (-not $packageFile) {
        $packageFile = "$AppName-deployment.tar.gz"
    }
    
    Write-Success "Deployment package created: $packageFile"
} catch {
    Write-Error "Failed to create deployment package: $_"
    exit 1
}

# Transfer files to server using SCP
Write-Status "Transferring files to server $ServerIP..."
try {
    if (Get-Command scp -ErrorAction SilentlyContinue) {
        scp $packageFile "cekec@${ServerIP}:/tmp/"
        Write-Success "Files transferred successfully using SCP"
    } else {
        Write-Warning "SCP not found. Please manually transfer $packageFile to the server."
        Write-Warning "You can use WinSCP, FileZilla, or any other file transfer tool."
        Read-Host "Press Enter after transferring the file to /tmp/ on the server"
    }
} catch {
    Write-Error "Failed to transfer files: $_"
    Write-Warning "Please manually transfer $packageFile to /tmp/ on the server"
    Read-Host "Press Enter after manual transfer"
}

# Deploy on server using SSH
Write-Status "Deploying application on server..."
try {
    if (Get-Command ssh -ErrorAction SilentlyContinue) {
        $deployScript = @"
set -e

echo "📦 Extracting deployment package..."
cd /tmp
if [ -f "$AppName-deployment.tar.gz" ]; then
    tar -xzf $AppName-deployment.tar.gz
elif [ -f "$AppName-deployment.zip" ]; then
    unzip -o $AppName-deployment.zip
fi

echo "🏗️ Setting up deployment directory..."
sudo mkdir -p $DeployDir || mkdir -p ~/personeltakip
if [ -d "$DeployDir" ]; then
    cd $DeployDir
else
    echo "Using home directory deployment..."
    DEPLOY_DIR=~/personeltakip
    mkdir -p $DEPLOY_DIR
    cd $DEPLOY_DIR
fi

echo "🛑 Stopping existing container (if running)..."
docker-compose down || true
docker stop $ContainerName || true
docker rm $ContainerName || true

echo "🗑️ Removing old image (if exists)..."
docker rmi $ImageName || true

echo "📥 Loading new Docker image..."
docker load < /tmp/$AppName.tar

echo "📋 Copying configuration files..."
cp /tmp/docker-compose.yml .
cp /tmp/.env .
cp /tmp/migrate_ip_tracking.ps1 .
cp /tmp/DEPLOYMENT_NOTES.md .
cp /tmp/IP_TRACKING_VALIDATION_REPORT.md .
cp /tmp/DOCKER_DEPLOYMENT_GUIDE.md .

echo "🗂️ Creating logs directory..."
mkdir -p logs
chmod 755 logs

echo "🚀 Starting application..."
docker-compose up -d

echo "⏳ Waiting for application to start..."
sleep 30

echo "🔍 Checking application health..."
if docker-compose ps | grep -q "Up"; then
    echo "✅ Application is running successfully!"
    docker-compose ps
else
    echo "❌ Application failed to start. Checking logs..."
    docker-compose logs
    exit 1
fi

echo "🧹 Cleaning up temporary files..."
rm -f /tmp/$AppName-deployment.*
rm -f /tmp/$AppName.tar
rm -f /tmp/docker-compose.yml
rm -f /tmp/.env
rm -f /tmp/migrate_ip_tracking.ps1
rm -f /tmp/DEPLOYMENT_NOTES.md
rm -f /tmp/IP_TRACKING_VALIDATION_REPORT.md
rm -f /tmp/DOCKER_DEPLOYMENT_GUIDE.md

echo "🎉 Deployment completed successfully!"
echo "📍 Application is available at: http://$ServerIP:8087"
echo "📊 Health check: http://$ServerIP:8087/actuator/health"
"@

        # Execute deployment script on server
        $deployScript | ssh "cekec@$ServerIP" 'bash -s'
        Write-Success "Deployment completed successfully!"
    } else {
        Write-Warning "SSH not found. Please manually execute deployment on the server."
        Write-Host "Manual deployment commands:" -ForegroundColor $Yellow
        Write-Host "1. SSH to the server: ssh cekec@$ServerIP"
        Write-Host "2. Extract package: cd /tmp && tar -xzf $AppName-deployment.tar.gz"
        Write-Host "3. Setup directory: mkdir -p $DeployDir && cd $DeployDir"
        Write-Host "4. Stop existing: docker-compose down"
        Write-Host "5. Load image: docker load < /tmp/$AppName.tar"
        Write-Host "6. Copy files: cp /tmp/docker-compose.yml . && cp /tmp/.env ."
        Write-Host "7. Start app: docker-compose up -d"
        Read-Host "Press Enter after manual deployment"
    }
} catch {
    Write-Error "Failed to deploy on server: $_"
    Write-Warning "Please check server connection and try manual deployment"
}

# Clean up local files
Write-Status "Cleaning up local files..."
try {
    Remove-Item "$AppName.tar" -ErrorAction SilentlyContinue
    Remove-Item "$AppName-deployment.*" -ErrorAction SilentlyContinue
    Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    Write-Success "Local cleanup completed"
} catch {
    Write-Warning "Some cleanup files could not be removed"
}

Write-Host ""
Write-Success "🎉 Personnel Tracking System deployment process completed!"
Write-Host ""
Write-Status "📍 Application URL: http://$ServerIP:8087"
Write-Status "📊 Health Check: http://$ServerIP:8087/actuator/health"
Write-Host ""
Write-Warning "⚠️  IMPORTANT: Don't forget to:"
Write-Warning "   1. Configure nginx reverse proxy for hesap.gaziantep.edu.tr/personeltakip"
Write-Warning "   2. Apply database migration if not done already"
Write-Warning "   3. Test the application functionality"
Write-Warning "   4. Monitor application logs: docker-compose logs -f"
Write-Host ""

# Offer to open browser for health check
$openBrowser = Read-Host "Would you like to open the health check URL in browser? (y/n)"
if ($openBrowser -eq "y" -or $openBrowser -eq "Y") {
    Start-Process "http://$ServerIP:8080/actuator/health"
}