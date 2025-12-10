// Admin Dashboard JavaScript

let dashboardRefreshInterval;

// Initialize dashboard
document.addEventListener('DOMContentLoaded', function() {
    loadDashboardStats();
    
    // Set up auto-refresh every 30 seconds
    dashboardRefreshInterval = setInterval(loadDashboardStats, 30000);
});

// Load dashboard statistics
async function loadDashboardStats() {
    try {
        const response = await makeAuthenticatedRequest('/api/admin/dashboard/stats');
        
        if (!response) return;
        
        if (response.ok) {
            const stats = await response.json();
            updateDashboardStats(stats);
        } else {
            const error = await response.json();
            showError(error.message || 'İstatistikler yüklenirken hata oluştu');
        }
    } catch (error) {
        console.error('Dashboard stats loading failed:', error);
        showError('İstatistikler yüklenirken hata oluştu');
    }
}

// Update dashboard statistics in UI
function updateDashboardStats(stats) {
    // Update stat cards
    updateElement('totalUsers', stats.totalUsers || 0);
    updateElement('todayEntries', stats.todayEntryCount || 0);
    updateElement('todayExits', stats.todayExitCount || 0);
    updateElement('todayTotal', stats.todayTotalActivity || 0);
    
    // Update last updated time
    updateElement('lastUpdated', formatTime(stats.lastUpdated));
    
    // Update recent records
    updateRecentRecords(stats.recentRecords || []);
    
    // Update recent audit logs
    updateRecentAuditLogs(stats.recentAuditLogs || []);
}

// Update element content safely
function updateElement(id, content) {
    const element = document.getElementById(id);
    if (element) {
        element.textContent = content;
    }
}

// Update recent entry/exit records
function updateRecentRecords(records) {
    const container = document.getElementById('recentRecords');
    if (!container) return;
    
    if (records.length === 0) {
        container.innerHTML = '<div class="loading">Son 24 saatte aktivite bulunamadı</div>';
        return;
    }
    
    const recordsHtml = records.map(record => {
        const isEntry = record.type === 'ENTRY';
        const iconClass = isEntry ? 'entry' : 'exit';
        const iconName = isEntry ? 'fa-sign-in-alt' : 'fa-sign-out-alt';
        const actionText = isEntry ? 'Giriş' : 'Çıkış';
        
        return `
            <div class="activity-item">
                <div class="activity-icon ${iconClass}">
                    <i class="fas ${iconName}"></i>
                </div>
                <div class="activity-content">
                    <div class="activity-title">${actionText} - ${record.user?.firstName || 'Bilinmeyen'} ${record.user?.lastName || 'Kullanıcı'}</div>
                    <div class="activity-subtitle">QR: ${record.qrCodeValue}</div>
                </div>
                <div class="activity-time">${getRelativeTime(record.timestamp)}</div>
            </div>
        `;
    }).join('');
    
    container.innerHTML = recordsHtml;
}

// Update recent audit logs
function updateRecentAuditLogs(auditLogs) {
    const container = document.getElementById('recentAuditLogs');
    if (!container) return;
    
    if (auditLogs.length === 0) {
        container.innerHTML = '<div class="loading">Son 24 saatte admin işlemi bulunamadı</div>';
        return;
    }
    
    const logsHtml = auditLogs.map(log => {
        const actionText = getActionText(log.action);
        
        return `
            <div class="activity-item">
                <div class="activity-icon admin">
                    <i class="fas fa-cog"></i>
                </div>
                <div class="activity-content">
                    <div class="activity-title">${actionText}</div>
                    <div class="activity-subtitle">Admin ID: ${log.adminUserId}</div>
                </div>
                <div class="activity-time">${getRelativeTime(log.timestamp)}</div>
            </div>
        `;
    }).join('');
    
    container.innerHTML = logsHtml;
}

// Get human-readable action text
function getActionText(action) {
    const actionMap = {
        'ROLE_CHANGE': 'Rol Değişikliği',
        'USER_UPDATE': 'Kullanıcı Güncelleme',
        'RECORD_EXPORT': 'Kayıt Dışa Aktarma',
        'USER_DELETE': 'Kullanıcı Silme',
        'SYSTEM_CONFIG': 'Sistem Yapılandırması'
    };
    
    return actionMap[action] || action;
}

// Refresh dashboard manually
async function refreshDashboard() {
    const refreshBtn = document.querySelector('.refresh-btn');
    if (refreshBtn) {
        const icon = refreshBtn.querySelector('i');
        if (icon) {
            icon.classList.add('fa-spin');
        }
        refreshBtn.disabled = true;
    }
    
    await loadDashboardStats();
    
    if (refreshBtn) {
        const icon = refreshBtn.querySelector('i');
        if (icon) {
            icon.classList.remove('fa-spin');
        }
        refreshBtn.disabled = false;
    }
}

// Clean up on page unload
window.addEventListener('beforeunload', function() {
    if (dashboardRefreshInterval) {
        clearInterval(dashboardRefreshInterval);
    }
});