// Admin Attendance Reports JavaScript

let currentReport = null;
let currentTab = 'withEntries';

// Initialize page
document.addEventListener('DOMContentLoaded', function() {
    checkDepartmentPermissions();
    loadDepartments();
    loadReport();
});

// Check department permissions for sidebar
function checkDepartmentPermissions() {
    try {
        const token = localStorage.getItem('authToken');
        if (token) {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const role = payload.role;
            
            if (role === 'ADMIN' || role === 'SUPER_ADMIN') {
                const deptLink = document.getElementById('departmentPermissionsLink');
                if (deptLink) deptLink.style.display = 'block';
            }
        }
    } catch (error) {
        console.error('Error checking permissions:', error);
    }
}

// Load departments for filter dropdown
async function loadDepartments() {
    try {
        const response = await makeAuthenticatedRequest('/api/admin/attendance-reports/departments');
        
        if (!response) return;
        
        if (response.ok) {
            const departments = await response.json();
            populateDepartmentSelect(departments);
        }
    } catch (error) {
        console.error('Error loading departments:', error);
    }
}

// Populate department select dropdown
function populateDepartmentSelect(departments) {
    const select = document.getElementById('departmentSelect');
    
    departments.forEach(dept => {
        const option = document.createElement('option');
        option.value = dept.code;
        option.textContent = dept.name;
        select.appendChild(option);
    });
}

// Load report data
async function loadReport() {
    showLoading();
    
    try {
        const period = document.getElementById('periodSelect').value;
        const departmentCode = document.getElementById('departmentSelect').value;
        
        const params = new URLSearchParams({ period });
        if (departmentCode) {
            params.append('departmentCode', departmentCode);
        }
        
        const response = await makeAuthenticatedRequest(`/api/admin/attendance-reports?${params}`);
        
        if (!response) return;
        
        if (response.ok) {
            currentReport = await response.json();
            updateStatistics();
            renderTable();
        } else {
            showError('Rapor yüklenirken hata oluştu');
        }
    } catch (error) {
        console.error('Error loading report:', error);
        showError('Rapor yüklenirken hata oluştu');
    } finally {
        hideLoading();
    }
}


// Update statistics boxes
function updateStatistics() {
    if (!currentReport) return;
    
    document.getElementById('totalPersonnel').textContent = currentReport.totalPersonnel || 0;
    document.getElementById('withEntries').textContent = currentReport.personnelWithEntries || 0;
    document.getElementById('withoutEntries').textContent = currentReport.personnelWithoutEntries || 0;
    document.getElementById('lateEntries').textContent = currentReport.lateEntries || 0;
    document.getElementById('earlyExits').textContent = currentReport.earlyExits || 0;
}

// Show tab
function showTab(tabName) {
    currentTab = tabName;
    
    // Update tab buttons
    document.querySelectorAll('.report-tab').forEach(tab => {
        tab.classList.remove('active');
        if (tab.dataset.tab === tabName) {
            tab.classList.add('active');
        }
    });
    
    renderTable();
}

// Render table based on current tab
function renderTable() {
    if (!currentReport) return;
    
    const tbody = document.getElementById('reportTableBody');
    let data = [];
    
    switch (currentTab) {
        case 'withEntries':
            data = currentReport.personnelWithEntriesList || [];
            break;
        case 'withoutEntries':
            data = currentReport.personnelWithoutEntriesList || [];
            break;
        case 'lateEntries':
            data = currentReport.lateEntriesList || [];
            break;
        case 'earlyExits':
            data = currentReport.earlyExitsList || [];
            break;
    }
    
    if (data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="no-data">Bu kategoride kayıt bulunamadı</td></tr>';
        return;
    }
    
    tbody.innerHTML = data.map(person => `
        <tr>
            <td>${person.tcNo || '-'}</td>
            <td>${person.fullName || '-'}</td>
            <td>${person.personnelNo || '-'}</td>
            <td>${person.departmentName || '-'}</td>
            <td>${person.entryCount || 0}</td>
            <td>${person.exitCount || 0}</td>
            <td>${person.lateEntryCount > 0 ? 
                '<span class="badge badge-warning">' + person.lateEntryCount + '</span>' : 
                '<span class="badge badge-success">0</span>'}</td>
            <td>${person.earlyExitCount > 0 ? 
                '<span class="badge badge-warning">' + person.earlyExitCount + '</span>' : 
                '<span class="badge badge-success">0</span>'}</td>
            <td>${person.lastEntryTime ? formatDateTime(person.lastEntryTime) : '-'}</td>
        </tr>
    `).join('');
}

// Export report to CSV
function exportReport() {
    if (!currentReport) {
        showError('Dışa aktarılacak rapor bulunamadı');
        return;
    }
    
    let data = [];
    let filename = 'rapor';
    
    switch (currentTab) {
        case 'withEntries':
            data = currentReport.personnelWithEntriesList || [];
            filename = 'giris_cikis_yapanlar';
            break;
        case 'withoutEntries':
            data = currentReport.personnelWithoutEntriesList || [];
            filename = 'giris_cikis_yapmayanlar';
            break;
        case 'lateEntries':
            data = currentReport.lateEntriesList || [];
            filename = 'gec_girenler';
            break;
        case 'earlyExits':
            data = currentReport.earlyExitsList || [];
            filename = 'erken_cikanlar';
            break;
    }
    
    if (data.length === 0) {
        showError('Dışa aktarılacak veri bulunamadı');
        return;
    }
    
    // Build CSV content
    const headers = ['TC Kimlik No', 'Ad Soyad', 'Sicil No', 'Departman', 'Giriş Sayısı', 'Çıkış Sayısı', 'Geç Giriş', 'Erken Çıkış', 'Son Giriş'];
    const rows = data.map(p => [
        p.tcNo || '',
        p.fullName || '',
        p.personnelNo || '',
        p.departmentName || '',
        p.entryCount || 0,
        p.exitCount || 0,
        p.lateEntryCount || 0,
        p.earlyExitCount || 0,
        p.lastEntryTime ? formatDateTime(p.lastEntryTime) : ''
    ]);
    
    const csvContent = [headers, ...rows]
        .map(row => row.map(cell => `"${cell}"`).join(','))
        .join('\n');
    
    downloadCsv(csvContent, `${filename}_${formatDateForFilename(new Date())}.csv`);
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

// Format date for filename
function formatDateForFilename(date) {
    return date.toISOString().split('T')[0];
}

// Show loading overlay
function showLoading() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) overlay.style.display = 'flex';
}

// Hide loading overlay
function hideLoading() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) overlay.style.display = 'none';
}

// Show error message
function showError(message) {
    alert(message);
}
