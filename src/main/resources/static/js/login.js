// Check if already logged in - only run if we're on the login page
(async function checkExistingAuth() {
    // Only check authentication if we're actually on the login page
    if (window.location.pathname !== '/login') {
        return;
    }
    
    const token = localStorage.getItem('authToken');
    const user = localStorage.getItem('user');
    
    if (token && user) {
        try {
            const userData = JSON.parse(user);
            
            // Choose appropriate API endpoint based on user role
            let testEndpoint = '/api/qrcode/daily'; // Default for normal users
            if (userData.role === 'ADMIN' || userData.role === 'SUPER_ADMIN') {
                testEndpoint = '/api/admin/dashboard/stats';
            }
            
            // Verify token is still valid by making a test API call
            const response = await fetch(testEndpoint, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            
            if (response.ok) {
                // Token is valid, redirect based on role
                console.log('Valid token found, redirecting user');
                if (userData.role === 'ADMIN' || userData.role === 'SUPER_ADMIN') {
                    window.location.href = '/admin/dashboard';
                } else {
                    window.location.href = '/qrcode';
                }
            } else {
                // Token is invalid, clear storage and stay on login page
                console.log('Token validation failed, clearing storage');
                localStorage.removeItem('authToken');
                localStorage.removeItem('tokenType');
                localStorage.removeItem('user');
                document.cookie = 'jwt=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
            }
        } catch (e) {
            // Error occurred, clear storage and stay on login page
            console.log('Error validating token:', e);
            localStorage.removeItem('authToken');
            localStorage.removeItem('tokenType');
            localStorage.removeItem('user');
            document.cookie = 'jwt=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
        }
    }
})();

// Utility functions
function showError(elementId, message) {
    const errorElement = document.getElementById(elementId);
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.style.display = 'block';
    }
}

function clearError(elementId) {
    const errorElement = document.getElementById(elementId);
    if (errorElement) {
        errorElement.textContent = '';
        errorElement.style.display = 'none';
    }
}

function showAlert(alertId, message) {
    const alertElement = document.getElementById(alertId);
    if (alertElement) {
        alertElement.textContent = message;
        alertElement.style.display = 'block';
    }
}

function hideAlert(alertId) {
    const alertElement = document.getElementById(alertId);
    if (alertElement) {
        alertElement.style.display = 'none';
    }
}

function setLoading(buttonId, isLoading) {
    const button = document.getElementById(buttonId);
    if (button) {
        const btnText = button.querySelector('.btn-text');
        const spinner = button.querySelector('.spinner');
        
        button.disabled = isLoading;
        if (btnText) btnText.style.display = isLoading ? 'none' : 'inline';
        if (spinner) spinner.style.display = isLoading ? 'inline-block' : 'none';
    }
}

// Validation functions
function validateTcNo(tcNo) {
    if (!tcNo) {
        return 'TC Kimlik No zorunludur';
    }
    if (!/^\d{11}$/.test(tcNo)) {
        return 'TC Kimlik No 11 haneli olmalıdır';
    }
    return null;
}

function validatePassword(password) {
    if (!password) {
        return 'Şifre zorunludur';
    }
    return null;
}

// Login form submission
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    // Clear previous errors
    clearError('tcNoError');
    clearError('passwordError');
    hideAlert('loginError');
    
    const tcNo = document.getElementById('tcNo').value.trim();
    const password = document.getElementById('password').value;
    
    // Validate inputs
    const tcNoError = validateTcNo(tcNo);
    const passwordError = validatePassword(password);
    
    let hasError = false;
    
    if (tcNoError) {
        showError('tcNoError', tcNoError);
        document.getElementById('tcNo').classList.add('error');
        hasError = true;
    } else {
        document.getElementById('tcNo').classList.remove('error');
    }
    
    if (passwordError) {
        showError('passwordError', passwordError);
        document.getElementById('password').classList.add('error');
        hasError = true;
    } else {
        document.getElementById('password').classList.remove('error');
    }
    
    if (hasError) {
        return;
    }
    
    // Call login API
    setLoading('loginBtn', true);
    
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ tcNo, password })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            // Store JWT token in localStorage
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('tokenType', data.tokenType);
            localStorage.setItem('user', JSON.stringify(data.user));
            
            // Also store token in cookie for server-side authentication
            document.cookie = `jwt=${data.token}; path=/; max-age=1800; SameSite=Lax`;
            
            // Redirect based on user role
            if (data.user.role === 'ADMIN' || data.user.role === 'SUPER_ADMIN') {
                window.location.href = '/admin/dashboard';
            } else {
                window.location.href = '/qrcode';
            }
        } else {
            // Show error message
            const errorMessage = data.message || 'TC Kimlik No veya şifre hatalı';
            showAlert('loginError', errorMessage);
        }
    } catch (error) {
        showAlert('loginError', 'Bir hata oluştu. Lütfen tekrar deneyin.');
        console.error('Login error:', error);
    } finally {
        setLoading('loginBtn', false);
    }
});

// Input formatting
document.getElementById('tcNo').addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').substring(0, 11);
    clearError('tcNoError');
});

document.getElementById('password').addEventListener('input', () => {
    clearError('passwordError');
});

// Clear any error messages when user starts typing
document.getElementById('tcNo').addEventListener('focus', () => {
    clearError('tcNoError');
    hideAlert('loginError');
});

document.getElementById('password').addEventListener('focus', () => {
    clearError('passwordError');
    hideAlert('loginError');
});
