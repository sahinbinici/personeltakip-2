// Admin Users Management JavaScript

let currentPage = 0;
let currentSize = 20;
let currentSearchTerm = '';
let currentRoleFilter = '';
let currentDepartmentFilter = '';
let totalPages = 0;
let selectedUserId = null;
let allDepartments = [];

// Initialize users page
document.addEventListener('DOMContentLoaded', function() {
    loadUserStats();
    loadDepartments();
    loadUsers();
    setupEventListeners();
});

// Setup event listeners
function setupEventListeners() {
    // Search functionality
    const searchInput = document.getElementById('searchInput');
    const clearSearch = document.getElementById('clearSearch');
    
    searchInput.addEventListener('input', debounce(handleSearch, 300));
    clearSearch.addEventListener('click', clearSearchInput);
    
    // Role filter
    const roleFilter = document.getElementById('roleFilter');
    roleFilter.addEventListener('change', handleRoleFilter);
    
    // Department filter
    const departmentFilter = document.getElementById('departmentFilter');
    departmentFilter.addEventListener('change', handleDepartmentFilter);
    
    // Refresh button
    const refreshBtn = document.getElementById('refreshBtn');
    refreshBtn.addEventListener('click', refreshUsers);
    
    // Pagination
    const prevPageBtn = document.getElementById('prevPageBtn');
    const nextPageBtn = document.getElementById('nextPageBtn');
    
    prevPageBtn.addEventListener('click', () => changePage(currentPage - 1));
    nextPageBtn.addEventListener('click', () => changePage(currentPage + 1));
}

// Load user statistics
async function loadUserStats() {
    try {
        const response = await makeAuthenticatedRequest('/api/admin/users/stats');
        
        if (!response) return;
        
        if (response.ok) {
            const stats = await response.json();
            updateUserStats(stats);
        } else {
            console.error('Failed to load user stats');
        }
    } catch (error) {
        console.error('Error loading user stats:', error);
    }
}

// Update user statistics in UI
function updateUserStats(stats) {
    document.getElementById('totalUsersCount').textContent = stats.totalUsers || 0;
    document.getElementById('normalUsersCount').textContent = stats.normalUsers || 0;
    document.getElementById('adminUsersCount').textContent = stats.adminUsers || 0;
    document.getElementById('superAdminUsersCount').textContent = stats.superAdminUsers || 0;
}

// Load users with current filters
async function loadUsers() {
    showLoading();
    
    try {
        let url = '/api/admin/users';
        const params = new URLSearchParams({
            page: currentPage,
            size: currentSize
        });
        
        // Add search or filter parameters
        if (currentSearchTerm) {
            url = '/api/admin/users/search';
            params.append('searchTerm', currentSearchTerm);
        } else if (currentRoleFilter || currentDepartmentFilter) {
            url = '/api/admin/users/filter';
            if (currentRoleFilter) {
                params.append('role', currentRoleFilter);
            }
            if (currentDepartmentFilter) {
                params.append('departmentCode', currentDepartmentFilter);
            }
        }
        
        const response = await makeAuthenticatedRequest(`${url}?${params}`);
        
        if (!response) return;
        
        if (response.ok) {
            const data = await response.json();
            displayUsers(data);
            updatePagination(data);
        } else {
            showError('Kullanıcılar yüklenirken hata oluştu');
        }
    } catch (error) {
        console.error('Error loading users:', error);
        showError('Kullanıcılar yüklenirken hata oluştu');
    } finally {
        hideLoading();
    }
}

