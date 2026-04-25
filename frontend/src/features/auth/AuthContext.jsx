import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { authApi, tryRefreshToken } from './api';
import { getAccessToken, getPendingEmail, setAccessToken, setPendingEmail } from './storage';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pendingEmail, setPendingEmailState] = useState(getPendingEmail());

  useEffect(() => {
    bootstrapAuth();
  }, []);

  async function bootstrapAuth() {
    try {
      const token = getAccessToken();

      if (!token) {
        setIsLoading(false);
        return;
      }

      if (token) {
        const profile = await authApi.me();
        setUser(profile);
        setPendingEmail(null);
        setPendingEmailState(null);
      }
    } catch {
      setAccessToken(null);
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }

  async function signIn(credentials) {
    const response = await authApi.signIn(credentials);
    return handleAuthResponse(response);
  }

  async function signUp(payload) {
    const response = await authApi.signUp(payload);
    return handleAuthResponse(response);
  }

  async function verifyEmail(payload) {
    const response = await authApi.verifyEmail(payload);
    const result = handleAuthResponse(response);
    if (result.status === 'AUTHENTICATED') {
      const profile = await authApi.me();
      setUser(profile);
    }
    return result;
  }

  async function resendVerificationCode(email) {
    const response = await authApi.resendCode({ email });
    setPendingEmail(email);
    setPendingEmailState(email);
    return response;
  }

  function handleAuthResponse(response) {
    if (response.status === 'AUTHENTICATED') {
      setAccessToken(response.accessToken);
      setPendingEmail(null);
      setPendingEmailState(null);
      return response;
    }

    setAccessToken(null);
    setUser(null);
    setPendingEmail(response.email);
    setPendingEmailState(response.email);
    return response;
  }

  function logout() {
    setAccessToken(null);
    setPendingEmail(null);
    setPendingEmailState(null);
    setUser(null);
  }

  const value = useMemo(
    () => ({
      user,
      pendingEmail,
      isLoading,
      isAuthenticated: Boolean(getAccessToken()),
      isEmailVerified: Boolean(user?.emailVerified),
      signIn,
      signUp,
      verifyEmail,
      resendVerificationCode,
      logout,
      refreshProfile: bootstrapAuth,
    }),
    [user, pendingEmail, isLoading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}