# API inicial

## GET /api/v1/lab/config
Devolve configuração do laboratório, incluindo saldo fictício, risco, modo e mercado.

## GET /api/v1/market/universe
Devolve até 50 pares USDT com volume cotado 24h >= 5M USDT.
Este universo é apenas um filtro de liquidez atual; ainda não é um universo histórico sem survivorship bias.

## Próximos endpoints
- `GET /api/v1/signals?strategy=DAY|SWING`
- `POST /api/v1/backtests/run`
- `GET /api/v1/backtests/{id}/report`
- `GET /api/v1/paper/portfolio`
- `GET /api/v1/paper/trades`

Nota: Os endpoints de sinais, backtesting e paper portfolio descritos como futuros ainda nao estao implementados.

## Persistência v0.4
- `GET /api/v1/research/backtest?symbol=BTCUSDT&strategy=DAY&candles=500`: executa e **guarda** a simulação. Retorna `id` e `report`.
- `GET /api/v1/research/backtests`: lista as últimas 50 simulações guardadas.
- `GET /api/v1/research/backtests/{id}`: detalhes de uma simulação e respetivas operações.

## GET /api/v1/research/candles?symbol=BTCUSDT&interval=15m&limit=500
Lê os candles fechados do PostgreSQL e descarrega apenas os intervalos em falta da API pública Binance. Retorna `candles`, `fetched`, `stored` e `complete`. Intervalos: `5m`, `15m`, `1h`, `4h`, `1d`; 70–10000 candles por pedido. Os backtests reutilizam o mesmo cache.
