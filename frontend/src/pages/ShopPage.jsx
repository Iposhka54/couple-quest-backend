const products = [
  { title: 'Кино-вечер по выбору', price: '350 ❤' },
  { title: 'Завтрак в постель', price: '500 ❤' },
  { title: 'Выходной без домашних дел', price: '700 ❤' },
];

export function ShopPage() {
  return (
    <div className="stack-lg">
      <section className="panel">
        <h1>Магазин наград</h1>
        <p>Тратьте заработанные сердечки на милые бонусы, подарки и особые привилегии внутри пары.</p>
      </section>

      <section className="grid-3">
        {products.map((product) => (
          <article className="feature-card" key={product.title}>
            <h3>{product.title}</h3>
            <p>Цена: {product.price}</p>
            <button className="primary-button">Купить</button>
          </article>
        ))}
      </section>
    </div>
  );
}