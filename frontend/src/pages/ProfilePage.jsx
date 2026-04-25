import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import { ApiError, coupleApi } from '../features/auth/api';

const FRONTEND_ORIGIN = window.location.origin?.startsWith('http')
  ? window.location.origin
  : 'http://localhost';

function formatGender(gender) {
  return gender === 'MALE' ? 'Парень' : gender === 'FEMALE' ? 'Девушка' : gender;
}

function formatDateTime(value) {
  if (!value) {
    return '—';
  }

  return new Intl.DateTimeFormat('ru-RU', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

export function ProfilePage() {
  const { user, refreshProfile } = useAuth();
  const [searchParams] = useSearchParams();
  const inviteTokenFromUrl = searchParams.get('invite');

  const [invite, setInvite] = useState(null);
  const [inviteLoading, setInviteLoading] = useState(false);
  const [inviteActionLoading, setInviteActionLoading] = useState(false);
  const [inviteError, setInviteError] = useState('');
  const [inviteSuccess, setInviteSuccess] = useState('');

  const hasCouple = Boolean(user?.couple?.hasCouple);
  const partner = user?.couple?.partner;

  const inviteLink = useMemo(() => {
    if (!invite?.invite) {
      return '';
    }

    return `${FRONTEND_ORIGIN}/invite/${encodeURIComponent(invite.invite)}`;
  }, [invite]);

  useEffect(() => {
    if (!hasCouple) {
      loadInvite();
    } else {
      setInvite(null);
      setInviteError('');
    }
  }, [hasCouple]);

  async function loadInvite() {
    setInviteLoading(true);
    try {
      const response = await coupleApi.getInvite();
      setInvite(response);
      setInviteError('');
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setInvite(null);
        setInviteError('');
      } else {
        setInviteError(err.message);
      }
    } finally {
      setInviteLoading(false);
    }
  }

  async function handleCreateInvite() {
    setInviteActionLoading(true);
    setInviteError('');
    setInviteSuccess('');

    try {
      const response = await coupleApi.createInvite();
      setInvite(response);
      setInviteSuccess('Ссылка-приглашение готова. Можешь отправить её партнёру.');
    } catch (err) {
      setInviteError(err.message);
    } finally {
      setInviteActionLoading(false);
    }
  }

  async function handleRevokeInvite() {
    setInviteActionLoading(true);
    setInviteError('');
    setInviteSuccess('');

    try {
      await coupleApi.revokeInvite();
      setInvite(null);
      setInviteSuccess('Приглашение отозвано. При необходимости можно создать новое.');
    } catch (err) {
      setInviteError(err.message);
    } finally {
      setInviteActionLoading(false);
    }
  }

  async function handleCopyInvite() {
    if (!inviteLink) {
      return;
    }

    try {
      await navigator.clipboard.writeText(inviteLink);
      setInviteSuccess('Ссылка скопирована в буфер обмена.');
    } catch {
      setInviteSuccess('Не удалось скопировать автоматически. Скопируй ссылку вручную ниже.');
    }
  }

  async function handleAcceptInvite() {
    if (!inviteTokenFromUrl) {
      return;
    }

    setInviteActionLoading(true);
    setInviteError('');
    setInviteSuccess('');

    try {
      await coupleApi.acceptInvite(inviteTokenFromUrl);
      await refreshProfile();
      setInviteSuccess('Приглашение принято! Информация о вашей паре обновлена.');
    } catch (err) {
      setInviteError(err.message);
    } finally {
      setInviteActionLoading(false);
    }
  }

  return (
    <div className="stack-lg">
      <section className="panel gradient-panel">
        <span className="badge">Профиль</span>
        <h1>{user?.name}</h1>
        <p>
          Здесь собрана информация о тебе и вашей паре. Если пары пока нет — создай приглашение
          и отправь ссылку партнёру.
        </p>
      </section>

      <section className="profile-grid">
        <article className="panel stack-md">
          <div className="row-between">
            <h2 className="section-title">Личные данные</h2>
            <span className="status-pill">{user?.emailVerified ? 'Email подтвержден' : 'Email не подтвержден'}</span>
          </div>

          <div className="info-list">
            <div className="info-item">
              <span>Имя</span>
              <strong>{user?.name}</strong>
            </div>
            <div className="info-item">
              <span>Email</span>
              <strong>{user?.email}</strong>
            </div>
            <div className="info-item">
              <span>Пол</span>
              <strong>{formatGender(user?.gender)}</strong>
            </div>
          </div>
        </article>

        <article className="panel stack-md">
          <div className="row-between">
            <h2 className="section-title">Моя пара</h2>
            <span className={`status-pill ${hasCouple ? '' : 'soft'}`}>
              {hasCouple ? 'Пара активна' : 'Пара не создана'}
            </span>
          </div>

          {hasCouple ? (
            <div className="couple-card">
              <div className="couple-avatar">❤</div>
              <div>
                <strong>{partner?.name}</strong>
                <p>Партнёр: {formatGender(partner?.gender)}</p>
              </div>
            </div>
          ) : (
            <>
              <p className="muted-text">
                Когда партнёр примет приглашение, здесь появится информация о вашей паре.
              </p>

              {inviteTokenFromUrl ? (
                <div className="invite-box">
                  <span className="badge soft">Входящее приглашение</span>
                  <p>
                    Ты открыл(а) ссылку-приглашение. Нажми кнопку ниже, чтобы присоединиться к паре.
                  </p>
                  <button
                    className="primary-button"
                    type="button"
                    disabled={inviteActionLoading}
                    onClick={handleAcceptInvite}
                  >
                    {inviteActionLoading ? 'Принимаем приглашение...' : 'Принять приглашение'}
                  </button>
                </div>
              ) : null}

              {inviteLoading ? <div className="center-card">Загружаем приглашение...</div> : null}

              {!inviteLoading && invite ? (
                <div className="invite-box">
                  <span className="badge soft">Моё приглашение</span>
                  <div className="info-list compact">
                    <div className="info-item">
                      <span>Статус</span>
                      <strong>{invite.status}</strong>
                    </div>
                    <div className="info-item">
                      <span>Действует до</span>
                      <strong>{formatDateTime(invite.expiresAt)}</strong>
                    </div>
                  </div>
                  <label>
                    Ссылка для партнёра
                    <input type="text" readOnly value={inviteLink} />
                  </label>
                  <div className="button-row">
                    <button className="primary-button" type="button" onClick={handleCopyInvite}>
                      Скопировать ссылку
                    </button>
                    <button
                      className="ghost-button"
                      type="button"
                      disabled={inviteActionLoading}
                      onClick={handleRevokeInvite}
                    >
                      {inviteActionLoading ? 'Отзываем...' : 'Отозвать приглашение'}
                    </button>
                  </div>
                </div>
              ) : null}

              {!inviteLoading && !invite ? (
                <div className="invite-box">
                  <span className="badge soft">Создать приглашение</span>
                  <p>
                    Сгенерируй персональную ссылку — партнёр откроет её, авторизуется и сможет
                    принять приглашение.
                  </p>
                  <button
                    className="primary-button"
                    type="button"
                    disabled={inviteActionLoading}
                    onClick={handleCreateInvite}
                  >
                    {inviteActionLoading ? 'Создаём ссылку...' : 'Создать ссылку-приглашение'}
                  </button>
                </div>
              ) : null}
            </>
          )}

          {inviteError ? <div className="error-box">{inviteError}</div> : null}
          {inviteSuccess ? <div className="success-box">{inviteSuccess}</div> : null}
        </article>
      </section>
    </div>
  );
}