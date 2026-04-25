const ACCESS_TOKEN_KEY = 'couplequest.accessToken';
const PENDING_EMAIL_KEY = 'couplequest.pendingEmail';

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setAccessToken(token) {
  if (token) {
    localStorage.setItem(ACCESS_TOKEN_KEY, token);
  } else {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
  }
}

export function getPendingEmail() {
  return sessionStorage.getItem(PENDING_EMAIL_KEY);
}

export function setPendingEmail(email) {
  if (email) {
    sessionStorage.setItem(PENDING_EMAIL_KEY, email);
  } else {
    sessionStorage.removeItem(PENDING_EMAIL_KEY);
  }
}