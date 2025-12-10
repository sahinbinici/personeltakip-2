// Admin Records Management JavaScript

let currentPage = 0;
let currentSize = 20;
let currentFilters = {
    startDate: null,
    endDate: null,
    userId: null,
    departmentCode: null
};
let totalPages = 0;
let allUsers = [];
let allDepartments = [];

// Initialize records page
document.addEventListener('DOMContentLoaded', function() {
    loadDailyStats();
    loadUsers();
    loadDepartments();
    loadRecords();
    setupEventListeners();
    setDefaultDates();
});

// Setup event listeners
function setupEventListeners() {
    // Filter buttons
    const applyFiltersBtn = document.getElementById('applyFiltersBtn');
    const clearFiltersBtn = document.getElementById('clearFiltersBtn');
    const exportCsvBtn = document.getElementById('exportCsvBtn');
    const refreshBtn = document.getElementById('refreshBtn');
    
    applyFiltersBtn.addEventListener('click', applyFilters);
    clearFiltersBtn.addEventListener('click', clearFilters);
    exportCsvBtn.addEventListener('click', exportCsv);
    refreshBtn.addEventListener('click', refreshRecords);
    
    // Quick filter buttons
    const quickFilterBtns = document.querySelectorAll('.quick-filter-btn');
    quickFilterBtns.forEach(btn => {
        btn.addEventListener('click', () => applyQuickFilter(btn.dataset.period));
    });
    
    // Pagination
    const prevPageBtn = document.getElementById('prevPageBtn');
    const nextPageBtn = document.getElementById('nextPageBtn');
    
    prevPageBtn.addEventListener('click', () => changePage(currentPage - 1));
    nextPageBtn.addEventListener('click', () => changePage(currentPage + 1));
}

// Set default dates (last 7 days)
function setDefaultDates() {
    const today = new Date();
    const weekAgo = new Date(today);
    weekAgo.setDate(today.getDate() - 7);
    
    document.getElementById('endDate').value = formatDateForInput(today);
    document.getElementById('startDate').value = formatDateForInput(weekAgo);
}

// Load daily statistics
async function loadDailyStats() {
    try {
        const response = await makeAuthenticatedRequest('/api/admin/records/stats/daily');
        
        if (!response) return;
        
        if (response.ok) {
            const stats = await response.json();
            updateDailyStats(stats);
        } else {
            console.error('Failed to load daily stats');
        }
    } catch (error) {
        console.error('Error loading daily stats:', error);
    }
}

// Update daily statistics in UI
function updateDailyStats(stats) {
    document.getElementById('todayTotalRecords').textContent = stats.totalRecords || 0;
    document.getElementById('todayEntryCount').textContent = stats.entryCount || 0;
    document.getElementById('todayExitCount').textContent = stats.exitCount || 0;
    document.getElementById('todayUniqueUsers').textContent = stats.uniqueUsers || 0;
}

// Load users for filter dropdown
async function loadUsers() {
    try {
        const response = await makeAuthenticatedRequest('/api/admin/users?size=1000');
        
        if (!response) return;
        
        if (response.ok) {
            const data = await response.json();
            allUsers = data.content || [];
            populateUserSelect();
        } else {
            console.error('Failed to load users');
        }
    } catch (error) {
        console.error('Error loading users:', error);
    }
}

