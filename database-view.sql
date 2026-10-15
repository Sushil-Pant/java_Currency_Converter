USE currency_db;

SHOW TABLES;

SELECT from_currency, to_currency, rate
FROM exchange_rates
ORDER BY from_currency, to_currency;

SELECT id, from_currency, to_currency, original_amount, converted_amount, created_at
FROM conversion_history
ORDER BY id DESC;
