import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../features/auth/AuthContext';
import { ApiError, menuApi } from '../features/auth/api';

const MEAL_TYPES = [
  { value: 'BREAKFAST', label: 'Завтрак' },
  { value: 'LUNCH', label: 'Обед' },
  { value: 'DINNER', label: 'Ужин' },
  { value: 'SNACK', label: 'Перекус' },
];

const DAY_LABELS = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье'];

function getWeekStart(date = new Date()) {
  const local = new Date(date);
  const day = local.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  local.setDate(local.getDate() + diff);
  local.setHours(0, 0, 0, 0);
  return local;
}

function toInputDate(date) {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

function parseApiError(error) {
  if (!(error instanceof ApiError)) {
    return 'Что-то пошло не так';
  }

  if (Array.isArray(error.payload?.message)) {
    return error.payload.message.join(', ');
  }

  return error.message;
}

function formatWeekRange(weekStart) {
  const weekEnd = new Date(weekStart);
  weekEnd.setDate(weekStart.getDate() + 6);

  const formatter = new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long' });
  return `${formatter.format(weekStart)} — ${formatter.format(weekEnd)}`;
}

function emptyMealForm(coupleId) {
  return {
    id: null,
    coupleId,
    name: '',
    description: '',
    portionSize: '',
    calories: '',
    proteins: '',
    fats: '',
    carbs: '',
    ingredients: [{ name: '', amount: '', unit: 'г' }],
  };
}

export function MenuPage() {
  const { user } = useAuth();
  const coupleId = user?.couple?.coupleId;

  const [weekStart, setWeekStart] = useState(() => getWeekStart());
  const [meals, setMeals] = useState([]);
  const [weekData, setWeekData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [savingMeal, setSavingMeal] = useState(false);
  const [savingEntry, setSavingEntry] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [mealForm, setMealForm] = useState(() => emptyMealForm(coupleId));
  const [entryForm, setEntryForm] = useState({
    plannedDate: toInputDate(getWeekStart()),
    mealType: 'BREAKFAST',
    mealId: '',
    servings: 2,
    note: '',
  });

  useEffect(() => {
    setMealForm((current) => ({ ...current, coupleId }));
  }, [coupleId]);

  useEffect(() => {
    setEntryForm((current) => ({ ...current, plannedDate: toInputDate(weekStart) }));
  }, [weekStart]);

  useEffect(() => {
    if (!coupleId) {
      setLoading(false);
      return;
    }

    loadData();
  }, [coupleId, weekStart]);

  async function loadData() {
    setLoading(true);
    setError('');
    try {
      const [mealsResponse, weekResponse] = await Promise.all([
        menuApi.getMeals(coupleId),
        menuApi.getWeek(toInputDate(weekStart), coupleId),
      ]);
      setMeals(mealsResponse);
      setWeekData(weekResponse);
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setLoading(false);
    }
  }

  const weekDays = useMemo(
    () => DAY_LABELS.map((label, index) => {
      const date = new Date(weekStart);
      date.setDate(weekStart.getDate() + index);
      return {
        label,
        date: toInputDate(date),
      };
    }),
    [weekStart],
  );

  const entriesMap = useMemo(() => {
    const map = new Map();
    for (const entry of weekData?.entries ?? []) {
      map.set(`${entry.plannedDate}_${entry.mealType}`, entry);
    }
    return map;
  }, [weekData]);

  function updateMealField(field, value) {
    setMealForm((current) => ({ ...current, [field]: value }));
  }

  function updateIngredient(index, field, value) {
    setMealForm((current) => ({
      ...current,
      ingredients: current.ingredients.map((ingredient, ingredientIndex) => (
        ingredientIndex === index ? { ...ingredient, [field]: value } : ingredient
      )),
    }));
  }

  function addIngredient() {
    setMealForm((current) => ({
      ...current,
      ingredients: [...current.ingredients, { name: '', amount: '', unit: 'г' }],
    }));
  }

  function removeIngredient(index) {
    setMealForm((current) => ({
      ...current,
      ingredients: current.ingredients.length === 1
        ? current.ingredients
        : current.ingredients.filter((_, ingredientIndex) => ingredientIndex !== index),
    }));
  }

  async function handleMealSubmit(event) {
    event.preventDefault();
    setSavingMeal(true);
    setError('');
    setSuccess('');

    const payload = {
      ...mealForm,
      coupleId,
      calories: Number(mealForm.calories),
      proteins: Number(mealForm.proteins),
      fats: Number(mealForm.fats),
      carbs: Number(mealForm.carbs),
      ingredients: mealForm.ingredients.map((ingredient) => ({
        ...ingredient,
        amount: Number(ingredient.amount),
      })),
    };

    try {
      if (mealForm.id) {
        await menuApi.updateMeal(mealForm.id, payload);
        setSuccess('Блюдо обновлено');
      } else {
        await menuApi.createMeal(payload);
        setSuccess('Блюдо создано');
      }

      setMealForm(emptyMealForm(coupleId));
      await loadData();
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setSavingMeal(false);
    }
  }

  function handleEditMeal(meal) {
    setMealForm({
      id: meal.id,
      coupleId,
      name: meal.name,
      description: meal.description ?? '',
      portionSize: meal.portionSize,
      calories: meal.calories,
      proteins: meal.proteins,
      fats: meal.fats,
      carbs: meal.carbs,
      ingredients: meal.ingredients.map((ingredient) => ({
        name: ingredient.name,
        amount: ingredient.amount,
        unit: ingredient.unit,
      })),
    });
  }

  async function handleDeleteMeal(mealId) {
    if (!window.confirm('Удалить блюдо? Все связанные записи в меню недели тоже удалятся.')) {
      return;
    }

    try {
      await menuApi.deleteMeal(mealId, coupleId);
      setSuccess('Блюдо удалено');
      if (mealForm.id === mealId) {
        setMealForm(emptyMealForm(coupleId));
      }
      await loadData();
    } catch (err) {
      setError(parseApiError(err));
    }
  }

  async function handleEntrySubmit(event) {
    event.preventDefault();
    setSavingEntry(true);
    setError('');
    setSuccess('');

    try {
      await menuApi.upsertEntry(toInputDate(weekStart), {
        coupleId,
        entry: {
          plannedDate: entryForm.plannedDate,
          mealType: entryForm.mealType,
          mealId: Number(entryForm.mealId),
          servings: Number(entryForm.servings),
          note: entryForm.note,
        },
      });
      setSuccess('Блюдо добавлено в недельное меню');
      await loadData();
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setSavingEntry(false);
    }
  }

  async function handleDeleteEntry(entry) {
    try {
      await menuApi.deleteEntry(toInputDate(weekStart), entry.id, coupleId);
      setSuccess('Запись удалена из недельного меню');
      await loadData();
    } catch (err) {
      setError(parseApiError(err));
    }
  }

  if (!coupleId) {
    return (
      <div className="stack-lg">
        <section className="panel">
          <h1>Меню на неделю</h1>
          <p>Сначала нужно создать пару в профиле, чтобы планировать блюда и общую закупку продуктов.</p>
        </section>
      </div>
    );
  }

  return (
    <div className="stack-lg">
      <section className="panel gradient-panel">
        <span className="badge">Меню пары</span>
        <h1>Планирование питания по неделям</h1>
        <p>
          Создавайте блюда с КБЖУ и ингредиентами, раскладывайте их по дням недели и сразу смотрите,
          какие продукты нужно купить на всю неделю.
        </p>
      </section>

      {error ? <div className="error-box">{error}</div> : null}
      {success ? <div className="success-box">{success}</div> : null}

      <section className="menu-toolbar panel">
        <div>
          <span className="badge soft">Неделя</span>
          <h2 className="section-title">{formatWeekRange(weekStart)}</h2>
        </div>
        <div className="button-row">
          <button className="ghost-button" type="button" onClick={() => setWeekStart((current) => {
            const next = new Date(current);
            next.setDate(current.getDate() - 7);
            return next;
          })}>
            ← Предыдущая
          </button>
          <button className="ghost-button" type="button" onClick={() => setWeekStart(getWeekStart())}>
            Текущая неделя
          </button>
          <button className="ghost-button" type="button" onClick={() => setWeekStart((current) => {
            const next = new Date(current);
            next.setDate(current.getDate() + 7);
            return next;
          })}>
            Следующая →
          </button>
        </div>
      </section>

      <section className="menu-layout">
        <div className="stack-lg">
          <article className="panel stack-md">
            <div className="row-between">
              <h2 className="section-title">Добавить блюдо в неделю</h2>
              <span className="status-pill">{weekData?.entries?.length ?? 0} записей</span>
            </div>

            <form className="form menu-form" onSubmit={handleEntrySubmit}>
              <div className="form-grid form-grid-4">
                <label>
                  День
                  <select value={entryForm.plannedDate} onChange={(event) => setEntryForm((current) => ({ ...current, plannedDate: event.target.value }))}>
                    {weekDays.map((day) => (
                      <option key={day.date} value={day.date}>{day.label}</option>
                    ))}
                  </select>
                </label>
                <label>
                  Приём пищи
                  <select value={entryForm.mealType} onChange={(event) => setEntryForm((current) => ({ ...current, mealType: event.target.value }))}>
                    {MEAL_TYPES.map((type) => (
                      <option key={type.value} value={type.value}>{type.label}</option>
                    ))}
                  </select>
                </label>
                <label>
                  Блюдо
                  <select value={entryForm.mealId} onChange={(event) => setEntryForm((current) => ({ ...current, mealId: event.target.value }))} required>
                    <option value="">Выбрать блюдо</option>
                    {meals.map((meal) => (
                      <option key={meal.id} value={meal.id}>{meal.name}</option>
                    ))}
                  </select>
                </label>
                <label>
                  Порций
                  <input type="number" min="1" max="20" value={entryForm.servings} onChange={(event) => setEntryForm((current) => ({ ...current, servings: event.target.value }))} required />
                </label>
              </div>

              <label>
                Заметка
                <input value={entryForm.note} onChange={(event) => setEntryForm((current) => ({ ...current, note: event.target.value }))} placeholder="Например: приготовить вечером вместе" />
              </label>

              <button className="primary-button" type="submit" disabled={savingEntry || meals.length === 0}>
                {savingEntry ? 'Сохраняем...' : 'Добавить в неделю'}
              </button>
            </form>

            {loading ? <div className="center-card">Загружаем недельное меню...</div> : null}

            <div className="week-grid">
              {weekDays.map((day) => (
                <article className="week-day-card" key={day.date}>
                  <div className="row-between">
                    <strong>{day.label}</strong>
                    <span className="muted-text">{day.date.slice(8, 10)}.{day.date.slice(5, 7)}</span>
                  </div>

                  <div className="stack-md">
                    {MEAL_TYPES.map((type) => {
                      const entry = entriesMap.get(`${day.date}_${type.value}`);
                      return (
                        <div className="planner-slot" key={type.value}>
                          <span className="planner-slot-title">{type.label}</span>
                          {entry ? (
                            <div className="planner-entry">
                              <strong>{entry.meal.name}</strong>
                              <small>{entry.servings} порц. · {entry.meal.portionSize}</small>
                              {entry.note ? <p>{entry.note}</p> : null}
                              <button className="ghost-button small-button" type="button" onClick={() => handleDeleteEntry(entry)}>
                                Удалить
                              </button>
                            </div>
                          ) : (
                            <div className="planner-empty">Не запланировано</div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </article>
              ))}
            </div>
          </article>

          <article className="panel stack-md">
            <div className="row-between">
              <h2 className="section-title">База блюд пары</h2>
              <span className="status-pill">{meals.length} блюд</span>
            </div>

            <form className="form menu-form" onSubmit={handleMealSubmit}>
              <div className="form-grid form-grid-2">
                <label>
                  Название блюда
                  <input value={mealForm.name} onChange={(event) => updateMealField('name', event.target.value)} required />
                </label>
                <label>
                  Размер порции
                  <input value={mealForm.portionSize} onChange={(event) => updateMealField('portionSize', event.target.value)} placeholder="Например: 350 г / 1 тарелка" required />
                </label>
              </div>

              <label>
                Описание
                <input value={mealForm.description} onChange={(event) => updateMealField('description', event.target.value)} placeholder="Короткое описание блюда" />
              </label>

              <div className="form-grid form-grid-4">
                <label>
                  Ккал
                  <input type="number" step="0.01" value={mealForm.calories} onChange={(event) => updateMealField('calories', event.target.value)} required />
                </label>
                <label>
                  Белки
                  <input type="number" step="0.01" value={mealForm.proteins} onChange={(event) => updateMealField('proteins', event.target.value)} required />
                </label>
                <label>
                  Жиры
                  <input type="number" step="0.01" value={mealForm.fats} onChange={(event) => updateMealField('fats', event.target.value)} required />
                </label>
                <label>
                  Углеводы
                  <input type="number" step="0.01" value={mealForm.carbs} onChange={(event) => updateMealField('carbs', event.target.value)} required />
                </label>
              </div>

              <div className="stack-md">
                <div className="row-between">
                  <h3 className="section-title">Ингредиенты</h3>
                  <button className="ghost-button" type="button" onClick={addIngredient}>+ Добавить ингредиент</button>
                </div>
                {mealForm.ingredients.map((ingredient, index) => (
                  <div className="ingredient-row" key={`${index}_${ingredient.name}`}>
                    <input placeholder="Название" value={ingredient.name} onChange={(event) => updateIngredient(index, 'name', event.target.value)} required />
                    <input type="number" step="0.001" placeholder="Количество" value={ingredient.amount} onChange={(event) => updateIngredient(index, 'amount', event.target.value)} required />
                    <input placeholder="Ед. изм." value={ingredient.unit} onChange={(event) => updateIngredient(index, 'unit', event.target.value)} required />
                    <button className="ghost-button" type="button" onClick={() => removeIngredient(index)} disabled={mealForm.ingredients.length === 1}>
                      Убрать
                    </button>
                  </div>
                ))}
              </div>

              <div className="button-row">
                <button className="primary-button" type="submit" disabled={savingMeal}>
                  {savingMeal ? 'Сохраняем...' : mealForm.id ? 'Обновить блюдо' : 'Создать блюдо'}
                </button>
                {mealForm.id ? (
                  <button className="ghost-button" type="button" onClick={() => setMealForm(emptyMealForm(coupleId))}>
                    Отменить редактирование
                  </button>
                ) : null}
              </div>
            </form>

            <div className="meal-cards">
              {meals.map((meal) => (
                <article className="meal-card" key={meal.id}>
                  <div className="row-between">
                    <div>
                      <h3>{meal.name}</h3>
                      <p>{meal.description || 'Без описания'}</p>
                    </div>
                    <span className="badge soft">{meal.portionSize}</span>
                  </div>
                  <div className="meal-kbju">
                    <span>К: {meal.calories}</span>
                    <span>Б: {meal.proteins}</span>
                    <span>Ж: {meal.fats}</span>
                    <span>У: {meal.carbs}</span>
                  </div>
                  <div className="ingredient-tags">
                    {meal.ingredients.map((ingredient) => (
                      <span className="badge" key={`${meal.id}_${ingredient.id ?? ingredient.name}`}>
                        {ingredient.name}: {ingredient.amount} {ingredient.unit}
                      </span>
                    ))}
                  </div>
                  <div className="button-row">
                    <button className="ghost-button" type="button" onClick={() => handleEditMeal(meal)}>Редактировать</button>
                    <button className="ghost-button" type="button" onClick={() => handleDeleteMeal(meal.id)}>Удалить</button>
                  </div>
                </article>
              ))}
            </div>
          </article>
        </div>

        <aside className="panel stack-md shopping-panel">
          <div className="row-between">
            <h2 className="section-title">Список покупок</h2>
            <span className="status-pill">{weekData?.shoppingList?.length ?? 0} поз.</span>
          </div>
          <p className="muted-text">
            Автоматически собирается из всех блюд недели с учётом количества порций.
          </p>
          <div className="shopping-list">
            {(weekData?.shoppingList ?? []).length > 0 ? (
              weekData.shoppingList.map((item) => (
                <div className="shopping-item" key={`${item.name}_${item.unit}`}>
                  <strong>{item.name}</strong>
                  <span>{item.totalAmount} {item.unit}</span>
                </div>
              ))
            ) : (
              <div className="planner-empty">Когда запланируете блюда, здесь появятся продукты к покупке.</div>
            )}
          </div>
        </aside>
      </section>
    </div>
  );
}