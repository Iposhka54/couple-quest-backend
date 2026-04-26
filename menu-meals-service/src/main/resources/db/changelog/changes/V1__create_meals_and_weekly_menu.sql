CREATE TABLE IF NOT EXISTS meals (
    id BIGSERIAL PRIMARY KEY,
    couple_id BIGINT NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    portion_size VARCHAR(100) NOT NULL,
    calories NUMERIC(10, 2) NOT NULL,
    proteins NUMERIC(10, 2) NOT NULL,
    fats NUMERIC(10, 2) NOT NULL,
    carbs NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_meals_couple_id ON meals(couple_id);

CREATE TABLE IF NOT EXISTS meal_ingredients (
    id BIGSERIAL PRIMARY KEY,
    meal_id BIGINT NOT NULL REFERENCES meals(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    amount NUMERIC(12, 3) NOT NULL,
    unit VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_meal_ingredients_meal_id ON meal_ingredients(meal_id);

CREATE TABLE IF NOT EXISTS weekly_menu_entries (
    id BIGSERIAL PRIMARY KEY,
    couple_id BIGINT NOT NULL,
    meal_id BIGINT NOT NULL REFERENCES meals(id) ON DELETE CASCADE,
    planned_date DATE NOT NULL,
    meal_type VARCHAR(30) NOT NULL,
    servings INTEGER NOT NULL,
    note VARCHAR(1000),
    updated_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_weekly_menu_entries UNIQUE (couple_id, planned_date, meal_type)
);

CREATE INDEX IF NOT EXISTS idx_weekly_menu_entries_couple_date
    ON weekly_menu_entries(couple_id, planned_date);