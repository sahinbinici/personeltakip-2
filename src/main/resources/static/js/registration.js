// Registration state
let registrationState = {
    tcNo: '',
    personnelNo: '',
    mobilePhone: '',
    currentStep: 1
};

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

function goToStep(stepNumber) {
    // Hide all steps
    document.querySelectorAll('.form-step').forEach(step => {
        step.classList.remove('active');
    });
    
    // Show target step
    const targetStep = document.getElementById(`step${stepNumber}`);
    if (targetStep) {
        targetStep.classList.add('active');
        registrationState.currentStep = stepNumber;
    }
}

function showSuccessStep() {
    document.querySelectorAll('.form-step').forEach(step => {
        step.classList.remove('active');
    });
    document.getElementById('successStep').classList.add('active');
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

function validatePersonnelNo(personnelNo) {
    if (!personnelNo) {
        return 'Sicil No zorunludur';
    }
    if (personnelNo.length < 1 || personnelNo.length > 20) {
        return 'Sicil No 1-20 karakter arasında olmalıdır';
    }
    return null;
}

function validateOtpCode(otpCode) {
    if (!otpCode) {
        return 'Doğrulama kodu zorunludur';
    }
    if (!/^\d{6}$/.test(otpCode)) {
        return 'Doğrulama kodu 6 haneli olmalıdır';
    }
    return null;
}

function validatePassword(password) {
    const errors = [];
    
    if (!password) {
        return 'Şifre zorunludur';
    }
    
    if (password.length < 8) {
        errors.push('En az 8 karakter');
    }
    
    if (!/[A-Z]/.test(password)) {
        errors.push('En az bir büyük harf');
    }
    
    if (!/[a-z]/.test(password)) {
        errors.push('En az bir küçük harf');
    }
    
    if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
        errors.push('En az bir özel karakter');
    }
    
    if (errors.length > 0) {
        return 'Şifre gereksinimleri: ' + errors.join(', ');
    }
    
    return null;
}

function updatePasswordRequirements(password) {
    const requirements = {
        'req-length': password.length >= 8,
        'req-uppercase': /[A-Z]/.test(password),
        'req-lowercase': /[a-z]/.test(password),
        'req-special': /[!@#$%^&*(),.?":{}|<>]/.test(password)
    };
    
    Object.keys(requirements).forEach(reqId => {
        const element = document.getElementById(reqId);
        if (element) {
            if (requirements[reqId]) {
                element.classList.add('valid');
                element.querySelector('.icon').textContent = '✓';
            } else {
                element.classList.remove('valid');
                element.querySelector('.icon').textContent = '✗';
            }
        }
    });
}

// Step 1: Personnel Validation
document.getElementById('personnelValidationForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    // Clear previous errors
    clearError('tcNoError');
    clearError('personnelNoError');
    hideAlert('step1Error');
    hideAlert('step1Success');
    
    const tcNo = document.getElementById('tcNo').value.trim();
    const personnelNo = document.getElementById('personnelNo').value.trim();
    
    // Validate inputs
    const tcNoError = validateTcNo(tcNo);
    const personnelNoError = validatePersonnelNo(personnelNo);
    
    let hasError = false;
    
    if (tcNoError) {
        showError('tcNoError', tcNoError);
        document.getElementById('tcNo').classList.add('error');
        hasError = true;
    } else {
        document.getElementById('tcNo').classList.remove('error');
    }
    
    if (personnelNoError) {
        showError('personnelNoError', personnelNoError);
        document.getElementById('personnelNo').classList.add('error');
        hasError = true;
    } else {
        document.getElementById('personnelNo').classList.remove('error');
    }
    
    if (hasError) {
        return;
    }
    
    // Call API
    setLoading('validateBtn', true);
    
    try {
        const response = await fetch('/api/register/validate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ tcNo, personnelNo })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            // Save state
            registrationState.tcNo = tcNo;
            registrationState.personnelNo = personnelNo;
            registrationState.mobilePhone = data.mobilePhone;
            
            // Send OTP
            await sendOtp();
            
            // Show success and move to next step
            showAlert('step1Success', 'Personel bilgileri doğrulandı. SMS gönderiliyor...');
            
            setTimeout(() => {
                goToStep(2);
                document.getElementById('phoneDisplay').textContent = 
                    `Kod gönderildi: ${maskPhone(data.mobilePhone)}`;
            }, 1000);
        } else {
            showAlert('step1Error', data.message || 'Personel bilgileri doğrulanamadı');
        }
    } catch (error) {
        showAlert('step1Error', 'Bir hata oluştu. Lütfen tekrar deneyin.');
        console.error('Validation error:', error);
    } finally {
        setLoading('validateBtn', false);
    }
});

