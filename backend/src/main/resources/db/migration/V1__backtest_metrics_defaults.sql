-- Safe additive migration for databases created before extended backtest metrics.
-- It is intentionally idempotent so it can be applied during local upgrades.
ALTER TABLE IF EXISTS backtest_runs ADD COLUMN IF NOT EXISTS expectancy double precision NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS backtest_runs ADD COLUMN IF NOT EXISTS average_gain double precision NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS backtest_runs ADD COLUMN IF NOT EXISTS average_loss double precision NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS backtest_runs ADD COLUMN IF NOT EXISTS average_duration_seconds double precision NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS backtest_runs ADD COLUMN IF NOT EXISTS max_consecutive_losses integer NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS backtest_runs ADD COLUMN IF NOT EXISTS random_benchmark_percent double precision NOT NULL DEFAULT 0;
