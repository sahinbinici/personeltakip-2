// Admin Dashboard JavaScript

let dashboardRefreshInterval;

// Initialize dashboard
document.addEventListener('DOMContentLoaded', function() {
    loadDashboardStats();
    loadIpTrackingInfo();
    
    // Set up auto-refresh every 30 seconds
    dashboardRefreshInterval = setInterval(() => {
        loadDashboardStats();
        loadIpTrackingInfo();
    }, 30000);
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

// Load IP tracking information
async function loadIpTrackingInfo() {
    try {
        const response = await makeAuthenticatedRequest('/api/admin/dashboard/ip-tracking/info');
        
        if (!response) return;
        
        if (response.ok) {
            const info = await response.json();
            updateIpTrackingInfo(info);
        } else {
            console.error('IP tracking info loading failed');
        }
    } catch (error) {
        console.error('IP tracking info loading failed:', error);
    }
}

// Update IP tracking information in UI
function updateIpTrackingInfo(info) {
    // Update IP tracking status card
    updateIpTrackingStatusCard(info);
    
    // Update privacy status card
    updatePrivacyStatusCard(info);
    
    // Update IP tracking notice
    updateIpTrackingNotice(info);
}

// Update IP tracking status card
function updateIpTrackingStatusCard(info) {
    const statusCard = document.getElementById('ipTrackingStatusCard');
    const icon = document.getElementById('ipTrackingIcon');
    const title = document.getElementById('ipTrackingTitle');
    const description = document.getElementById('ipTrackingDescription');
    const status = document.getElementById('ipTrackingStatus');
    
    if (!statusCard || !icon || !title || !description || !status) return;
    
    const indicator = info.statusIndicator;
    
    // Update icon
    icon.className = `ip-info-icon ${indicator.colorClass}`;
    icon.innerHTML = `<i class="${indicator.iconClass}"></i>`;
    
    // Update content
    title.textContent = indicator.title;
    description.textContent = indicator.description;
    
    // Update status
    status.className = `ip-info-status ${indicator.colorClass}`;
    status.innerHTML = `<span class="status-text">${info.statusDisplay}</span>`;
}

// Update privacy status card
function updatePrivacyStatusCard(info) {
    const statusCard = document.getElementById('privacyStatusCard');
    const icon = document.getElementById('privacyIcon');
    const title = document.getElementById('privacyTitle');
    const description = document.getElementById('privacyDescription');
    const status = document.getElementById('privacyStatus');
    
    if (!statusCard || !icon || !title || !description || !status) return;
    
    const indicator = info.privacyIndicator;
    
    // Update icon
    icon.className = `ip-info-icon ${indicator.colorClass}`;
    icon.innerHTML = `<i class="${indicator.iconClass}"></i>`;
    
    // Update content
    title.textContent = indicator.title;
    description.textContent = indicator.description;
    
    // Update status
    status.className = `ip-info-status ${indicator.colorClass}`;
    status.innerHTML = `<span class="status-text">${info.privacyInfo}</span>`;
}

// Update IP tracking notice
function updateIpTrackingNotice(info) {
    const notice = document.getElementById('ipTrackingNotice');
    const noticeText = document.getElementById('ipTrackingNoticeText');
    
    if (!notice || !noticeText) return;
    
    if (info.notice && info.notice.trim()) {
        noticeText.textContent = info.notice;
        notice.style.display = 'flex';
    } else {
        notice.style.display = 'none';
    }
}

// Refresh IP tracking information manually
async function refreshIpTrackingInfo() {
    const refreshBtn = document.querySelector('.dashboard-section .refresh-btn');
    if (refreshBtn) {
        const icon = refreshBtn.querySelector('i');
        if (icon) {
            icon.classList.add('fa-spin');
        }
        refreshBtn.disabled = true;
    }
    
    await loadIpTrackingInfo();
    
    if (refreshBtn) {
        const icon = refreshBtn.querySelector('i');
        if (icon) {
            icon.classList.remove('fa-spin');
        }
        refreshBtn.disabled = false;
    }
}

// Show IP assignment help modal
function showIpAssignmentHelp() {
    loadIpTrackingInfo().then(() => {
        // Get help text from the loaded info
        makeAuthenticatedRequest('/api/admin/dashboard/ip-tracking/info')
            .then(response => response.json())
            .then(info => {
                showIpHelpModal(info.helpText);
            })
            .catch(error => {
                console.error('Failed to load help text:', error);
                showIpHelpModal('IP adresi atama yardımı yüklenemedi.');
            });
    });
}

// Show IP help modal
function showIpHelpModal(helpText) {
    const modal = document.createElement('div');
    modal.className = 'ip-help-modal';
    modal.innerHTML = `
        <div class="ip-help-modal-content">
            <div class="ip-help-modal-header">
                <h3><i class="fas fa-question-circle"></i> IP Adresi Atama Yardımı</h3>
                <button class="modal-close" onclick="closeIpHelpModal()">
                    <i class="fas fa-times"></i>
                </button>
            </div>
            <div class="ip-help-modal-body">
                <div class="ip-help-section">
                    <h4><i class="fas fa-info-circle"></i> Genel Bilgi</h4>
                    <p>${helpText}</p>
                </div>
                
                <div class="ip-help-section">
                    <h4><i class="fas fa-list"></i> Desteklenen Formatlar</h4>
                    <ul class="ip-help-list">
                        <li><i class="fas fa-check"></i> IPv4: <code>192.168.1.100</code></li>
                        <li><i class="fas fa-check"></i> IPv6: <code>2001:db8::1</code></li>
                        <li><i class="fas fa-check"></i> Birden fazla IP: <code>192.168.1.100, 10.0.0.50</code></li>
                        <li><i class="fas fa-check"></i> Noktalı virgül ile: <code>192.168.1.100; 10.0.0.50</code></li>
                    </ul>
                </div>
                
                <div class="ip-help-section">
                    <h4><i class="fas fa-lightbulb"></i> Örnekler</h4>
                    <div class="ip-help-examples">
                        <p><strong>Tek IP:</strong> <code>192.168.1.100</code></p>
                        <p><strong>Birden fazla IP:</strong> <code>192.168.1.100, 192.168.1.101, 10.0.0.50</code></p>
                        <p><strong>IPv6 ile karışık:</strong> <code>192.168.1.100, 2001:db8::1</code></p>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    // Close modal when clicking outside
    modal.addEventListener('click', function(e) {
        if (e.target === modal) {
            closeIpHelpModal();
        }
    });
}

// Close IP help modal
function closeIpHelpModal() {
    const modal = document.querySelector('.ip-help-modal');
    if (modal) {
        modal.remove();
    }
}

// Clean up on page unload
window.addEventListener('beforeunload', function() {
    if (dashboardRefreshInterval) {
        clearInterval(dashboardRefreshInterval);
    }
});