// Display users in table
function displayUsers(data) {
    const tbody = document.getElementById('usersTableBody');
    const tableInfo = document.getElementById('tableInfo');
    
    if (!data.content || data.content.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" class="no-data">
                    <div class="no-data-message">
                        <i class="fas fa-users"></i>
                        <p>Kullanıcı bulunamadı</p>
                    </div>
                </td>
            </tr>
        `;
        tableInfo.textContent = 'Kullanıcı bulunamadı';
        return;
    }
    
    const usersHtml = data.content.map(user => {
        const lastLogin = user.lastLoginAt ? formatDateTime(user.lastLoginAt) : 'Hiç giriş yapmamış';
        const createdAt = formatDate(user.createdAt);
        const fullName = user.fullName || 'Bilinmeyen';
        const roleDisplayName = getRoleDisplayName(user.role);
        
        const departmentName = user.departmentName || user.departmentCode || '--';
        
        const assignedIps = user.assignedIpAddresses || '';
        const ipDisplay = assignedIps ? 
            `<span class="ip-assigned" title="${assignedIps}">${formatIpAddresses(assignedIps)}</span>` : 
            '<span class="ip-not-assigned">Atanmamış</span>';
        
        return `
            <tr>
                <td>${user.tcNo}</td>
                <td>${fullName}</td>
                <td>${user.personnelNo}</td>
                <td>${departmentName}</td>
                <td>${user.mobilePhone}</td>
                <td class="ip-address-cell">${ipDisplay}</td>
                <td>
                    <span class="role-badge role-${user.role.toLowerCase()}">${roleDisplayName}</span>
                </td>
                <td>${createdAt}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-sm btn-info" onclick="showUserDetail(${user.id})" title="Detayları Görüntüle">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn btn-sm btn-primary" onclick="showIpAssignmentModal(${user.id}, '${user.fullName}', '${assignedIps}')" title="IP Adresi Yönet">
                            <i class="fas fa-network-wired"></i>
                        </button>
                        <button class="btn btn-sm btn-warning" onclick="showRoleChangeModal(${user.id}, '${user.fullName}', '${user.role}')" title="Rol Değiştir">
                            <i class="fas fa-user-cog"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
    
    tbody.innerHTML = usersHtml;
    
    // Update table info
    const start = data.number * data.size + 1;
    const end = Math.min((data.number + 1) * data.size, data.totalElements);
    tableInfo.textContent = `${start}-${end} / ${data.totalElements} kullanıcı`;
}

// Update pagination controls
function updatePagination(data) {
    totalPages = data.totalPages;
    currentPage = data.number;
    
    const prevBtn = document.getElementById('prevPageBtn');
    const nextBtn = document.getElementById('nextPageBtn');
    const pageNumbers = document.getElementById('pageNumbers');
    const paginationInfo = document.getElementById('paginationInfo');
    
    // Update buttons
    prevBtn.disabled = currentPage === 0;
    nextBtn.disabled = currentPage >= totalPages - 1;
    
    // Update page numbers
    pageNumbers.innerHTML = generatePageNumbers();
    
    // Update pagination info
    paginationInfo.textContent = `Sayfa ${currentPage + 1} / ${totalPages}`;
}

// Generate page numbers HTML
function generatePageNumbers() {
    if (totalPages <= 1) return '';
    
    let html = '';
    const maxVisible = 5;
    let start = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible);
    
    if (end - start < maxVisible) {
        start = Math.max(0, end - maxVisible);
    }
    
    for (let i = start; i < end; i++) {
        const isActive = i === currentPage;
        html += `
            <button class="page-number ${isActive ? 'active' : ''}" onclick="changePage(${i})">
                ${i + 1}
            </button>
        `;
    }
    
    return html;
}

// Change page
function changePage(page) {
    if (page < 0 || page >= totalPages || page === currentPage) return;
    
    currentPage = page;
    loadUsers();
}

// Handle search input
function handleSearch() {
    const searchInput = document.getElementById('searchInput');
    const clearSearch = document.getElementById('clearSearch');
    
    currentSearchTerm = searchInput.value.trim();
    currentRoleFilter = ''; // Clear role filter when searching
    currentPage = 0;
    
    // Update UI
    document.getElementById('roleFilter').value = '';
    clearSearch.style.display = currentSearchTerm ? 'block' : 'none';
    
    loadUsers();
}

