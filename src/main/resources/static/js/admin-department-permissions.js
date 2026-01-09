// Admin Department Permissions Management JavaScript

let departmentAdmins = [];
let availableDepartments = [];
let currentEditingUserId = null;

// Initialize page
document.addEventListener('DOMContentLoaded', function() {
    // Wait for admin.js to check auth first
    setTimeout(() => {
        loadData();
    }, 100);
});

// Load all data
async function loadData() {
    showLoading();
    
    try {
        await Promise.all([
            loadDepartmentAdmins(),
            loadAvailableDepartments()
        ]);
        
        displayDepartmentAdmins();
    } catch (error) {
        console.error('Error loading data:', error);
        showError('Veriler yüklenirken hata oluştu');
    } finally {
        hideLoading();
    }
}

// Load department admins
async function loadDepartmentAdmins() {
    try {
        const response = await makeAuthenticatedRequest('/api/admin/department-permissions/department-admins');
        
        if (!response) return;
        
        if (response.ok) {
            departmentAdmins = await response.json();
        } else {
            throw new Error('Failed to load department admins');
        }
    } catch (error) {
        console.error('Error loading department admins:', error);
        throw error;
    }
}

// Load available departments
async function loadAvailableDepartments() {
    try {
        const response = await makeAuthenticatedRequest('/api/admin/department-permissions/available-departments');
        
        if (!response) return;
        
        if (response.ok) {
            availableDepartments = await response.json();
        } else {
            throw new Error('Failed to load available departments');
        }
    } catch (error) {
        console.error('Error loading available departments:', error);
        throw error;
    }
}

// Display department admins
function displayDepartmentAdmins() {
    const container = document.getElementById('departmentAdminsList');
    
    if (!departmentAdmins || departmentAdmins.length === 0) {
        container.innerHTML = `
            <div class="permission-card">
                <div class="permission-card-body" style="text-align: center; padding: 2rem;">
                    <i class="fas fa-user-shield fa-3x" style="color: #ccc; margin-bottom: 1rem;"></i>
                    <h4 style="color: #666;">Departman Admini Bulunamadı</h4>
                    <p style="color: #999;">Henüz hiç departman admini tanımlanmamış.</p>
                </div>
            </div>
        `;
        return;
    }
    
    const adminsHtml = departmentAdmins.map(admin => {
        const permissionsHtml = admin.permissions && admin.permissions.length > 0 
            ? admin.permissions.map(deptCode => {
                const dept = availableDepartments.find(d => d.code === deptCode);
                return `<span class="department-badge">${dept ? dept.name : deptCode}</span>`;
              }).join('')
            : '<span class="no-permissions">Henüz departman yetkisi verilmemiş</span>';
        
        return `
            <div class="permission-card">
                <div class="permission-card-header">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <h4 style="margin: 0; color: #333;">
                                <i class="fas fa-user-shield" style="color: #1976d2;"></i>
                                ${admin.fullName}
                            </h4>
                            <small style="color: #666;">
                                TC: ${admin.tcNo} | 
                                Departman: ${admin.departmentName || admin.departmentCode || 'Bilinmiyor'}
                            </small>
                        </div>
                        <div>
                            <button class="btn btn-primary" onclick="editPermissions(${admin.id}, '${escapeHtml(admin.fullName)}', '${admin.tcNo}', '${escapeHtml(admin.departmentName || admin.departmentCode || '')}')">
                                <i class="fas fa-edit"></i>
                                Düzenle
                            </button>
                        </div>
                    </div>
                </div>
                <div class="permission-card-body">
                    <div style="margin-bottom: 0.5rem;">
                        <strong>Yönetebileceği Departmanlar:</strong>
                    </div>
                    <div>
                        ${permissionsHtml}
                    </div>
                </div>
            </div>
        `;
    }).join('');
    
    container.innerHTML = adminsHtml;
}

