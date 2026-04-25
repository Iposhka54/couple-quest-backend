const tasks = [
  { title: 'Приготовить любимый завтрак', reward: '80 ❤', assignee: 'Для парня', status: 'В процессе' },
  { title: 'Организовать свидание-сюрприз', reward: '200 ❤', assignee: 'Для девушки', status: 'Новое' },
  { title: 'Совместная прогулка 10 000 шагов', reward: '120 ❤', assignee: 'Совместное', status: 'Сегодня' },
];

export function TasksPage() {
  return (
    <div className="stack-lg">
      <section className="panel">
        <h1>Задания друг для друга</h1>
        <p>
          В этой демо-странице показано, как могут выглядеть квесты для пары: романтика, быт,
          забота и совместные челленджи с наградой в сердечках.
        </p>
      </section>

      <section className="grid-3">
        {tasks.map((task) => (
          <article key={task.title} className="feature-card">
            <div className="row-between">
              <span className="badge soft">{task.assignee}</span>
              <span className="status-pill">{task.status}</span>
            </div>
            <h3>{task.title}</h3>
            <p>Награда: {task.reward}</p>
          </article>
        ))}
      </section>
    </div>
  );
}