// Clear search input
function clearSearchInput() {
    const searchInput = document.getElementById('searchInput');
    const clearSearch = document.getElementById('clearSearch');
    const roleFilter = document.getElementById('roleFilter');
    const departmentFilter = document.getElementById('departmentFilter');
    
    searchInput.value = '';
    roleFilter.value = '';
    departmentFilter.value = '';
    
    currentSearchTerm = '';
    currentRoleFilter = '';
    currentDepartmentFilter = '';
    currentPage = 0;
    clearSearch.style.display = 'none';
    
    loadUsers();
}

// Handle role filter
function handleRoleFilter() {
    const roleFilter = document.getElementById('roleFilter');
    
    currentRoleFilter = roleFilter.value;
    currentSearchTerm = ''; // Clear search when filtering
    currentPage = 0;
    
    // Update UI
    document.getElementById('searchInput').value = '';
    document.getElementById('clearSearch').style.display = 'none';
    
    loadUsers();
}

// Handle department filter
function handleDepartmentFilter() {
    const departmentFilter = document.getElementById('departmentFilter');
    
    currentDepartmentFilter = departmentFilter.value;
    currentSearchTerm = ''; // Clear search when filtering
    currentPage = 0;
    
    // Update UI
    document.getElementById('searchInput').value = '';
    document.getElementById('clearSearch').style.display = 'none';
    
    loadUsers();
}

// Refresh users
function refreshUsers() {
    const refreshBtn = document.getElementById('refreshBtn');
    const icon = refreshBtn.querySelector('i');
    
    icon.classList.add('fa-spin');
    refreshBtn.disabled = true;
    
    Promise.all([loadUserStats(), loadDepartments(), loadUsers()]).finally(() => {
        icon.classList.remove('fa-spin');
        refreshBtn.disabled = false;
    });
}

// Load departments for filter dropdown
async function loadDepartments() {
    try {
        const response = await makeAuthenticatedRequest('/api/admin/records/departments');
        
        if (!response) return;
        
        if (response.ok) {
            const data = await response.json();
            allDepartments = data || [];
            populateDepartmentSelect();
        } else {
            console.error('Failed to load departments');
        }
    } catch (error) {
        console.error('Error loading departments:', error);
    }
}

// Populate department select dropdown
function populateDepartmentSelect() {
    const departmentSelect = document.getElementById('departmentFilter');
    
    // Clear existing options except the first one
    while (departmentSelect.children.length > 1) {
        departmentSelect.removeChild(departmentSelect.lastChild);
    }
    
    // Add department options
    allDepartments.forEach(department => {
        const option = document.createElement('option');
        option.value = department.code;
        option.textContent = department.name;
        departmentSelect.appendChild(option);
    });
}

// Show user detail modal
async function showUserDetail(userId) {
    try {
        showLoading();
        
        const response = await makeAuthenticatedRequest(`/api/admin/users/${userId}`);
        
        if (!response) return;
        
        if (response.ok) {
            const user = await response.json();
            displayUserDetail(user);
        } else {
            showError('Kullanıcı detayları yüklenirken hata oluştu');
        }
    } catch (error) {
        console.error('Error loading user detail:', error);
        showError('Kullanıcı detayları yüklenirken hata oluştu');
    } finally {
        hideLoading();
    }
}

