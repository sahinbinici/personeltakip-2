// Admin Records Management JavaScript

let currentPage = 0;
let currentSize = 20;
let currentFilters = {
    startDate: null,
    endDate: null,
    userId: null,
    departmentCode: null,
    ipAddress: null,
    ipMismatch: null
};
let totalPages = 0;
let allUsers = [];
let allDepartments = [];
let currentUserRole = null; // Store current user's role

// Initialize records page
document.addEventListener('DOMContentLoaded', function() {
    loadCurrentUserRole();
    loadDailyStats();
    loadUsers();
    loadDepartments();
    loadRecords();
    setupEventListeners();
    setDefaultDates();
});

// Load current user's role from JWT token
function loadCurrentUserRole() {
    try {
        const token = localStorage.getItem('token');
        if (token) {
            const payload = JSON.parse(atob(token.split('.')[1]));
            currentUserRole = payload.role;
        }
    } catch (error) {
        console.error('Error parsing JWT token:', error);
        currentUserRole = null;
    }
}

// Check if current user can see all departments
function canSeeAllDepartments() {
    return currentUserRole === 'ADMIN' || currentUserRole === 'SUPER_ADMIN';
}

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
    
    // IP Filter dropdown
    const ipFilterDropdownBtn = document.getElementById('ipFilterDropdownBtn');
    const ipFilterDropdownMenu = document.getElementById('ipFilterDropdownMenu');
    
    ipFilterDropdownBtn.addEventListener('click', toggleIpFilterDropdown);
    
    // IP Filter options
    const ipFilterOptions = document.querySelectorAll('.ip-filter-option');
    ipFilterOptions.forEach(option => {
        option.addEventListener('click', () => selectIpFilterOption(option.dataset.filter));
    });
    
    // Advanced IP Search
    const advancedIpSearchBtn = document.getElementById('advancedIpSearchBtn');
    const closeAdvancedSearchBtn = document.getElementById('closeAdvancedSearchBtn');
    const applyAdvancedSearchBtn = document.getElementById('applyAdvancedSearchBtn');
    const clearAdvancedSearchBtn = document.getElementById('clearAdvancedSearchBtn');
    const saveSearchPresetBtn = document.getElementById('saveSearchPresetBtn');
    
    advancedIpSearchBtn.addEventListener('click', toggleAdvancedIpSearch);
    closeAdvancedSearchBtn.addEventListener('click', closeAdvancedIpSearch);
    applyAdvancedSearchBtn.addEventListener('click', applyAdvancedIpSearch);
    clearAdvancedSearchBtn.addEventListener('click', clearAdvancedIpSearch);
    saveSearchPresetBtn.addEventListener('click', saveSearchPreset);
    
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
    
    // Close dropdowns when clicking outside
    document.addEventListener('click', (event) => {
        if (!event.target.closest('.ip-filter-container')) {
            closeIpFilterDropdown();
        }
    });
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
    
    // Update the first option text based on available departments
    const firstOption = departmentSelect.firstElementChild;
    if (canSeeAllDepartments()) {
        firstOption.textContent = 'Tüm Departmanlar';
    } else {
        // For department admin
        if (allDepartments.length === 1) {
            firstOption.textContent = allDepartments[0].name;
        } else if (allDepartments.length > 1) {
            firstOption.textContent = 'Tüm Departmanlar';
        } else {
            firstOption.textContent = 'Departman Yok';
        }
    }
    
    // Add department options - only if there are multiple departments
    if (allDepartments.length > 1 || canSeeAllDepartments()) {
        allDepartments.forEach(department => {
            const option = document.createElement('option');
            option.value = department.code;
            option.textContent = department.name;
            departmentSelect.appendChild(option);
        });
    }
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
        if (currentFilters.startDate || currentFilters.endDate || currentFilters.userId || 
            currentFilters.departmentCode || currentFilters.ipAddress || currentFilters.ipMismatch) {
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
            if (currentFilters.ipAddress) {
                params.append('ipAddress', currentFilters.ipAddress);
            }
            if (currentFilters.ipMismatch) {
                params.append('ipMismatch', currentFilters.ipMismatch);
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
                <td colspan="11" class="no-data">
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
        
        // Check if this is an excuse-only record (no real QR scan)
        const isExcuseOnlyRecord = record.excuse && record.excuse.trim() !== '' && 
                                   record.qrCodeValue && record.qrCodeValue.startsWith('EXCUSE-');
        
        const gpsInfo = isExcuseOnlyRecord ? '-' : (record.hasGpsCoordinates ? 
            `${record.latitude.toFixed(6)}, ${record.longitude.toFixed(6)}` : 
            'GPS bilgisi yok');
        
        const departmentName = record.userDepartmentName || record.userDepartmentCode || '--';
        
        // Format IP address with mismatch highlighting
        const ipAddressHtml = isExcuseOnlyRecord ? '-' : formatIpAddressDisplay(record);
        
        // Format excuse display with timestamp
        let excuseHtml = '<span class="no-excuse">-</span>';
        if (record.excuse) {
            const excuseTime = formatDateTime(record.timestamp);
            const shortExcuse = record.excuse.length > 30 ? record.excuse.substring(0, 30) + '...' : record.excuse;
            excuseHtml = `<span class="excuse-badge" title="${record.excuse} (${excuseTime})">
                <i class="fas fa-comment-alt"></i> ${shortExcuse}
                <small class="excuse-time">${excuseTime}</small>
            </span>`;
        }
        
        // QR code display - hide for excuse-only records
        const qrCodeHtml = isExcuseOnlyRecord ? '-' : record.qrCodeValue;
        
        // Type display - hide for excuse-only records
        const typeHtml = isExcuseOnlyRecord ? '-' : 
            `<span class="type-badge type-${record.type.toLowerCase()}">${record.typeDisplayName}</span>`;
        
        return `
            <tr ${record.ipMismatch ? 'class="ip-mismatch-row"' : ''}>
                <td>${timestamp}</td>
                <td>${record.userTcNo || 'Bilinmeyen'}</td>
                <td>${record.userFullName || 'Bilinmeyen Kullanıcı'}</td>
                <td>${record.userPersonnelNo || 'Bilinmeyen'}</td>
                <td>${departmentName}</td>
                <td>${typeHtml}</td>
                <td class="excuse-cell">${excuseHtml}</td>
                <td class="ip-address-cell">${ipAddressHtml}</td>
                <td class="qr-code-cell">${qrCodeHtml}</td>
                <td class="gps-cell">${gpsInfo}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-sm btn-info" onclick="showRecordDetail(${record.id})" title="Detayları Görüntüle">
                            <i class="fas fa-eye"></i>
                        </button>
                        ${!isExcuseOnlyRecord && record.hasGpsCoordinates ? `
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
    const ipAddress = document.getElementById('ipAddressFilter').value;
    const ipMismatch = document.getElementById('ipMismatchFilter').value;
    
    currentFilters = {
        startDate: startDate || null,
        endDate: endDate || null,
        userId: userId || null,
        departmentCode: departmentCode || null,
        ipAddress: ipAddress || null,
        ipMismatch: ipMismatch || null
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
    document.getElementById('ipAddressFilter').value = '';
    document.getElementById('ipMismatchFilter').value = '';
    
    currentFilters = {
        startDate: null,
        endDate: null,
        userId: null,
        departmentCode: null,
        ipAddress: null,
        ipMismatch: null
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
        if (currentFilters.ipAddress) {
            params.append('ipAddress', currentFilters.ipAddress);
        }
        if (currentFilters.ipMismatch) {
            params.append('ipMismatch', currentFilters.ipMismatch);
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

// Format IP address display with mismatch highlighting
function formatIpAddressDisplay(record) {
    let ipAddress = record.ipAddress;
    let displayHtml = '';
    
    // Handle unknown IP addresses
    if (!ipAddress || ipAddress === 'Unknown' || ipAddress === 'N/A' || ipAddress.trim() === '') {
        displayHtml = '<span class="ip-unknown">Bilinmeyen</span>';
    } else {
        // Format IPv4 and IPv6 addresses
        const formattedIp = formatIpAddress(ipAddress);
        
        // Check for IP mismatch and add appropriate styling
        if (record.ipMismatch === true) {
            displayHtml = `<span class="ip-mismatch" title="IP adresi uyumsuzluğu tespit edildi">
                            <i class="fas fa-exclamation-triangle"></i> ${formattedIp}
                           </span>`;
        } else if (record.ipMismatch === false) {
            displayHtml = `<span class="ip-match" title="IP adresi uyumlu">
                            <i class="fas fa-check-circle"></i> ${formattedIp}
                           </span>`;
        } else {
            // No assignment or unknown status
            displayHtml = `<span class="ip-normal">${formattedIp}</span>`;
        }
    }
    
    return displayHtml;
}

// Format IP address for display (IPv4 and IPv6)
function formatIpAddress(ipAddress) {
    if (!ipAddress || ipAddress.trim() === '') {
        return 'Bilinmeyen';
    }
    
    const trimmedIp = ipAddress.trim();
    
    // IPv4 formatting - return as-is for readability
    if (isIPv4(trimmedIp)) {
        return trimmedIp;
    }
    
    // IPv6 formatting - ensure lowercase for consistency
    if (isIPv6(trimmedIp)) {
        return trimmedIp.toLowerCase();
    }
    
    // Unknown format - return as-is
    return trimmedIp;
}

// Check if IP address is IPv4 format
function isIPv4(ip) {
    const ipv4Regex = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    return ipv4Regex.test(ip);
}

// Check if IP address is IPv6 format
function isIPv6(ip) {
    // Simple check for IPv6 - contains colons and valid hex characters
    return ip.includes(':') && /^[0-9a-fA-F:]+$/.test(ip);
}

// Show IP address details in a tooltip or modal
function showIpAddressDetails(ipAddress, assignedIps, mismatchStatus) {
    let message = `IP Adresi: ${ipAddress}\n`;
    
    if (assignedIps && assignedIps.length > 0) {
        message += `Atanmış IP'ler: ${assignedIps.join(', ')}\n`;
        message += `Durum: ${mismatchStatus ? 'Uyumsuz' : 'Uyumlu'}`;
    } else {
        message += 'Atanmış IP adresi bulunmuyor';
    }
    
    alert(message);
}

// IP Filter Dropdown Functions
function toggleIpFilterDropdown() {
    const dropdown = document.getElementById('ipFilterDropdownMenu');
    const isVisible = dropdown.style.display !== 'none';
    
    if (isVisible) {
        closeIpFilterDropdown();
    } else {
        dropdown.style.display = 'block';
    }
}

function closeIpFilterDropdown() {
    const dropdown = document.getElementById('ipFilterDropdownMenu');
    dropdown.style.display = 'none';
}

function selectIpFilterOption(filterType) {
    const ipAddressFilter = document.getElementById('ipAddressFilter');
    
    switch (filterType) {
        case 'exact':
            ipAddressFilter.placeholder = 'Tam IP adresi (192.168.1.100)';
            break;
        case 'range':
            ipAddressFilter.placeholder = 'IP aralığı (192.168.1.)';
            break;
        case 'subnet':
            ipAddressFilter.placeholder = 'Alt ağ (192.168.0.0/24)';
            break;
        case 'contains':
            ipAddressFilter.placeholder = 'İçeren metin';
            break;
        case 'unknown':
            ipAddressFilter.value = 'unknown';
            ipAddressFilter.placeholder = 'Bilinmeyen IP adresleri';
            break;
    }
    
    closeIpFilterDropdown();
    ipAddressFilter.focus();
}

// Advanced IP Search Functions
function toggleAdvancedIpSearch() {
    const panel = document.getElementById('advancedIpSearchPanel');
    const isVisible = panel.style.display !== 'none';
    
    if (isVisible) {
        closeAdvancedIpSearch();
    } else {
        panel.style.display = 'block';
        loadCommonIps();
        loadIpStatistics();
    }
}

function closeAdvancedIpSearch() {
    const panel = document.getElementById('advancedIpSearchPanel');
    panel.style.display = 'none';
}

function applyAdvancedIpSearch() {
    const ipRangeStart = document.getElementById('ipRangeStart').value;
    const ipRangeEnd = document.getElementById('ipRangeEnd').value;
    const ipSubnet = document.getElementById('ipSubnet').value;
    const ipTypeFilter = document.getElementById('ipTypeFilter').value;
    const complianceStatus = document.getElementById('complianceStatus').value;
    const commonIpsFilter = document.getElementById('commonIpsFilter').value;
    
    // Build advanced IP filter string
    let advancedFilter = '';
    
    if (ipRangeStart && ipRangeEnd) {
        advancedFilter = `range:${ipRangeStart}-${ipRangeEnd}`;
    } else if (ipSubnet) {
        advancedFilter = `subnet:${ipSubnet}`;
    } else if (commonIpsFilter) {
        advancedFilter = commonIpsFilter;
    }
    
    // Apply the advanced filter
    if (advancedFilter) {
        document.getElementById('ipAddressFilter').value = advancedFilter;
    }
    
    // Apply compliance status filter
    if (complianceStatus) {
        const ipMismatchFilter = document.getElementById('ipMismatchFilter');
        switch (complianceStatus) {
            case 'compliant':
                ipMismatchFilter.value = 'match';
                break;
            case 'non-compliant':
                ipMismatchFilter.value = 'mismatch';
                break;
            case 'no-assignment':
                ipMismatchFilter.value = 'unknown';
                break;
        }
    }
    
    // Apply filters and close advanced search
    applyFilters();
    closeAdvancedIpSearch();
    
    showSuccess('Gelişmiş IP arama filtreleri uygulandı');
}

function clearAdvancedIpSearch() {
    document.getElementById('ipRangeStart').value = '';
    document.getElementById('ipRangeEnd').value = '';
    document.getElementById('ipSubnet').value = '';
    document.getElementById('ipTypeFilter').value = '';
    document.getElementById('complianceStatus').value = '';
    document.getElementById('commonIpsFilter').value = '';
}

function saveSearchPreset() {
    const presetName = prompt('Arama ön ayarı için bir isim girin:');
    if (!presetName) return;
    
    const preset = {
        name: presetName,
        ipRangeStart: document.getElementById('ipRangeStart').value,
        ipRangeEnd: document.getElementById('ipRangeEnd').value,
        ipSubnet: document.getElementById('ipSubnet').value,
        ipTypeFilter: document.getElementById('ipTypeFilter').value,
        complianceStatus: document.getElementById('complianceStatus').value,
        commonIpsFilter: document.getElementById('commonIpsFilter').value
    };
    
    // Save to localStorage
    let presets = JSON.parse(localStorage.getItem('ipSearchPresets') || '[]');
    presets.push(preset);
    localStorage.setItem('ipSearchPresets', JSON.stringify(presets));
    
    showSuccess(`Arama ön ayarı "${presetName}" kaydedildi`);
}

// Load common IP addresses for quick selection
async function loadCommonIps() {
    try {
        // This would typically come from an API endpoint
        // For now, we'll use a placeholder implementation
        const commonIpsSelect = document.getElementById('commonIpsFilter');
        
        // Clear existing options except the first one
        while (commonIpsSelect.children.length > 1) {
            commonIpsSelect.removeChild(commonIpsSelect.lastChild);
        }
        
        // Add some common IP ranges (this could be loaded from API)
        const commonIps = [
            { value: '192.168.1.', label: '192.168.1.x (Yerel Ağ 1)' },
            { value: '192.168.0.', label: '192.168.0.x (Yerel Ağ 2)' },
            { value: '10.0.0.', label: '10.0.0.x (Kurumsal Ağ)' },
            { value: '172.16.', label: '172.16.x.x (Özel Ağ)' },
            { value: 'unknown', label: 'Bilinmeyen IP Adresleri' }
        ];
        
        commonIps.forEach(ip => {
            const option = document.createElement('option');
            option.value = ip.value;
            option.textContent = ip.label;
            commonIpsSelect.appendChild(option);
        });
        
    } catch (error) {
        console.error('Error loading common IPs:', error);
    }
}

// Load IP statistics for advanced search
async function loadIpStatistics() {
    try {
        const response = await makeAuthenticatedRequest('/api/admin/records/ip-statistics');
        
        if (!response) return;
        
        if (response.ok) {
            const stats = await response.json();
            displayIpStatistics(stats);
        } else {
            console.error('Failed to load IP statistics');
        }
    } catch (error) {
        console.error('Error loading IP statistics:', error);
    }
}

// Display IP statistics in the advanced search panel
function displayIpStatistics(stats) {
    // Create or update statistics display
    let statsContainer = document.querySelector('.ip-stats-grid');
    if (!statsContainer) {
        statsContainer = document.createElement('div');
        statsContainer.className = 'ip-stats-grid';
        
        const searchContent = document.querySelector('.advanced-search-content');
        searchContent.insertBefore(statsContainer, searchContent.firstChild);
    }
    
    statsContainer.innerHTML = `
        <div class="ip-stat-item">
            <span class="stat-number">${stats.totalWithIp || 0}</span>
            <span class="stat-label">IP'li Kayıt</span>
        </div>
        <div class="ip-stat-item">
            <span class="stat-number">${stats.ipv4Count || 0}</span>
            <span class="stat-label">IPv4</span>
        </div>
        <div class="ip-stat-item">
            <span class="stat-number">${stats.ipv6Count || 0}</span>
            <span class="stat-label">IPv6</span>
        </div>
        <div class="ip-stat-item">
            <span class="stat-number">${stats.mismatchCount || 0}</span>
            <span class="stat-label">Uyumsuz</span>
        </div>
        <div class="ip-stat-item">
            <span class="stat-number">${stats.matchCount || 0}</span>
            <span class="stat-label">Uyumlu</span>
        </div>
        <div class="ip-stat-item">
            <span class="stat-number">${stats.totalWithoutIp || 0}</span>
            <span class="stat-label">IP'siz</span>
        </div>
    `;
    
    // Update common IPs dropdown with actual data
    updateCommonIpsDropdown(stats.commonIps || []);
}

// Update common IPs dropdown with real data
function updateCommonIpsDropdown(commonIps) {
    const commonIpsSelect = document.getElementById('commonIpsFilter');
    
    // Clear existing options except the first one
    while (commonIpsSelect.children.length > 1) {
        commonIpsSelect.removeChild(commonIpsSelect.lastChild);
    }
    
    // Add common IPs from statistics
    commonIps.forEach(ipInfo => {
        const option = document.createElement('option');
        option.value = ipInfo.ipAddress;
        option.textContent = `${ipInfo.ipAddress} (${ipInfo.count} kayıt, ${ipInfo.type})`;
        commonIpsSelect.appendChild(option);
    });
    
    // Add some predefined ranges
    const predefinedRanges = [
        { value: '192.168.1.', label: '192.168.1.x (Yerel Ağ 1)' },
        { value: '192.168.0.', label: '192.168.0.x (Yerel Ağ 2)' },
        { value: '10.0.0.', label: '10.0.0.x (Kurumsal Ağ)' },
        { value: '172.16.', label: '172.16.x.x (Özel Ağ)' },
        { value: 'unknown', label: 'Bilinmeyen IP Adresleri' }
    ];
    
    predefinedRanges.forEach(range => {
        const option = document.createElement('option');
        option.value = range.value;
        option.textContent = range.label;
        commonIpsSelect.appendChild(option);
    });
}

// Enhanced IP address validation
function validateIpAddress(ip) {
    if (!ip || ip.trim() === '') return false;
    
    // Check for special filters
    if (ip === 'unknown' || ip.startsWith('range:') || ip.startsWith('subnet:')) {
        return true;
    }
    
    // IPv4 validation
    const ipv4Regex = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    if (ipv4Regex.test(ip)) return true;
    
    // IPv4 range validation (e.g., 192.168.1.)
    const ipv4RangeRegex = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){1,3}$/;
    if (ipv4RangeRegex.test(ip)) return true;
    
    // CIDR notation validation
    const cidrRegex = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/([0-9]|[1-2][0-9]|3[0-2])$/;
    if (cidrRegex.test(ip)) return true;
    
    // IPv6 validation (basic)
    const ipv6Regex = /^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$/;
    if (ipv6Regex.test(ip)) return true;
    
    return false;
}

// Enhanced IP address formatting with type detection
function formatIpAddressWithType(ipAddress) {
    if (!ipAddress || ipAddress === 'Unknown' || ipAddress === 'N/A' || ipAddress.trim() === '') {
        return '<span class="ip-unknown">Bilinmeyen</span>';
    }
    
    const trimmedIp = ipAddress.trim();
    let typeClass = 'ip-normal';
    let typeIcon = '';
    
    if (isIPv4(trimmedIp)) {
        typeClass = 'ipv4-address';
        typeIcon = '<i class="fas fa-network-wired" title="IPv4"></i> ';
    } else if (isIPv6(trimmedIp)) {
        typeClass = 'ipv6-address';
        typeIcon = '<i class="fas fa-project-diagram" title="IPv6"></i> ';
    }
    
    return `<span class="${typeClass}">${typeIcon}${trimmedIp}</span>`;
}

// IP address search and highlighting
function highlightIpMatches(ipAddress, searchTerm) {
    if (!searchTerm || !ipAddress) return ipAddress;
    
    const regex = new RegExp(`(${searchTerm.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
    return ipAddress.replace(regex, '<mark>$1</mark>');
}
// IP Tracking Information Functions for Records Page

// Add IP tracking information section to records page
function addIpTrackingInfoSection() {
    const section = document.getElementById('ipTrackingInfoSection');
    if (!section) return;
    
    // Load IP tracking info and display
    makeAuthenticatedRequest('/api/admin/dashboard/ip-tracking/info')
        .then(response => response.json())
        .then(info => {
            if (info.enabled) {
                section.innerHTML = `
                    <section class="dashboard-section">
                        <div class="section-header">
                            <h2><i class="fas fa-network-wired"></i> IP Takibi Bilgileri</h2>
                            <button class="btn btn-secondary" onclick="showIpAssignmentHelp()">
                                <i class="fas fa-question-circle"></i> Yardım
                            </button>
                        </div>
                        
                        <div class="ip-tracking-info-grid">
                            <div class="ip-info-card">
                                <div class="ip-info-header">
                                    <div class="ip-info-icon success">
                                        <i class="fas fa-shield-alt"></i>
                                    </div>
                                    <div class="ip-info-content">
                                        <h3>IP Takibi Aktif</h3>
                                        <p>Giriş/çıkış kayıtlarında IP adresleri görüntüleniyor</p>
                                    </div>
                                </div>
                                <div class="ip-info-status success">
                                    <span class="status-text">${info.statusDisplay}</span>
                                </div>
                            </div>
                            
                            ${info.privacyEnabled ? `
                                <div class="ip-info-card">
                                    <div class="ip-info-header">
                                        <div class="ip-info-icon info">
                                            <i class="fas fa-user-shield"></i>
                                        </div>
                                        <div class="ip-info-content">
                                            <h3>Gizlilik Koruması</h3>
                                            <p>IP adresleri gizlilik ayarlarına uygun görüntüleniyor</p>
                                        </div>
                                    </div>
                                    <div class="ip-info-status info">
                                        <span class="status-text">${info.privacyInfo}</span>
                                    </div>
                                </div>
                            ` : ''}
                        </div>
                        
                        ${info.notice ? `
                            <div class="ip-tracking-notice">
                                <div class="notice-icon">
                                    <i class="fas fa-info-circle"></i>
                                </div>
                                <div class="notice-content">
                                    <p>${info.notice}</p>
                                </div>
                            </div>
                        ` : ''}
                    </section>
                `;
                section.style.display = 'block';
            }
        })
        .catch(error => {
            console.error('Failed to load IP tracking info:', error);
        });
}

// Show IP assignment help modal (reuse from admin-users.js)
async function showIpAssignmentHelp() {
    try {
        const response = await makeAuthenticatedRequest('/api/admin/dashboard/ip-tracking/info');
        
        if (response && response.ok) {
            const info = await response.json();
            showIpHelpModal(info.helpText, info);
        } else {
            showIpHelpModal('IP adresi takibi yardımı yüklenemedi.', null);
        }
    } catch (error) {
        console.error('Failed to load IP tracking info:', error);
        showIpHelpModal('IP adresi takibi yardımı yüklenemedi.', null);
    }
}

// Show IP help modal for records page
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
                    Gizlilik koruması aktif - IP adresleri maskelenebilir
                </div>
            ` : ''}
        </div>
    ` : '';
    
    modal.innerHTML = `
        <div class="ip-help-modal-content">
            <div class="ip-help-modal-header">
                <h3><i class="fas fa-network-wired"></i> IP Takibi Bilgileri</h3>
                <button class="modal-close" onclick="closeIpHelpModal()">
                    <i class="fas fa-times"></i>
                </button>
            </div>
            <div class="ip-help-modal-body">
                ${statusSection}
                
                <div class="ip-help-section">
                    <h4><i class="fas fa-question-circle"></i> IP Takibi Nedir?</h4>
                    <p>IP takibi, giriş/çıkış işlemlerinin hangi IP adresinden yapıldığını kaydetme özelliğidir. Bu sayede:</p>
                    <ul class="ip-help-list">
                        <li><i class="fas fa-check"></i> Hangi cihazlardan erişim yapıldığını görebilirsiniz</li>
                        <li><i class="fas fa-check"></i> Güvenlik kontrolü yapabilirsiniz</li>
                        <li><i class="fas fa-check"></i> Kullanıcı davranışlarını analiz edebilirsiniz</li>
                        <li><i class="fas fa-check"></i> IP uyumluluk raporları oluşturabilirsiniz</li>
                    </ul>
                </div>
                
                <div class="ip-help-section">
                    <h4><i class="fas fa-filter"></i> IP Filtreleme</h4>
                    <p>Kayıtları IP adresine göre filtreleyebilirsiniz:</p>
                    <div class="ip-help-examples">
                        <p><strong>Tam eşleşme:</strong> <code>192.168.1.100</code></p>
                        <p><strong>IP aralığı:</strong> <code>192.168.1.</code> (192.168.1.x tüm IP'ler)</p>
                        <p><strong>Alt ağ:</strong> <code>192.168.0.0/24</code></p>
                        <p><strong>Bilinmeyen IP'ler:</strong> "Bilinmeyen IP" filtresini kullanın</p>
                    </div>
                </div>
                
                <div class="ip-help-section">
                    <h4><i class="fas fa-exclamation-triangle"></i> IP Uyumluluk</h4>
                    <p>Kullanıcılara atanan IP adresleri ile gerçek IP adresleri karşılaştırılır:</p>
                    <ul class="ip-help-list">
                        <li><i class="fas fa-check-circle" style="color: var(--success-color);"></i> <strong>Uyumlu:</strong> Kullanıcı atanan IP'den erişim yaptı</li>
                        <li><i class="fas fa-exclamation-triangle" style="color: var(--warning-color);"></i> <strong>Uyumsuz:</strong> Kullanıcı farklı IP'den erişim yaptı</li>
                        <li><i class="fas fa-question-circle" style="color: var(--info-color);"></i> <strong>Bilinmeyen:</strong> Kullanıcıya IP atanmamış</li>
                    </ul>
                </div>
                
                ${ipInfo && ipInfo.privacyEnabled ? `
                    <div class="ip-help-section">
                        <h4><i class="fas fa-user-shield"></i> Gizlilik Koruması</h4>
                        <p>Gizlilik koruması aktif olduğunda:</p>
                        <ul class="ip-help-list">
                            <li><i class="fas fa-info"></i> IP adresleri kısmen maskelenebilir</li>
                            <li><i class="fas fa-info"></i> Raporlarda IP adresleri anonimleştirilebilir</li>
                            <li><i class="fas fa-info"></i> Erişim logları güvenli şekilde tutulur</li>
                        </ul>
                    </div>
                ` : ''}
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

// Initialize IP tracking information display for records page
document.addEventListener('DOMContentLoaded', function() {
    // Add IP tracking info section after a short delay
    setTimeout(addIpTrackingInfoSection, 500);
});