// Populate user select dropdown
function populateUserSelect() {
    const userSelect = document.getElementById('userSelect');
    
    // Clear existing options except the first one
    while (userSelect.children.length > 1) {
        userSelect.removeChild(userSelect.lastChild);
    }
    
    // Add user options
    allUsers.forEach(user => {
        const option = document.createElement('option');
        option.value = user.id;
        option.textContent = `${user.tcNo} - ${user.firstName} ${user.lastName}`;
        userSelect.appendChild(option);
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
    const departmentSelect = document.getElementById('departmentSelect');
    
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

// Load records with current filters
async function loadRecords() {
    showLoading();
    
    try {
        let url = '/api/admin/records';
        const params = new URLSearchParams({
            page: currentPage,
            size: currentSize
        });
        
        // Add filters if any are set
        if (currentFilters.startDate || currentFilters.endDate || currentFilters.userId || currentFilters.departmentCode) {
            url = '/api/admin/records/filter';
            
            if (currentFilters.startDate) {
                params.append('startDate', currentFilters.startDate);
            }
            if (currentFilters.endDate) {
                params.append('endDate', currentFilters.endDate);
            }
            if (currentFilters.userId) {
                params.append('userId', currentFilters.userId);
            }
            if (currentFilters.departmentCode) {
                params.append('departmentCode', currentFilters.departmentCode);
            }
        }
        
        const response = await makeAuthenticatedRequest(`${url}?${params}`);
        
        if (!response) return;
        
        if (response.ok) {
            const data = await response.json();
            displayRecords(data);
            updatePagination(data);
        } else {
            showError('Kayıtlar yüklenirken hata oluştu');
        }
    } catch (error) {
        console.error('Error loading records:', error);
        showError('Kayıtlar yüklenirken hata oluştu');
    } finally {
        hideLoading();
    }
}

// Display records in table
function displayRecords(data) {
    const tbody = document.getElementById('recordsTableBody');
    const tableInfo = document.getElementById('tableInfo');
    
    if (!data.content || data.content.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" class="no-data">
                    <div class="no-data-message">
                        <i class="fas fa-clipboard-list"></i>
                        <p>Kayıt bulunamadı</p>
                    </div>
                </td>
            </tr>
        `;
        tableInfo.textContent = 'Kayıt bulunamadı';
        return;
    }
    
    const recordsHtml = data.content.map(record => {
        const timestamp = formatDateTime(record.timestamp);
        const gpsInfo = record.hasGpsCoordinates ? 
            `${record.latitude.toFixed(6)}, ${record.longitude.toFixed(6)}` : 
            'GPS bilgisi yok';
        
        const departmentName = record.userDepartmentName || record.userDepartmentCode || '--';
        
        return `
            <tr>
                <td>${timestamp}</td>
                <td>${record.userTcNo || 'Bilinmeyen'}</td>
                <td>${record.userFullName || 'Bilinmeyen Kullanıcı'}</td>
                <td>${record.userPersonnelNo || 'Bilinmeyen'}</td>
                <td>${departmentName}</td>
                <td>
                    <span class="type-badge type-${record.type.toLowerCase()}">${record.typeDisplayName}</span>
                </td>
                <td class="qr-code-cell">${record.qrCodeValue}</td>
                <td class="gps-cell">${gpsInfo}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-sm btn-info" onclick="showRecordDetail(${record.id})" title="Detayları Görüntüle">
                            <i class="fas fa-eye"></i>
                        </button>
                        ${record.hasGpsCoordinates ? `
                            <button class="btn btn-sm btn-success" onclick="showLocationOnMap(${record.latitude}, ${record.longitude})" title="Haritada Göster">
                                <i class="fas fa-map-marker-alt"></i>
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
    
    tbody.innerHTML = recordsHtml;
    
    // Update table info
    const start = data.number * data.size + 1;
    const end = Math.min((data.number + 1) * data.size, data.totalElements);
    tableInfo.textContent = `${start}-${end} / ${data.totalElements} kayıt`;
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
    loadRecords();
}

// Apply filters
function applyFilters() {
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const userId = document.getElementById('userSelect').value;
    const departmentCode = document.getElementById('departmentSelect').value;
    
    currentFilters = {
        startDate: startDate || null,
        endDate: endDate || null,
        userId: userId || null,
        departmentCode: departmentCode || null
    };
    
    currentPage = 0;
    loadRecords();
    
    // Update quick filter buttons
    updateQuickFilterButtons();
}

// Clear filters
function clearFilters() {
    document.getElementById('startDate').value = '';
    document.getElementById('endDate').value = '';
    document.getElementById('userSelect').value = '';
    document.getElementById('departmentSelect').value = '';
    
    currentFilters = {
        startDate: null,
        endDate: null,
        userId: null,
        departmentCode: null
    };
    
    currentPage = 0;
    loadRecords();
    
    // Update quick filter buttons
    updateQuickFilterButtons();
}

// Apply quick filter
function applyQuickFilter(period) {
    const today = new Date();
    let startDate, endDate;
    
    switch (period) {
        case 'today':
            startDate = endDate = today;
            break;
        case 'yesterday':
            startDate = endDate = new Date(today);
            startDate.setDate(today.getDate() - 1);
            endDate = startDate;
            break;
        case 'week':
            startDate = new Date(today);
            startDate.setDate(today.getDate() - today.getDay()); // Start of week (Sunday)
            endDate = today;
            break;
        case 'month':
            startDate = new Date(today.getFullYear(), today.getMonth(), 1);
            endDate = today;
            break;
    }
    
    document.getElementById('startDate').value = formatDateForInput(startDate);
    document.getElementById('endDate').value = formatDateForInput(endDate);
    
    applyFilters();
}

// Update quick filter button states
function updateQuickFilterButtons() {
    const quickFilterBtns = document.querySelectorAll('.quick-filter-btn');
    quickFilterBtns.forEach(btn => btn.classList.remove('active'));
}

// Export CSV
async function exportCsv() {
    try {
        showLoading();
        
        let url = '/api/admin/records/export/csv';
        const params = new URLSearchParams();
        
        if (currentFilters.startDate) {
            params.append('startDate', currentFilters.startDate);
        }
        if (currentFilters.endDate) {
            params.append('endDate', currentFilters.endDate);
        }
        if (currentFilters.userId) {
            params.append('userId', currentFilters.userId);
        }
        if (currentFilters.departmentCode) {
            params.append('departmentCode', currentFilters.departmentCode);
        }
        
        if (params.toString()) {
            url += '?' + params.toString();
        }
        
        const response = await makeAuthenticatedRequest(url);
        
        if (!response) return;
        
        if (response.ok) {
            const csvContent = await response.text();
            downloadCsv(csvContent, 'giris_cikis_kayitlari.csv');
            showSuccess('CSV dosyası başarıyla indirildi');
        } else {
            showError('CSV dışa aktarımında hata oluştu');
        }
    } catch (error) {
        console.error('Error exporting CSV:', error);
        showError('CSV dışa aktarımında hata oluştu');
    } finally {
        hideLoading();
    }
}

// Download CSV file
function downloadCsv(content, filename) {
    const blob = new Blob(['\ufeff' + content], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    
    if (link.download !== undefined) {
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', filename);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
}

// Refresh records
function refreshRecords() {
    const refreshBtn = document.getElementById('refreshBtn');
    const icon = refreshBtn.querySelector('i');
    
    icon.classList.add('fa-spin');
    refreshBtn.disabled = true;
    
    Promise.all([loadDailyStats(), loadRecords()]).finally(() => {
        icon.classList.remove('fa-spin');
        refreshBtn.disabled = false;
    });
}

// Show record detail modal
async function showRecordDetail(recordId) {
    try {
        showLoading();
        
        // Find record in current data
        const tbody = document.getElementById('recordsTableBody');
        const rows = tbody.querySelectorAll('tr');
        
        // For now, we'll create a simple detail view
        // In a real implementation, you might want to fetch full details from API
        
        const content = document.getElementById('recordDetailContent');
        content.innerHTML = `
            <div class="record-detail-grid">
                <div class="detail-row">
                    <label>Kayıt ID:</label>
                    <span>${recordId}</span>
                </div>
                <div class="detail-row">
                    <label>Detay Bilgisi:</label>
                    <span>Kayıt detayları yükleniyor...</span>
                </div>
            </div>
        `;
        
        document.getElementById('recordDetailModal').style.display = 'flex';
    } catch (error) {
        console.error('Error showing record detail:', error);
        showError('Kayıt detayları yüklenirken hata oluştu');
    } finally {
        hideLoading();
    }
}

// Close record detail modal
function closeRecordDetailModal() {
    document.getElementById('recordDetailModal').style.display = 'none';
}

// Show location on map (placeholder function)
function showLocationOnMap(latitude, longitude) {
    const mapUrl = `https://www.google.com/maps?q=${latitude},${longitude}`;
    window.open(mapUrl, '_blank');
}

// Format date for input field
function formatDateForInput(date) {
    return date.toISOString().split('T')[0];
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