// Display user detail in modal
function displayUserDetail(user) {
    const content = document.getElementById('userDetailContent');
    const lastLogin = user.lastLoginAt ? formatDateTime(user.lastLoginAt) : 'Hiç giriş yapmamış';
    const createdAt = formatDateTime(user.createdAt);
    const updatedAt = formatDateTime(user.updatedAt);
    const fullName = user.fullName || 'Bilinmeyen';
    const roleDisplayName = getRoleDisplayName(user.role);
    
    const assignedIpsDisplay = user.assignedIpAddresses ? 
        `<span class="ip-assigned">${user.assignedIpAddresses}</span>` : 
        '<span class="ip-not-assigned">Atanmamış</span>';
    
    content.innerHTML = `
        <div class="user-detail-grid">
            <div class="detail-row">
                <label>TC Kimlik No:</label>
                <span>${user.tcNo}</span>
            </div>
            <div class="detail-row">
                <label>Ad Soyad:</label>
                <span>${fullName}</span>
            </div>
            <div class="detail-row">
                <label>Sicil No:</label>
                <span>${user.personnelNo}</span>
            </div>
            <div class="detail-row">
                <label>Telefon:</label>
                <span>${user.mobilePhone}</span>
            </div>
            <div class="detail-row">
                <label>Departman:</label>
                <span>${user.departmentName || user.departmentCode || 'Belirtilmemiş'}</span>
            </div>
            <div class="detail-row">
                <label>Departman Kodu:</label>
                <span>${user.departmentCode || 'Belirtilmemiş'}</span>
            </div>
            <div class="detail-row">
                <label>Ünvan Kodu:</label>
                <span>${user.titleCode || 'Belirtilmemiş'}</span>
            </div>
            <div class="detail-row">
                <label>Atanmış IP Adresleri:</label>
                ${assignedIpsDisplay}
            </div>
            <div class="detail-row">
                <label>Rol:</label>
                <span class="role-badge role-${user.role.toLowerCase()}">${roleDisplayName}</span>
            </div>
            <div class="detail-row">
                <label>Son Giriş:</label>
                <span>${lastLogin}</span>
            </div>
            <div class="detail-row">
                <label>Kayıt Tarihi:</label>
                <span>${createdAt}</span>
            </div>
            <div class="detail-row">
                <label>Son Güncelleme:</label>
                <span>${updatedAt}</span>
            </div>
        </div>
    `;
    
    document.getElementById('userDetailModal').style.display = 'flex';
}

// Close user detail modal
function closeUserDetailModal() {
    document.getElementById('userDetailModal').style.display = 'none';
}

// Show role change modal
function showRoleChangeModal(userId, userName, currentRole) {
    selectedUserId = userId;
    
    document.getElementById('roleChangeUserName').textContent = userName;
    document.getElementById('roleChangeCurrentRole').textContent = getRoleDisplayName(currentRole);
    document.getElementById('newRoleSelect').value = currentRole;
    
    document.getElementById('roleChangeModal').style.display = 'flex';
}

// Close role change modal
function closeRoleChangeModal() {
    document.getElementById('roleChangeModal').style.display = 'none';
    selectedUserId = null;
}

// Confirm role change
async function confirmRoleChange() {
    if (!selectedUserId) return;
    
    const newRole = document.getElementById('newRoleSelect').value;
    
    try {
        showLoading();
        
        const response = await makeAuthenticatedRequest(`/api/admin/users/${selectedUserId}/role`, {
            method: 'PUT',
            body: JSON.stringify({ role: newRole })
        });
        
        if (!response) return;
        
        if (response.ok) {
            showSuccess('Kullanıcı rolü başarıyla güncellendi');
            closeRoleChangeModal();
            loadUsers();
            loadUserStats();
        } else {
            const error = await response.json();
            showError(error.message || 'Rol güncellenirken hata oluştu');
        }
    } catch (error) {
        console.error('Error updating user role:', error);
        showError('Rol güncellenirken hata oluştu');
    } finally {
        hideLoading();
    }
}

// Get role display name
function getRoleDisplayName(role) {
    switch (role) {
        case 'NORMAL_USER':
            return 'Normal Kullanıcı';
        case 'ADMIN':
            return 'Yönetici';
        case 'SUPER_ADMIN':
            return 'Süper Yönetici';
        default:
            return role;
    }
}

// Show success message
function showSuccess(message) {
    const successAlert = document.getElementById('successAlert');
    const successMessage = document.getElementById('successMessage');
    
    if (successAlert && successMessage) {
        successMessage.textContent = message;
        successAlert.style.display = 'flex';
        
        // Auto-hide after 3 seconds
        setTimeout(() => {
            hideSuccess();
        }, 3000);
    }
}

