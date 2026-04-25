import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import { coupleApi } from '../features/auth/api';

export function InvitePage() {
  const { token } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, isEmailVerified, refreshProfile, user } = useAuth();

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  async function handleAcceptInvite() {
    if (!token) {
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      await coupleApi.acceptInvite(token);
      await refreshProfile();
      setSuccess('Приглашение принято! Сейчас перенаправим тебя в профиль.');
      setTimeout(() => navigate('/profile', { replace: true }), 900);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  if (!token) {
    return <div className="center-card">Ссылка приглашения некорректна.</div>;
  }

  return (
    <div className="invite-page-shell">
      <section className="panel invite-landing stack-md">
        <span className="badge">Приглашение в пару</span>
        <h1>Тебя пригласили в Couple Quest ❤</h1>
        <p>
          Открой приглашение, авторизуйся и подтверди присоединение к паре. После принятия
          приглашения информация появится в вашем профиле.
        </p>

        {isAuthenticated ? (
          <>
            <div className="info-item">
              <span>Ты вошёл как</span>
              <strong>{user?.name ?? user?.email}</strong>
            </div>

            {!isEmailVerified ? (
              <div className="invite-box">
                <p>
                  Сначала нужно подтвердить email, а потом можно будет принять приглашение.
                </p>
                <Link className="primary-button" to="/verify-email">
                  Подтвердить email
                </Link>
              </div>
            ) : (
              <div className="invite-box">
                <p>Нажми кнопку ниже, чтобы присоединиться к паре по этой ссылке.</p>
                <button
                  className="primary-button"
                  type="button"
                  onClick={handleAcceptInvite}
                  disabled={loading}
                >
                  {loading ? 'Принимаем приглашение...' : 'Принять приглашение'}
                </button>
              </div>
            )}
          </>
        ) : (
          <div className="invite-box">
            <p>
              Чтобы принять приглашение, сначала войди в аккаунт или зарегистрируйся. После входа
              ты вернёшься на эту же страницу.
            </p>
            <div className="button-row">
              <Link className="primary-button" to={`/login`} state={{ from: `/invite/${token}` }}>
                Войти
              </Link>
              <Link className="ghost-button" to={`/register`}>
                Регистрация
              </Link>
            </div>
          </div>
        )}

        {error ? <div className="error-box">{error}</div> : null}
        {success ? <div className="success-box">{success}</div> : null}
      </section>
    </div>
  );
}