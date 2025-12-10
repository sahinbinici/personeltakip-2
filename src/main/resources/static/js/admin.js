// Admin Panel Common JavaScript Functions

// Authentication and authorization utilities
function checkAdminAuth() {
    const token = localStorage.getItem('authToken');
    const user = localStorage.getItem('user');
    
    if (!token || !user) {
        redirectToLogin();
        return false;
    }
    
    try {
        const userData = JSON.parse(user);
        if (userData.role !== 'ADMIN' && userData.role !== 'SUPER_ADMIN') {
            // User is not admin, redirect to QR code page
            window.location.href = '/qrcode';
            return false;
        }
        
        // Update user info in header
        const adminUserName = document.getElementById('adminUserName');
        if (adminUserName) {
            adminUserName.textContent = `${userData.firstName} ${userData.lastName}`;
        }
        
        return true;
    } catch (e) {
        redirectToLogin();
        return false;
    }
}

function redirectToLogin() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('tokenType');
    localStorage.removeItem('user');
    document.cookie = 'jwt=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
    window.location.href = '/login';
}

function logout() {
    if (confirm('Çıkış yapmak istediğinizden emin misiniz?')) {
        redirectToLogin();
    }
}

// API request utilities
function getAuthHeaders() {
    const token = localStorage.getItem('authToken');
    const tokenType = localStorage.getItem('tokenType') || 'Bearer';
    
    return {
        'Authorization': `${tokenType} ${token}`,
        'Content-Type': 'application/json'
    };
}

async function makeAuthenticatedRequest(url, options = {}) {
    const headers = {
        ...getAuthHeaders(),
        ...options.headers
    };
    
    try {
        const response = await fetch(url, {
            ...options,
            headers
        });
        
        if (response.status === 401 || response.status === 403) {
            // Token expired or insufficient permissions
            redirectToLogin();
            return null;
        }
        
        return response;
    } catch (error) {
        console.error('API request failed:', error);
        showError('Bir hata oluştu. Lütfen tekrar deneyin.');
        return null;
    }
}

// UI utilities
function showError(message) {
    const errorAlert = document.getElementById('errorAlert');
    const errorMessage = document.getElementById('errorMessage');
    
    if (errorAlert && errorMessage) {
        errorMessage.textContent = message;
        errorAlert.style.display = 'flex';
        
        // Auto-hide after 5 seconds
        setTimeout(() => {
            hideError();
        }, 5000);
    }
}

function hideError() {
    const errorAlert = document.getElementById('errorAlert');
    if (errorAlert) {
        errorAlert.style.display = 'none';
    }
}

function showLoading() {
    const loadingOverlay = document.getElementById('loadingOverlay');
    if (loadingOverlay) {
        loadingOverlay.style.display = 'flex';
    }
}

function hideLoading() {
    const loadingOverlay = document.getElementById('loadingOverlay');
    if (loadingOverlay) {
        loadingOverlay.style.display = 'none';
    }
}

// Date and time formatting utilities
function formatDateTime(dateTimeString) {
    if (!dateTimeString) return '--';
    
    const date = new Date(dateTimeString);
    return date.toLocaleString('tr-TR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatDate(dateString) {
    if (!dateString) return '--';
    
    const date = new Date(dateString);
    return date.toLocaleDateString('tr-TR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    });
}

function formatTime(dateTimeString) {
    if (!dateTimeString) return '--';
    
    const date = new Date(dateTimeString);
    return date.toLocaleTimeString('tr-TR', {
        hour: '2-digit',
        minute: '2-digit'
    });
}

function getRelativeTime(dateTimeString) {
    if (!dateTimeString) return '--';
    
    const date = new Date(dateTimeString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);
    
    if (diffMins < 1) return 'Az önce';
    if (diffMins < 60) return `${diffMins} dakika önce`;
    if (diffHours < 24) return `${diffHours} saat önce`;
    if (diffDays < 7) return `${diffDays} gün önce`;
    
    return formatDate(dateTimeString);
}

// Mobile sidebar toggle
function toggleSidebar() {
    const sidebar = document.querySelector('.admin-sidebar');
    if (sidebar) {
        sidebar.classList.toggle('open');
    }
}

// Initialize admin panel
document.addEventListener('DOMContentLoaded', function() {
    // Check authentication on page load
    if (!checkAdminAuth()) {
        return;
    }
    
    // Add mobile menu button if needed
    if (window.innerWidth <= 768) {
        addMobileMenuButton();
    }
    
    // Handle window resize
    window.addEventListener('resize', function() {
        if (window.innerWidth <= 768) {
            addMobileMenuButton();
        } else {
            removeMobileMenuButton();
        }
    });
});

function addMobileMenuButton() {
    const header = document.querySelector('.admin-header .header-left');
    if (header && !document.getElementById('mobileMenuBtn')) {
        const menuBtn = document.createElement('button');
        menuBtn.id = 'mobileMenuBtn';
        menuBtn.className = 'mobile-menu-btn';
        menuBtn.innerHTML = '<i class="fas fa-bars"></i>';
        menuBtn.onclick = toggleSidebar;
        header.insertBefore(menuBtn, header.firstChild);
    }
}

function removeMobileMenuButton() {
    const menuBtn = document.getElementById('mobileMenuBtn');
    if (menuBtn) {
        menuBtn.remove();
    }
}