// Hide success message
function hideSuccess() {
    const successAlert = document.getElementById('successAlert');
    if (successAlert) {
        successAlert.style.display = 'none';
    }
}

// IP Assignment Modal Functions
let selectedUserIdForIp = null;

// Show IP assignment modal
function showIpAssignmentModal(userId, userName, currentIps) {
    selectedUserIdForIp = userId;
    
    document.getElementById('ipAssignmentUserName').textContent = userName;
    document.getElementById('ipAssignmentCurrentIps').textContent = currentIps || 'Atanmamış';
    document.getElementById('ipAddressesInput').value = currentIps || '';
    
    // Clear validation error
    hideIpValidationError();
    
    document.getElementById('ipAssignmentModal').style.display = 'flex';
}

// Close IP assignment modal
function closeIpAssignmentModal() {
    document.getElementById('ipAssignmentModal').style.display = 'none';
    selectedUserIdForIp = null;
    hideIpValidationError();
}

// Confirm IP assignment
async function confirmIpAssignment() {
    if (!selectedUserIdForIp) return;
    
    const ipAddresses = document.getElementById('ipAddressesInput').value.trim();
    
    // Validate IP addresses
    if (ipAddresses && !validateIpAddresses(ipAddresses)) {
        return; // Validation error already shown
    }
    
    try {
        showLoading();
        
        const response = await makeAuthenticatedRequest(`/api/admin/users/${selectedUserIdForIp}/ip-assignment`, {
            method: 'PUT',
            body: JSON.stringify({ ipAddresses: ipAddresses })
        });
        
        if (!response) return;
        
        if (response.ok) {
            showSuccess('IP adresi ataması başarıyla güncellendi');
            closeIpAssignmentModal();
            loadUsers();
        } else {
            const error = await response.json();
            showError(error.error || 'IP adresi güncellenirken hata oluştu');
        }
    } catch (error) {
        console.error('Error updating IP assignment:', error);
        showError('IP adresi güncellenirken hata oluştu');
    } finally {
        hideLoading();
    }
}

// Remove IP assignment
async function removeIpAssignment() {
    if (!selectedUserIdForIp) return;
    
    if (!confirm('Bu kullanıcının IP adresi atamasını kaldırmak istediğinizden emin misiniz?')) {
        return;
    }
    
    try {
        showLoading();
        
        const response = await makeAuthenticatedRequest(`/api/admin/users/${selectedUserIdForIp}/ip-assignment`, {
            method: 'DELETE'
        });
        
        if (!response) return;
        
        if (response.ok) {
            showSuccess('IP adresi ataması başarıyla kaldırıldı');
            closeIpAssignmentModal();
            loadUsers();
        } else {
            const error = await response.json();
            showError(error.error || 'IP adresi kaldırılırken hata oluştu');
        }
    } catch (error) {
        console.error('Error removing IP assignment:', error);
        showError('IP adresi kaldırılırken hata oluştu');
    } finally {
        hideLoading();
    }
}

// Validate IP addresses
function validateIpAddresses(ipAddresses) {
    if (!ipAddresses || ipAddresses.trim() === '') {
        return true; // Empty is valid (removes assignment)
    }
    
    const ipArray = ipAddresses.split(/[,;]/);
    const invalidIps = [];
    
    for (let ip of ipArray) {
        const trimmedIp = ip.trim();
        if (trimmedIp && !isValidIpAddress(trimmedIp)) {
            invalidIps.push(trimmedIp);
        }
    }
    
    if (invalidIps.length > 0) {
        showIpValidationError(`Geçersiz IP adresi formatı: ${invalidIps.join(', ')}`);
        return false;
    }
    
    hideIpValidationError();
    return true;
}

