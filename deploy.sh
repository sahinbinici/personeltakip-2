#!/bin/bash

# Personnel Tracking System - Docker Deployment Script
# This script deploys the application to the production server

set -e  # Exit on any error

echo "🚀 Starting Personnel Tracking System Deployment..."

# Configuration
SERVER_IP="193.140.136.26"
APP_NAME="personeltakip"
CONTAINER_NAME="personeltakip-app"
IMAGE_NAME="personeltakip:latest"
DEPLOY_DIR="/opt/personeltakip"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if .env file exists
if [ ! -f ".env" ]; then
    print_warning ".env file not found. Creating from .env.example..."
    cp .env.example .env
    print_warning "Please update .env file with your production values before continuing."
    read -p "Press Enter to continue after updating .env file..."
fi

# Build the Docker image
print_status "Building Docker image..."
docker build -t $IMAGE_NAME .
print_success "Docker image built successfully"

# Save the image to a tar file for transfer
print_status "Saving Docker image to tar file..."
docker save $IMAGE_NAME > ${APP_NAME}.tar
print_success "Docker image saved to ${APP_NAME}.tar"

# Create deployment package
print_status "Creating deployment package..."
tar -czf ${APP_NAME}-deployment.tar.gz \
    ${APP_NAME}.tar \
    docker-compose.yml \
    .env \
    migrate_ip_tracking.ps1 \
    DEPLOYMENT_NOTES.md \
    IP_TRACKING_VALIDATION_REPORT.md

print_success "Deployment package created: ${APP_NAME}-deployment.tar.gz"

# Transfer files to server
print_status "Transferring files to server $SERVER_IP..."
scp ${APP_NAME}-deployment.tar.gz cekec@$SERVER_IP:/tmp/
print_success "Files transferred successfully"

# Deploy on server
print_status "Deploying application on server..."
ssh cekec@$SERVER_IP << EOF
    set -e
    
    echo "📦 Extracting deployment package..."
    cd /tmp
    tar -xzf ${APP_NAME}-deployment.tar.gz
    
    echo "🏗️ Setting up deployment directory..."
    sudo mkdir -p $DEPLOY_DIR || mkdir -p ~/personeltakip
    if [ -d "$DEPLOY_DIR" ]; then
        cd $DEPLOY_DIR
    else
        echo "Using home directory deployment..."
        DEPLOY_DIR=~/personeltakip
        mkdir -p $DEPLOY_DIR
        cd $DEPLOY_DIR
    fi
    
    echo "🛑 Stopping existing container (if running)..."
    docker-compose down || true
    docker stop $CONTAINER_NAME || true
    docker rm $CONTAINER_NAME || true
    
    echo "🗑️ Removing old image (if exists)..."
    docker rmi $IMAGE_NAME || true
    
    echo "📥 Loading new Docker image..."
    docker load < /tmp/${APP_NAME}.tar
    
    echo "📋 Copying configuration files..."
    cp /tmp/docker-compose.yml .
    cp /tmp/.env .
    cp /tmp/migrate_ip_tracking.ps1 .
    cp /tmp/DEPLOYMENT_NOTES.md .
    cp /tmp/IP_TRACKING_VALIDATION_REPORT.md .
    
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
    rm -f /tmp/${APP_NAME}-deployment.tar.gz
    rm -f /tmp/${APP_NAME}.tar
    rm -f /tmp/docker-compose.yml
    rm -f /tmp/.env
    rm -f /tmp/migrate_ip_tracking.ps1
    rm -f /tmp/DEPLOYMENT_NOTES.md
    rm -f /tmp/IP_TRACKING_VALIDATION_REPORT.md
    
    echo "🎉 Deployment completed successfully!"
    echo "📍 Application is available at: http://$SERVER_IP:8087"
    echo "📊 Health check: http://$SERVER_IP:8087/actuator/health"
EOF

print_success "Deployment completed successfully!"

# Clean up local files
print_status "Cleaning up local files..."
rm -f ${APP_NAME}.tar
rm -f ${APP_NAME}-deployment.tar.gz
print_success "Local cleanup completed"

echo ""
print_success "🎉 Personnel Tracking System deployed successfully!"
echo ""
print_status "📍 Application URL: http://$SERVER_IP:8087"
print_status "📊 Health Check: http://$SERVER_IP:8087/actuator/health"
echo ""
print_warning "⚠️  IMPORTANT: Don't forget to:"
print_warning "   1. Configure nginx reverse proxy for hesap.gaziantep.edu.tr/personeltakip"
print_warning "   2. Apply database migration if not done already"
print_warning "   3. Test the application functionality"
print_warning "   4. Monitor application logs: docker-compose logs -f"
echo ""