// Send OTP
async function sendOtp() {
    try {
        const response = await fetch('/api/register/send-otp', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                tcNo: registrationState.tcNo,
                mobilePhone: registrationState.mobilePhone
            })
        });
        
        if (!response.ok) {
            throw new Error('OTP gönderilemedi');
        }
    } catch (error) {
        console.error('OTP send error:', error);
        throw error;
    }
}

// Mask phone number
function maskPhone(phone) {
    if (!phone || phone.length < 4) return phone;
    return phone.substring(0, 3) + '****' + phone.substring(phone.length - 2);
}

// Step 2: OTP Verification
document.getElementById('otpVerificationForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    // Clear previous errors
    clearError('otpCodeError');
    hideAlert('step2Error');
    hideAlert('step2Success');
    
    const otpCode = document.getElementById('otpCode').value.trim();
    
    // Validate input
    const otpError = validateOtpCode(otpCode);
    
    if (otpError) {
        showError('otpCodeError', otpError);
        document.getElementById('otpCode').classList.add('error');
        return;
    } else {
        document.getElementById('otpCode').classList.remove('error');
    }
    
    // Call API
    setLoading('verifyOtpBtn', true);
    
    try {
        const response = await fetch('/api/register/verify-otp', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                tcNo: registrationState.tcNo,
                otpCode: otpCode
            })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            showAlert('step2Success', 'Doğrulama başarılı!');
            
            setTimeout(() => {
                goToStep(3);
            }, 1000);
        } else {
            showAlert('step2Error', data.message || 'Doğrulama kodu hatalı veya süresi dolmuş');
        }
    } catch (error) {
        showAlert('step2Error', 'Bir hata oluştu. Lütfen tekrar deneyin.');
        console.error('OTP verification error:', error);
    } finally {
        setLoading('verifyOtpBtn', false);
    }
});

// Resend OTP
document.getElementById('resendOtpBtn').addEventListener('click', async () => {
    hideAlert('step2Error');
    hideAlert('step2Success');
    
    try {
        await sendOtp();
        showAlert('step2Success', 'Doğrulama kodu tekrar gönderildi');
        
        setTimeout(() => {
            hideAlert('step2Success');
        }, 3000);
    } catch (error) {
        showAlert('step2Error', 'Kod gönderilemedi. Lütfen tekrar deneyin.');
    }
});

// Step 3: Password Creation
const passwordInput = document.getElementById('password');
passwordInput.addEventListener('input', (e) => {
    updatePasswordRequirements(e.target.value);
    clearError('passwordError');
    
    if (e.target.value) {
        const error = validatePassword(e.target.value);
        if (!error) {
            e.target.classList.remove('error');
            e.target.classList.add('success');
        } else {
            e.target.classList.remove('success');
        }
    }
});

document.getElementById('passwordCreationForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    // Clear previous errors
    clearError('passwordError');
    clearError('confirmPasswordError');
    hideAlert('step3Error');
    hideAlert('step3Success');
    
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    
    // Validate password
    const passwordError = validatePassword(password);
    
    let hasError = false;
    
    if (passwordError) {
        showError('passwordError', passwordError);
        document.getElementById('password').classList.add('error');
        hasError = true;
    } else {
        document.getElementById('password').classList.remove('error');
    }
    
    // Validate password confirmation
    if (!confirmPassword) {
        showError('confirmPasswordError', 'Şifre tekrarı zorunludur');
        document.getElementById('confirmPassword').classList.add('error');
        hasError = true;
    } else if (password !== confirmPassword) {
        showError('confirmPasswordError', 'Şifreler eşleşmiyor');
        document.getElementById('confirmPassword').classList.add('error');
        hasError = true;
    } else {
        document.getElementById('confirmPassword').classList.remove('error');
    }
    
    if (hasError) {
        return;
    }
    
    // Call API
    setLoading('completeBtn', true);
    
    try {
        const response = await fetch('/api/register/complete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                tcNo: registrationState.tcNo,
                password: password
            })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            showAlert('step3Success', 'Kayıt tamamlandı!');
            
            setTimeout(() => {
                showSuccessStep();
            }, 1000);
        } else {
            showAlert('step3Error', data.message || 'Kayıt tamamlanamadı');
        }
    } catch (error) {
        showAlert('step3Error', 'Bir hata oluştu. Lütfen tekrar deneyin.');
        console.error('Registration completion error:', error);
    } finally {
        setLoading('completeBtn', false);
    }
});

// Input formatting
document.getElementById('tcNo').addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').substring(0, 11);
    clearError('tcNoError');
});

document.getElementById('otpCode').addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').substring(0, 6);
    clearError('otpCodeError');
});

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    goToStep(1);
});