// Check if IP address is valid (IPv4 or IPv6)
function isValidIpAddress(ip) {
    // IPv4 regex
    const ipv4Regex = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    
    // IPv6 regex (simplified)
    const ipv6Regex = /^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::1$|^::$/;
    
    // IPv6 compressed format
    const ipv6CompressedRegex = /^(?:[0-9a-fA-F]{1,4}:)*::(?:[0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4}$|^::$/;
    
    return ipv4Regex.test(ip) || ipv6Regex.test(ip) || ipv6CompressedRegex.test(ip);
}

// Show IP validation error
function showIpValidationError(message) {
    const errorDiv = document.getElementById('ipValidationError');
    const errorSpan = errorDiv.querySelector('span');
    errorSpan.textContent = message;
    errorDiv.style.display = 'flex';
}

// Hide IP validation error
function hideIpValidationError() {
    const errorDiv = document.getElementById('ipValidationError');
    errorDiv.style.display = 'none';
}

// Format IP addresses for display (truncate if too long)
function formatIpAddresses(ipAddresses) {
    if (!ipAddresses) return '';
    
    const maxLength = 25;
    if (ipAddresses.length <= maxLength) {
        return ipAddresses;
    }
    
    return ipAddresses.substring(0, maxLength) + '...';
}

// Debounce function for search
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}
// IP Tracking Help and Information Functions

// Show IP assignment help modal
async function showIpAssignmentHelp() {
    try {
        const response = await makeAuthenticatedRequest('/api/admin/dashboard/ip-tracking/info');
        
        if (response && response.ok) {
            const info = await response.json();
            showIpHelpModal(info.helpText, info);
        } else {
            showIpHelpModal('IP adresi atama yardımı yüklenemedi.', null);
        }
    } catch (error) {
        console.error('Failed to load IP tracking info:', error);
        showIpHelpModal('IP adresi atama yardımı yüklenemedi.', null);
    }
}

