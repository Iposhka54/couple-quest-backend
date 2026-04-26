import { getAccessToken, setAccessToken } from './storage';

const API_URL = '/api';

class ApiError extends Error {
  constructor(message, status, payload = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = getAccessToken();

  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers,
    credentials: 'include',
  });

  if (response.status === 401 && path !== '/auth/refresh') {
    const refreshed = await tryRefreshToken();
    if (refreshed) {
      return request(path, options);
    }
  }

  if (!response.ok) {
    let message = 'Произошла ошибка запроса';
    let payload = null;
    try {
      payload = await response.json();
      message = payload.message || payload.error || message;
    } catch {
      // ignore
    }
    throw new ApiError(message, response.status, payload);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export async function tryRefreshToken() {
  try {
    const response = await fetch(`${API_URL}/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    });

    if (!response.ok) {
      setAccessToken(null);
      return false;
    }

    const data = await response.json();
    setAccessToken(data.accessToken ?? null);
    return Boolean(data.accessToken);
  } catch {
    setAccessToken(null);
    return false;
  }
}

export const authApi = {
  signUp: (body) => request('/auth/signUp', { method: 'POST', body: JSON.stringify(body) }),
  signIn: (body) => request('/auth/signIn', { method: 'POST', body: JSON.stringify(body) }),
  verifyEmail: (body) => request('/auth/verify-email-code', { method: 'POST', body: JSON.stringify(body) }),
  resendCode: (body) => request('/auth/resend-email-code', { method: 'POST', body: JSON.stringify(body) }),
  me: () => request('/auth/me', { method: 'GET' }),
};

export const coupleApi = {
  getInvite: () => request('/couple/invite', { method: 'GET' }),
  createInvite: () => request('/couple/invite', { method: 'POST' }),
  acceptInvite: (token) => request(`/couple/invite/accept?invite=${encodeURIComponent(token)}`, { method: 'POST' }),
  revokeInvite: () => request('/couple/invite', { method: 'DELETE' }),
};

export const menuApi = {
  getMeals: (coupleId) => request(`/meals?coupleId=${encodeURIComponent(coupleId)}`, { method: 'GET' }),
  createMeal: (body) => request('/meals', { method: 'POST', body: JSON.stringify(body) }),
  updateMeal: (mealId, body) => request(`/meals/${mealId}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteMeal: (mealId, coupleId) => request(`/meals/${mealId}?coupleId=${encodeURIComponent(coupleId)}`, { method: 'DELETE' }),
  getWeek: (weekStart, coupleId) => request(`/menu/weeks/${weekStart}?coupleId=${encodeURIComponent(coupleId)}`, { method: 'GET' }),
  upsertEntry: (weekStart, body) => request(`/menu/weeks/${weekStart}/entries`, { method: 'POST', body: JSON.stringify(body) }),
  deleteEntry: (weekStart, entryId, coupleId) => request(
    `/menu/weeks/${weekStart}/entries/${entryId}?coupleId=${encodeURIComponent(coupleId)}`,
    { method: 'DELETE' },
  ),
};

export { ApiError };