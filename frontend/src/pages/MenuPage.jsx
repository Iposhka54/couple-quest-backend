const menuPlan = [
  { day: 'Понедельник', meal: 'Паста с курицей и овощами', extra: 'Рецепт на 25 минут' },
  { day: 'Вторник', meal: 'Сырники и ягодный соус', extra: 'Идеально для уютного завтрака' },
  { day: 'Среда', meal: 'Том ям домашний', extra: 'Совместное приготовление вечером' },
];

export function MenuPage() {
  return (
    <div className="stack-lg">
      <section className="panel">
        <h1>Меню и рецепты</h1>
        <p>
          Отдельный раздел для планирования питания: можно собрать меню на неделю,
          придумать рецепты и отметить любимые блюда девушки или парня.
        </p>
      </section>

      <section className="grid-3">
        {menuPlan.map((item) => (
          <article className="feature-card" key={item.day}>
            <span className="badge soft">{item.day}</span>
            <h3>{item.meal}</h3>
            <p>{item.extra}</p>
          </article>
        ))}
      </section>
    </div>
  );
}