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
            // Store JWT token
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('tokenType', data.tokenType);
            localStorage.setItem('user', JSON.stringify(data.user));
            
            // Redirect to QR code page
            window.location.href = '/qrcode';
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
