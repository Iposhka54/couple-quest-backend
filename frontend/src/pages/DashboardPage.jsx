import { useAuth } from '../features/auth/AuthContext';

const metrics = [
  { label: 'Баланс пары', value: '1 280 ❤', hint: 'Сердечки за задания и привычки' },
  { label: 'Активные задания', value: '7', hint: '3 романтических и 4 бытовых' },
  { label: 'Запланированные блюда', value: '12', hint: 'На эту и следующую неделю' },
];

export function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="stack-lg">
      <section className="panel gradient-panel">
        <span className="badge">Личный кабинет</span>
        <h1>Привет, {user?.name}!</h1>
        <p>
          Здесь начинается ваша игровая жизнь как пары: задания, награды, магазин желаний,
          семейное меню и календарь заботы друг о друге.
        </p>
      </section>

      <section className="grid-3">
        {metrics.map((item) => (
          <article className="metric-card" key={item.label}>
            <span>{item.label}</span>
            <strong>{item.value}</strong>
            <small>{item.hint}</small>
          </article>
        ))}
      </section>
    </div>
  );
}