// FoodExpress API Client & Frontend Utilities
const API_BASE = window.location.protocol.startsWith('http') ? `${window.location.origin}/api` : 'http://localhost:8085/api';

const Auth = {
    getToken() {
        return localStorage.getItem('foodexpress_token');
    },
    setToken(token) {
        localStorage.setItem('foodexpress_token', token);
    },
    getUser() {
        const u = localStorage.getItem('foodexpress_user');
        return u ? JSON.parse(u) : null;
    },
    setUser(user) {
        localStorage.setItem('foodexpress_user', JSON.stringify(user));
    },
    logout() {
        localStorage.removeItem('foodexpress_token');
        localStorage.removeItem('foodexpress_user');
        window.location.href = '/login.html';
    },
    isLoggedIn() {
        return !!this.getToken();
    },
    hasRole(role) {
        const u = this.getUser();
        return u && u.role === role;
    }
};

async function apiCall(endpoint, method = 'GET', data = null) {
    const headers = {
        'Content-Type': 'application/json'
    };
    const token = Auth.getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const config = {
        method,
        headers
    };

    if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
        config.body = JSON.stringify(data);
    }

    try {
        const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint}`;
        const res = await fetch(url, config);

        if (res.status === 401 && !endpoint.includes('/auth/login')) {
            showToast('Session expired. Please log in again.', 'warning');
            setTimeout(() => {
                Auth.logout();
            }, 1500);
            throw new Error('Unauthorized');
        }

        const json = await res.json();
        if (!res.ok || json.success === false) {
            throw new Error(json.message || 'Request failed');
        }
        return json.data !== undefined ? json.data : json;
    } catch (err) {
        showToast(err.message, 'danger');
        throw err;
    }
}

function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `alert alert-${type} alert-dismissible fade show shadow-sm`;
    toast.style.minWidth = '280px';
    toast.innerHTML = `
        <strong>${type === 'danger' ? 'Error: ' : ''}</strong>${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    container.appendChild(toast);

    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 200);
    }, 4000);
}

function renderNavbar() {
    const nav = document.getElementById('main-nav');
    if (!nav) return;

    const user = Auth.getUser();
    let userHtml = `
        <li class="nav-item">
            <a class="nav-link" href="/login.html"><i class="fas fa-sign-in-alt me-1"></i>Login</a>
        </li>
        <li class="nav-item">
            <a class="btn btn-primary btn-sm ms-2" href="/register.html">Sign Up</a>
        </li>
    `;

    if (user) {
        let dashboardLink = '';
        if (user.role === 'ROLE_SYSTEM_ADMIN') {
            dashboardLink = `<a class="dropdown-item" href="/admin/dashboard.html"><i class="fas fa-chart-line me-2"></i>Admin Dashboard</a>`;
        } else if (user.role === 'ROLE_RESTAURANT_ADMIN') {
            dashboardLink = `<a class="dropdown-item" href="/restaurant/dashboard.html"><i class="fas fa-store me-2"></i>Restaurant Dashboard</a>`;
        } else if (user.role === 'ROLE_DELIVERY_PARTNER') {
            dashboardLink = `<a class="dropdown-item" href="/delivery/dashboard.html"><i class="fas fa-motorcycle me-2"></i>Delivery Dashboard</a>`;
        }

        userHtml = `
            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle text-dark font-weight-bold" href="#" data-bs-toggle="dropdown">
                    <i class="fas fa-user-circle me-1 text-primary"></i> ${user.fullName}
                </a>
                <ul class="dropdown-menu dropdown-menu-end shadow">
                    <li><h6 class="dropdown-header">${user.role.replace('ROLE_', '')}</h6></li>
                    ${dashboardLink}
                    <li><a class="dropdown-item" href="/orders.html"><i class="fas fa-receipt me-2"></i>My Orders</a></li>
                    <li><a class="dropdown-item" href="/profile.html"><i class="fas fa-user-cog me-2"></i>My Profile</a></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><a class="dropdown-item text-danger" href="javascript:void(0)" onclick="Auth.logout()"><i class="fas fa-power-off me-2"></i>Logout</a></li>
                </ul>
            </li>
        `;
    }

    nav.innerHTML = `
        <div class="container">
            <a class="navbar-brand" href="/index.html">
                <i class="fas fa-utensils text-danger"></i> FoodExpress
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navContent">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navContent">
                <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                    <li class="nav-item">
                        <a class="nav-link" href="/index.html"><i class="fas fa-home me-1"></i>Home</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/restaurants.html"><i class="fas fa-search me-1"></i>Restaurants</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/swagger-ui.html" target="_blank"><i class="fas fa-book me-1"></i>API Docs</a>
                    </li>
                </ul>
                <ul class="navbar-nav align-items-center">
                    <li class="nav-item me-2">
                        <a class="nav-link position-relative" href="/cart.html">
                            <i class="fas fa-shopping-cart fa-lg"></i>
                            <span id="cart-badge" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="display:none;">
                                0
                            </span>
                        </a>
                    </li>
                    ${userHtml}
                </ul>
            </div>
        </div>
    `;

    updateCartBadge();
}

async function updateCartBadge() {
    if (!Auth.isLoggedIn()) return;
    try {
        const cart = await apiCall('/cart');
        const badge = document.getElementById('cart-badge');
        if (badge && cart && cart.items) {
            const count = cart.items.reduce((acc, item) => acc + item.quantity, 0);
            if (count > 0) {
                badge.textContent = count;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        }
    } catch (e) {
        // Silently ignore cart count error if unauthenticated
    }
}

document.addEventListener('DOMContentLoaded', () => {
    renderNavbar();
});
