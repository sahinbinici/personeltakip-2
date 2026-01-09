// Check authentication on page load
function checkAuthentication() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        window.location.href = '/login';
        return false;
    }
    return true;
}

// Display user information
function displayUserInfo() {
    const userStr = localStorage.getItem('user');
    if (userStr) {
        try {
            const user = JSON.parse(userStr);
            document.getElementById('userName').textContent = `${user.firstName} ${user.lastName}`;
            document.getElementById('userTc').textContent = user.tcNo;
        } catch (e) {
            console.error('Error parsing user data:', e);
        }
    }
}

// Logout functionality
document.getElementById('logoutBtn').addEventListener('click', () => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('tokenType');
    localStorage.removeItem('user');
    window.location.href = '/login';
});

// Fetch user status
async function fetchUserStatus() {
    const token = localStorage.getItem('authToken');
    const tokenType = localStorage.getItem('tokenType') || 'Bearer';
    
    try {
        const response = await fetch('/api/mobil/durum', {
            method: 'GET',
            headers: {
                'Authorization': `${tokenType} ${token}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (response.status === 401) {
            // Token expired or invalid
            localStorage.removeItem('authToken');
            localStorage.removeItem('tokenType');
            localStorage.removeItem('user');
            window.location.href = '/login';
            return null;
        }
        
        if (!response.ok) {
            throw new Error('Kullanıcı durumu alınamadı');
        }
        
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('Error fetching user status:', error);
        throw error;
    }
}

// Display user status
function displayUserStatus(statusData) {
    const statusElement = document.getElementById('userStatus');
    const indicatorElement = document.getElementById('statusIndicator');
    const textElement = document.getElementById('statusText');
    
    if (statusData) {
        if (statusData.isInside) {
            statusElement.className = 'user-status inside';
            indicatorElement.textContent = '🟢';
            textElement.textContent = 'İçerisiniz';
        } else {
            statusElement.className = 'user-status outside';
            indicatorElement.textContent = '🔴';
            textElement.textContent = 'Dışarıdasınız';
        }
    } else {
        statusElement.className = 'user-status loading';
        indicatorElement.textContent = '⚠️';
        textElement.textContent = 'Durum alınamadı';
    }
}

// Load user status
async function loadUserStatus() {
    try {
        const statusData = await fetchUserStatus();
        displayUserStatus(statusData);
    } catch (error) {
        console.error('Error loading user status:', error);
        displayUserStatus(null);
    }
}

// Fetch QR code data
async function fetchQrCode() {
    const token = localStorage.getItem('authToken');
    const tokenType = localStorage.getItem('tokenType') || 'Bearer';
    
    try {
        const response = await fetch('/api/qrcode/daily', {
            method: 'GET',
            headers: {
                'Authorization': `${tokenType} ${token}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (response.status === 401) {
            // Token expired or invalid
            localStorage.removeItem('authToken');
            localStorage.removeItem('tokenType');
            localStorage.removeItem('user');
            window.location.href = '/login';
            return null;
        }
        
        if (!response.ok) {
            throw new Error('QR kod alınamadı');
        }
        
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('Error fetching QR code:', error);
        throw error;
    }
}

// Display QR code
function displayQrCode(qrCodeData) {
    // Hide loading, show content
    document.getElementById('loading').style.display = 'none';
    document.getElementById('qrcodeContent').style.display = 'block';
    document.getElementById('errorContainer').style.display = 'none';
    
    // Set QR code image
    const qrCodeImage = document.getElementById('qrcodeImage');
    const token = localStorage.getItem('authToken');
    const tokenType = localStorage.getItem('tokenType') || 'Bearer';
    
    // Construct image URL with authorization
    const imageUrl = `/api/qrcode/image?qrCodeValue=${encodeURIComponent(qrCodeData.qrCodeValue)}`;
    
    // Fetch image with authorization header
    fetch(imageUrl, {
        headers: {
            'Authorization': `${tokenType} ${token}`
        }
    })
    .then(response => response.blob())
    .then(blob => {
        const objectUrl = URL.createObjectURL(blob);
        qrCodeImage.src = objectUrl;
    })
    .catch(error => {
        console.error('Error loading QR code image:', error);
        qrCodeImage.alt = 'QR kod yüklenemedi';
    });
    
    // Set validity date
    const validDate = new Date(qrCodeData.validDate);
    const formattedDate = validDate.toLocaleDateString('tr-TR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });
    document.getElementById('validDate').textContent = formattedDate;
    
    // Set usage count
    document.getElementById('usageCount').textContent = qrCodeData.usageCount;
    document.getElementById('maxUsage').textContent = qrCodeData.maxUsage;
    
    // Update usage bar
    const usagePercentage = (qrCodeData.usageCount / qrCodeData.maxUsage) * 100;
    document.getElementById('usageBarFill').style.width = `${usagePercentage}%`;
    
    // Update status and usage text
    const statusElement = document.getElementById('status');
    const usageTextElement = document.getElementById('usageText');
    
    if (qrCodeData.usageCount === 0) {
        statusElement.textContent = 'Aktif';
        statusElement.className = 'info-value status active';
        usageTextElement.textContent = 'Giriş ve çıkış için kullanılabilir';
    } else if (qrCodeData.usageCount === 1) {
        statusElement.textContent = 'Kısmen Kullanıldı';
        statusElement.className = 'info-value status used';
        usageTextElement.textContent = 'Çıkış için kullanılabilir';
    } else {
        statusElement.textContent = 'Kullanım Tamamlandı';
        statusElement.className = 'info-value status expired';
        usageTextElement.textContent = 'Günlük kullanım limiti doldu';
    }
}

// Show error
function showError(message) {
    document.getElementById('loading').style.display = 'none';
    document.getElementById('qrcodeContent').style.display = 'none';
    document.getElementById('errorContainer').style.display = 'block';
    document.getElementById('errorMessage').textContent = message;
}

// Load QR code
async function loadQrCode() {
    try {
        document.getElementById('loading').style.display = 'block';
        document.getElementById('qrcodeContent').style.display = 'none';
        document.getElementById('errorContainer').style.display = 'none';
        
        const qrCodeData = await fetchQrCode();
        if (qrCodeData) {
            displayQrCode(qrCodeData);
        }
    } catch (error) {
        showError('QR kod yüklenirken bir hata oluştu. Lütfen tekrar deneyin.');
    }
}

// Refresh button handler
document.getElementById('refreshBtn').addEventListener('click', async () => {
    const button = document.getElementById('refreshBtn');
    const btnText = button.querySelector('.btn-text');
    const spinner = button.querySelector('.spinner-small');
    
    button.disabled = true;
    btnText.style.display = 'none';
    spinner.style.display = 'inline-block';
    
    try {
        await Promise.all([loadQrCode(), loadUserStatus()]);
    } finally {
        button.disabled = false;
        btnText.style.display = 'inline';
        spinner.style.display = 'none';
    }
});

// Retry button handler
document.getElementById('retryBtn').addEventListener('click', () => {
    loadQrCode();
    loadUserStatus();
});

// Check if it's a new day and refresh QR code
function checkForNewDay() {
    const lastCheckDate = localStorage.getItem('lastQrCodeDate');
    const today = new Date().toDateString();
    
    if (lastCheckDate !== today) {
        localStorage.setItem('lastQrCodeDate', today);
        loadQrCode();
        loadUserStatus();
    }
}

// Auto-refresh at midnight
function scheduleNextDayRefresh() {
    const now = new Date();
    const tomorrow = new Date(now);
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(0, 0, 1, 0); // 00:00:01 next day
    
    const timeUntilMidnight = tomorrow.getTime() - now.getTime();
    
    setTimeout(() => {
        loadQrCode();
        loadUserStatus();
        // Schedule next refresh
        scheduleNextDayRefresh();
    }, timeUntilMidnight);
}

// Periodic check for new day (every 5 minutes)
function startPeriodicCheck() {
    setInterval(() => {
        checkForNewDay();
    }, 5 * 60 * 1000); // Check every 5 minutes
}

// Periodic status update (every 30 seconds)
function startStatusUpdate() {
    setInterval(() => {
        loadUserStatus();
    }, 30 * 1000); // Update status every 30 seconds
}

// Initialize page
if (checkAuthentication()) {
    displayUserInfo();
    loadUserStatus();
    loadQrCode();
    scheduleNextDayRefresh();
    startPeriodicCheck();
    startStatusUpdate();
}