// Show IP help modal with detailed information
function showIpHelpModal(helpText, ipInfo) {
    const modal = document.createElement('div');
    modal.className = 'ip-help-modal';
    
    const statusSection = ipInfo ? `
        <div class="ip-help-section">
            <h4><i class="fas fa-info-circle"></i> IP Takibi Durumu</h4>
            <div class="ip-tracking-status-indicator ${ipInfo.enabled ? 'enabled' : 'disabled'}">
                <i class="fas ${ipInfo.enabled ? 'fa-check-circle' : 'fa-times-circle'}"></i>
                ${ipInfo.statusDisplay}
            </div>
            ${ipInfo.privacyEnabled ? `
                <div class="ip-tracking-status-indicator privacy">
                    <i class="fas fa-user-shield"></i>
                    Gizlilik koruması aktif
                </div>
            ` : ''}
            ${ipInfo.notice ? `
                <div class="ip-privacy-notice">
                    <div class="notice-icon">
                        <i class="fas fa-info-circle"></i>
                    </div>
                    <div class="notice-text">${ipInfo.notice}</div>
                </div>
            ` : ''}
        </div>
    ` : '';
    
    modal.innerHTML = `
        <div class="ip-help-modal-content">
            <div class="ip-help-modal-header">
                <h3><i class="fas fa-network-wired"></i> IP Adresi Atama Yardımı</h3>
                <button class="modal-close" onclick="closeIpHelpModal()">
                    <i class="fas fa-times"></i>
                </button>
            </div>
            <div class="ip-help-modal-body">
                ${statusSection}
                
                <div class="ip-help-section">
                    <h4><i class="fas fa-question-circle"></i> Genel Bilgi</h4>
                    <p>${helpText}</p>
                </div>
                
                <div class="ip-help-section">
                    <h4><i class="fas fa-list"></i> Desteklenen Formatlar</h4>
                    <ul class="ip-help-list">
                        <li><i class="fas fa-check"></i> <strong>IPv4:</strong> <code>192.168.1.100</code></li>
                        <li><i class="fas fa-check"></i> <strong>IPv6:</strong> <code>2001:db8::1</code></li>
                        <li><i class="fas fa-check"></i> <strong>Birden fazla IP (virgül):</strong> <code>192.168.1.100, 10.0.0.50</code></li>
                        <li><i class="fas fa-check"></i> <strong>Birden fazla IP (noktalı virgül):</strong> <code>192.168.1.100; 10.0.0.50</code></li>
                        <li><i class="fas fa-check"></i> <strong>Karışık format:</strong> <code>192.168.1.100, 2001:db8::1</code></li>
                    </ul>
                </div>
                
                <div class="ip-help-section">
                    <h4><i class="fas fa-lightbulb"></i> Kullanım Örnekleri</h4>
                    <div class="ip-help-examples">
                        <p><strong>Tek IPv4 adresi:</strong></p>
                        <code>192.168.1.100</code>
                        
                        <p><strong>Birden fazla IPv4 adresi:</strong></p>
                        <code>192.168.1.100, 192.168.1.101, 10.0.0.50</code>
                        
                        <p><strong>IPv6 adresi:</strong></p>
                        <code>2001:db8::1</code>
                        
                        <p><strong>IPv4 ve IPv6 karışık:</strong></p>
                        <code>192.168.1.100, 2001:db8::1, 10.0.0.50</code>
                        
                        <p><strong>Noktalı virgül ile ayırma:</strong></p>
                        <code>192.168.1.100; 192.168.1.101; 10.0.0.50</code>
                    </div>
                </div>
                
                <div class="ip-help-section">
                    <h4><i class="fas fa-exclamation-triangle"></i> Önemli Notlar</h4>
                    <ul class="ip-help-list">
                        <li><i class="fas fa-info"></i> IP adresi ataması opsiyoneldir. Boş bırakabilirsiniz.</li>
                        <li><i class="fas fa-info"></i> Geçersiz IP formatları otomatik olarak reddedilir.</li>
                        <li><i class="fas fa-info"></i> Atanan IP'ler giriş/çıkış kayıtlarında uyumluluk kontrolü için kullanılır.</li>
                        <li><i class="fas fa-info"></i> Birden fazla IP adresi atayarak kullanıcının farklı lokasyonlardan erişimine izin verebilirsiniz.</li>
                    </ul>
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
    
    // Close modal with Escape key
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
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
    
    // Remove escape key listener
    document.removeEventListener('keydown', closeIpHelpModal);
}

// Add IP tracking status indicator to user management interface
function addIpTrackingStatusIndicator() {
    const userManagementSection = document.querySelector('.search-section');
    if (!userManagementSection) return;
    
    // Check if indicator already exists
    if (document.querySelector('.ip-tracking-status-indicator')) return;
    
    // Load IP tracking info and add indicator
    makeAuthenticatedRequest('/api/admin/dashboard/ip-tracking/info')
        .then(response => response.json())
        .then(info => {
            const indicator = document.createElement('div');
            indicator.className = `ip-tracking-status-indicator ${info.enabled ? 'enabled' : 'disabled'}`;
            indicator.innerHTML = `
                <i class="fas ${info.enabled ? 'fa-shield-alt' : 'fa-shield-alt'}"></i>
                ${info.statusDisplay}
                ${info.privacyEnabled ? ' - Gizlilik koruması aktif' : ''}
            `;
            
            userManagementSection.appendChild(indicator);
            
            // Add privacy notice if needed
            if (info.notice && info.privacyEnabled) {
                const notice = document.createElement('div');
                notice.className = 'ip-privacy-notice';
                notice.innerHTML = `
                    <div class="notice-icon">
                        <i class="fas fa-info-circle"></i>
                    </div>
                    <div class="notice-text">${info.notice}</div>
                `;
                userManagementSection.appendChild(notice);
            }
        })
        .catch(error => {
            console.error('Failed to load IP tracking status:', error);
        });
}

// Initialize IP tracking information display
document.addEventListener('DOMContentLoaded', function() {
    // Add IP tracking status indicator after a short delay to ensure DOM is ready
    setTimeout(addIpTrackingStatusIndicator, 500);
});