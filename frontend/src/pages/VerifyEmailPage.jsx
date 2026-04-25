import { useEffect, useMemo, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { AuthCard } from '../components/AuthCard';
import { useAuth } from '../features/auth/AuthContext';

export function VerifyEmailPage() {
  const { pendingEmail, verifyEmail, resendVerificationCode, refreshProfile, isEmailVerified } = useAuth();
  const navigate = useNavigate();
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (!cooldown) return undefined;
    const timer = setInterval(() => setCooldown((value) => Math.max(0, value - 1)), 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  const email = useMemo(() => pendingEmail, [pendingEmail]);

  if (isEmailVerified) {
    return <Navigate to="/app" replace />;
  }

  if (!email) {
    return <Navigate to="/login" replace />;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      await verifyEmail({ email, code });
      await refreshProfile();
      setSuccess('Email успешно подтвержден. Перенаправляем в кабинет...');
      setTimeout(() => navigate('/app'), 900);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleResend() {
    setError('');
    setSuccess('');
    setResendLoading(true);

    try {
      const response = await resendVerificationCode(email);
      setCooldown(response.resendAvailableInSeconds ?? 60);
      setSuccess('Новый код отправлен на email.');
    } catch (err) {
      setError(err.message);
    } finally {
      setResendLoading(false);
    }
  }

  return (
    <AuthCard
      title="Подтверждение email"
      subtitle={`Мы отправили код на ${email}. Пока email не подтвержден, доступны только главная и эта страница.`}
    >
      <form className="form" onSubmit={handleSubmit}>
        <label>
          Код подтверждения
          <input
            type="text"
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
            placeholder="Введите код из письма"
            required
          />
        </label>

        {error ? <div className="error-box">{error}</div> : null}
        {success ? <div className="success-box">{success}</div> : null}

        <button className="primary-button full-width" disabled={loading} type="submit">
          {loading ? 'Проверяем...' : 'Подтвердить email'}
        </button>
        <button
          className="ghost-button full-width"
          disabled={resendLoading || cooldown > 0}
          onClick={handleResend}
          type="button"
        >
          {cooldown > 0 ? `Повторно отправить через ${cooldown}с` : resendLoading ? 'Отправляем...' : 'Отправить код еще раз'}
        </button>
      </form>
    </AuthCard>
  );
}