import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AuthCard } from '../components/AuthCard';
import { useAuth } from '../features/auth/AuthContext';

export function LoginPage() {
  const { signIn, refreshProfile } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      const result = await signIn(form);
      if (result.status === 'EMAIL_VERIFICATION_REQUIRED') {
        navigate('/verify-email');
        return;
      }

      await refreshProfile();
      navigate(location.state?.from || '/app');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthCard
      title="Вход"
      subtitle="Авторизуйтесь, чтобы открылись задания, магазин, меню и календарь."
      footer={<span>Нет аккаунта? <Link to="/register">Зарегистрироваться</Link></span>}
    >
      <form className="form" onSubmit={handleSubmit}>
        <label>
          Email
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            placeholder="love@example.com"
            required
          />
        </label>
        <label>
          Пароль
          <input
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            placeholder="Не меньше 8 символов"
            required
          />
        </label>

        {error ? <div className="error-box">{error}</div> : null}

        <button className="primary-button full-width" disabled={loading} type="submit">
          {loading ? 'Входим...' : 'Войти'}
        </button>
      </form>
    </AuthCard>
  );
}