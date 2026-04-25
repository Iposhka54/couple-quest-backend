import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';

const THEME_COOKIE = 'couplequest_theme';

function getCookie(name) {
  return document.cookie
    .split('; ')
    .find((item) => item.startsWith(`${name}=`))
    ?.split('=')[1];
}

function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  document.cookie = `${THEME_COOKIE}=${theme}; path=/; max-age=315360000; SameSite=Lax`;
}

export function MainLayout() {
  const { isAuthenticated, user, logout, isEmailVerified } = useAuth();
  const [theme, setTheme] = useState(() => getCookie(THEME_COOKIE) ?? 'dark');

  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  function toggleTheme() {
    setTheme((current) => (current === 'dark' ? 'light' : 'dark'));
  }

  return (
    <div className="page-shell">
      <header className="topbar">
        <Link to="/" className="brand">
          <span className="brand-mark">❤</span>
          Couple Quest
        </Link>

        <nav className="nav">
          <NavLink to="/">Главная</NavLink>
          {isAuthenticated && isEmailVerified && <NavLink to="/app">Кабинет</NavLink>}
          {isAuthenticated && isEmailVerified && <NavLink to="/tasks">Задания</NavLink>}
          {isAuthenticated && isEmailVerified && <NavLink to="/shop">Магазин</NavLink>}
          {isAuthenticated && isEmailVerified && <NavLink to="/menu">Меню</NavLink>}
          {isAuthenticated && isEmailVerified && <NavLink to="/calendar">Календарь</NavLink>}
        </nav>

        <div className="topbar-actions">
          <button
            className={`theme-toggle ${theme === 'light' ? 'light' : 'dark'}`}
            onClick={toggleTheme}
            type="button"
            aria-label="Переключить тему"
            title="Переключить тему"
          >
            <span className="theme-toggle-track">
              <span className="theme-toggle-thumb">
                <span className="theme-icon sun">☀</span>
                <span className="theme-icon moon">☾</span>
              </span>
            </span>
          </button>
          {isAuthenticated ? (
            <>
              <div className="user-chip">
                <strong>{user?.name ?? user?.email}</strong>
                <span>{isEmailVerified ? 'email подтвержден' : 'нужно подтвердить email'}</span>
              </div>
              <button className="ghost-button" onClick={logout}>Выйти</button>
            </>
          ) : (
            <>
              <Link to="/login" className="ghost-button">Вход</Link>
              <Link to="/register" className="primary-button">Регистрация</Link>
            </>
          )}
        </div>
      </header>

      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}