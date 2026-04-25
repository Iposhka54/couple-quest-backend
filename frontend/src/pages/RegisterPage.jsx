import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthCard } from '../components/AuthCard';
import { useAuth } from '../features/auth/AuthContext';

export function RegisterPage() {
  const { signUp } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    email: '',
    password: '',
    name: '',
    gender: 'MALE',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      await signUp(form);
      navigate('/verify-email');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthCard
      title="Регистрация"
      subtitle="Создайте аккаунт и подтвердите email, чтобы открыть весь функционал Couple Quest."
      footer={<span>Уже зарегистрированы? <Link to="/login">Войти</Link></span>}
    >
      <form className="form" onSubmit={handleSubmit}>
        <label>
          Имя
          <input
            type="text"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            placeholder="Например, Саша"
            minLength={2}
            required
          />
        </label>
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
            minLength={8}
            required
          />
        </label>
        <label>
          Пол
          <select value={form.gender} onChange={(e) => setForm({ ...form, gender: e.target.value })}>
            <option value="MALE">Парень</option>
            <option value="FEMALE">Девушка</option>
          </select>
        </label>

        {error ? <div className="error-box">{error}</div> : null}

        <button className="primary-button full-width" disabled={loading} type="submit">
          {loading ? 'Создаем аккаунт...' : 'Зарегистрироваться'}
        </button>
      </form>
    </AuthCard>
  );
}