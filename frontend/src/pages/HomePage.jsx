import { Link } from 'react-router-dom';

const highlights = [
  {
    title: 'Парные задания',
    text: 'Создавайте друг другу квесты, романтические челленджи и ежедневные мини-цели с наградой за выполнение.',
  },
  {
    title: 'Общая валюта и магазин',
    text: 'Копите сердечки за выполненные задания и тратьте их на подарки, свидания, бонусы и маленькие радости.',
  },
  {
    title: 'Меню, рецепты и календарь',
    text: 'Планируйте питание, добавляйте любимые рецепты и собирайте удобное расписание на неделю.',
  },
];

export function HomePage() {
  return (
    <div className="stack-lg">
      <section className="hero">
        <div>
          <span className="badge">Приложение для пар</span>
          <h1>Couple Quest — маленькая RPG для ваших отношений</h1>
          <p>
            Парень и девушка создают задания друг для друга, получают валюту за выполненные цели,
            собирают меню, рецепты и вместе планируют уютную жизнь.
          </p>
          <div className="hero-actions">
            <Link to="/register" className="primary-button">Начать вместе</Link>
            <Link to="/login" className="ghost-button">У меня уже есть аккаунт</Link>
          </div>
        </div>

        <div className="hero-card">
          <h3>Для чего Couple Quest</h3>
          <ul>
            <li>Создавать задания и милые челленджи друг для друга</li>
            <li>Получать награды за заботу, быт и совместные цели</li>
            <li>Копить общую валюту и тратить ее в магазине пары</li>
            <li>Планировать меню, рецепты и важные события</li>
            <li>Делать повседневную жизнь теплее и интереснее</li>
          </ul>
        </div>
      </section>

      <section className="grid-3">
        {highlights.map((item) => (
          <article key={item.title} className="feature-card">
            <h3>{item.title}</h3>
            <p>{item.text}</p>
          </article>
        ))}
      </section>
    </div>
  );
}