// Escape HTML to prevent XSS
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Edit permissions for a user
function editPermissions(userId, fullName, tcNo, departmentName) {
    currentEditingUserId = userId;
    
    // Set user info in modal
    document.getElementById('editUserName').textContent = fullName;
    document.getElementById('editUserInfo').textContent = `TC: ${tcNo} | Departman: ${departmentName}`;
    
    // Get current permissions for this user
    const admin = departmentAdmins.find(a => a.id === userId);
    const currentPermissions = admin ? admin.permissions : [];
    
    // Create department checkboxes
    const checkboxContainer = document.getElementById('departmentCheckboxes');
    const checkboxesHtml = availableDepartments.map(dept => {
        const isChecked = currentPermissions.includes(dept.code);
        return `
            <div class="department-checkbox">
                <input type="checkbox" 
                       id="dept_${dept.code}" 
                       value="${dept.code}" 
                       ${isChecked ? 'checked' : ''}>
                <label for="dept_${dept.code}">
                    ${dept.name} (${dept.userCount} kullanıcı)
                </label>
            </div>
        `;
    }).join('');
    
    checkboxContainer.innerHTML = checkboxesHtml;
    
    // Show modal
    document.getElementById('editPermissionsModal').style.display = 'flex';
}

// Close edit modal
function closeEditModal() {
    document.getElementById('editPermissionsModal').style.display = 'none';
    currentEditingUserId = null;
}

// Save permissions
async function savePermissions() {
    if (!currentEditingUserId) {
        showError('Kullanıcı seçilmedi');
        return;
    }
    
    // Get selected departments
    const checkboxes = document.querySelectorAll('#departmentCheckboxes input[type="checkbox"]:checked');
    const selectedDepartments = Array.from(checkboxes).map(cb => cb.value);
    
    showLoadingOverlay();
    
    try {
        const response = await makeAuthenticatedRequest(
            `/api/admin/department-permissions/user/${currentEditingUserId}`,
            {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    departmentCodes: selectedDepartments
                })
            }
        );
        
        if (!response) {
            hideLoadingOverlay();
            return;
        }
        
        if (response.ok) {
            showSuccess('Departman yetkileri başarıyla güncellendi');
            closeEditModal();
            await loadData();
        } else {
            const error = await response.json();
            showError(error.error || 'Departman yetkileri güncellenirken hata oluştu');
        }
    } catch (error) {
        console.error('Error saving permissions:', error);
        showError('Departman yetkileri güncellenirken hata oluştu');
    } finally {
        hideLoadingOverlay();
    }
}

// Refresh data
async function refreshData() {
    const refreshIcon = document.getElementById('refreshIcon');
    refreshIcon.classList.add('fa-spin');
    
    try {
        await loadData();
        showSuccess('Veriler başarıyla yenilendi');
    } catch (error) {
        showError('Veriler yenilenirken hata oluştu');
    } finally {
        refreshIcon.classList.remove('fa-spin');
    }
}

// Show loading indicator
function showLoading() {
    document.getElementById('loadingIndicator').style.display = 'block';
}

// Hide loading indicator
function hideLoading() {
    document.getElementById('loadingIndicator').style.display = 'none';
}

// Show loading overlay
function showLoadingOverlay() {
    document.getElementById('loadingOverlay').style.display = 'flex';
}

// Hide loading overlay
function hideLoadingOverlay() {
    document.getElementById('loadingOverlay').style.display = 'none';
}

// Show success message (use admin.js function if available)
function showSuccess(message) {
    const successAlert = document.getElementById('successAlert');
    const successMessage = document.getElementById('successMessage');
    
    if (successAlert && successMessage) {
        successMessage.textContent = message;
        successAlert.style.display = 'flex';
        
        setTimeout(() => {
            successAlert.style.display = 'none';
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

// Show error message
function showError(message) {
    const errorAlert = document.getElementById('errorAlert');
    const errorMessage = document.getElementById('errorMessage');
    
    if (errorAlert && errorMessage) {
        errorMessage.textContent = message;
        errorAlert.style.display = 'flex';
        
        setTimeout(() => {
            errorAlert.style.display = 'none';
        }, 5000);
    }
}

// Hide error message
function hideError() {
    const errorAlert = document.getElementById('errorAlert');
    if (errorAlert) {
        errorAlert.style.display = 'none';
    }
}