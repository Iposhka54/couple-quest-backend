const calendarItems = [
  { date: 'Пн, 22', title: 'Составить список покупок', kind: 'Быт' },
  { date: 'Вт, 23', title: 'Свидание дома + новый рецепт', kind: 'Романтика' },
  { date: 'Сб, 27', title: 'Обновить меню на неделю', kind: 'Планирование' },
];

export function CalendarPage() {
  return (
    <div className="stack-lg">
      <section className="panel">
        <h1>Календарь пары</h1>
        <p>
          Здесь можно планировать совместные дела, меню, приготовление блюд и важные события.
          Это особенно удобно для отдельного женского раздела с меню и повседневной организацией.
        </p>
      </section>

      <section className="timeline">
        {calendarItems.map((item) => (
          <article className="timeline-item" key={item.date + item.title}>
            <div>
              <strong>{item.date}</strong>
              <span>{item.kind}</span>
            </div>
            <p>{item.title}</p>
          </article>
        ))}
      </section>
    </div>
  );
}