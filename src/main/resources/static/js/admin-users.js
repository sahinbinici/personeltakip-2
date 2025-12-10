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
                <td colspan="8" class="no-data">
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
        
        return `
            <tr>
                <td>${user.tcNo}</td>
                <td>${fullName}</td>
                <td>${user.personnelNo}</td>
                <td>${departmentName}</td>
                <td>${user.mobilePhone}</td>
                <td>
                    <span class="role-badge role-${user.role.toLowerCase()}">${roleDisplayName}</span>
                </td>
                <td>${createdAt}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-sm btn-info" onclick="showUserDetail(${user.id})" title="Detayları Görüntüle">
                            <i class="fas fa-eye"></i>
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