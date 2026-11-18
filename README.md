# Crypto Trading Lab

Laboratório de investigação e paper trading para estratégias **Spot** de Day Trading e Swing Trading.
Não envia ordens reais. Não pede API keys. A Binance é usada apenas como fonte pública de dados.

## Configuração inicial
- Saldo virtual: 10.000 USDT
- Mercado: Spot
- Risco planeado por operação: 1% do equity (100 USDT inicialmente)
- Estratégias: Day e Swing, com rankings e parâmetros separados
- Execução: local, via Docker Compose

> O saldo é fictício. Sinais, backtests e métricas não garantem resultados futuros nem constituem aconselhamento financeiro.

## Stack
- Java 25, Spring Boot 4.1.2, Maven
- React 19 + TypeScript + Vite
- PostgreSQL 17, Redis 8, Kafka (KRaft)
- JUnit 5, Testcontainers (a acrescentar nos próximos incrementos)
- Docker Compose

## Arranque
Requisitos: JDK 25, Maven 3.9+, Docker Desktop/Engine e Node.js 24.x (obrigatório).

O frontend deve ser executado com Node 24. O ficheiro `frontend/.nvmrc` fixa a
versão recomendada (`24.22.1`) e `frontend/package.json` rejeita versões fora de
`>=24 <25`. Com nvm, executar `nvm use` dentro de `frontend/` antes dos comandos
`npm install`, `npm test` ou `npm run build`.

1. `docker compose up -d`
2. `cd backend && mvn spring-boot:run`
3. Noutro terminal: `cd frontend && npm install && npm run dev`
4. Abrir `http://localhost:5174`

Validação do frontend:

```bash
cd frontend
nvm use
npm test
npm run build
```

Backend: `http://localhost:8081`
- `GET /api/v1/lab/config`
- `GET /api/v1/market/universe`

## Estado desta entrega
Esta é a fundação executável do laboratório: configuração, domínio, sizing de posição, indicadores, motor de sinais explicável, endpoints e dashboard inicial. A recolha histórica persistente, backtester completo com fills realistas, paper broker, WebSocket, autenticação, observabilidade e testes de integração são etapas seguintes. Não interpretar o score como probabilidade de lucro.

## Princípios de investigação
1. Nunca otimizar e avaliar no mesmo período.
2. Incluir fee, spread e slippage em todos os resultados.
3. Separar Day e Swing.
4. Guardar todos os sinais, incluindo os que não foram executados.
5. Reportar drawdown, profit factor, expectancy, win rate e número de trades.
6. Paper trading antes de qualquer decisão sobre capital real.

## Correções v0.2
- Adicionado @vitejs/plugin-react que faltava.
- Corrigida documentação de endpoints ainda não implementados.
- Incluído motor inicial de backtesting determinístico com comissões e slippage e teste Java puro.
- Ainda não compilado com Maven nem testado em Docker neste ambiente.

## v0.3 — Backtest com candles reais

A nova rota `GET /api/v1/research/backtest?symbol=BTCUSDT&strategy=DAY&candles=500`
obtém candles **fechadas** da Binance e simula uma estratégia long-only EMA crossover:
- DAY: 15m, EMA 9/21; SWING: 4h, EMA 20/50.
- Sinal na vela anterior; entrada na abertura da vela seguinte.
- Stop a 2 ATR e target a 4 ATR; em vela ambígua assume stop primeiro.
- Quantidade limitada por 1% de risco planeado e pelo saldo disponível.
- Comissão de 0,1% e slippage de 0,05% por execução (configuráveis na API).
- Relatório com retorno líquido, buy-and-hold, trades, win rate, profit factor e drawdown.

**Limitações relevantes:** um só par/posição por backtest, até 1000 candles por pedido,
ATR como média simples do true range, sem volume/RSI no gatilho, sem avaliação fora da
amostra e sem paper broker em tempo real. A execução intrabar usa uma política
conservadora stop-first; gaps abaixo do stop são executados na abertura com slippage e
não representam fills garantidos. Históricos com gaps ou incompletos são recusados.
Posições abertas no fim são marcadas a mercado, não liquidadas. Win rate e profit factor
consideram apenas trades concluídos; métricas indefinidas são apresentadas como
`Indefinido` no frontend. Esta versão não é adequada para decidir investir dinheiro real.

**Validação efetuada:** o backend possui testes JUnit para sizing, custos, execução de
stops/targets/gaps, drawdown, métricas e integridade temporal/look-ahead. O frontend é
validado com `npm run build`.

O `ReplayBacktester` é o único motor usado pela API e pelo histórico persistido. O
motor close-to-close original foi isolado em `backtest.legacy.LegacyBacktestEngine`
apenas para preservar exemplos/testes didáticos; não deve ser usado para comparar
resultados de investigação.

## v0.4 — Persistência de backtests

Ao executar `GET /api/v1/research/backtest`, o relatório e as operações fechadas são guardados em PostgreSQL, numa transação única. A resposta inclui `{ "id": ..., "report": ... }`. O frontend apresenta o histórico.

- `GET /api/v1/research/backtests`: últimas 50 simulações (mais recentes primeiro).
- `GET /api/v1/research/backtests/{id}`: métricas e operações de uma simulação.
- Tabelas: `backtest_runs` e `backtest_trades`.
- O volume Docker `postgres_data` mantém os dados após reiniciar os containers; `docker compose down -v` **apaga os volumes**.
- Nesta fase usa-se `spring.jpa.hibernate.ddl-auto=update` apenas para desenvolvimento local; antes de produção, substituir por migrações Flyway.
- A base de dados ainda não guarda candles históricos nem paper orders.
- Não existe autenticação: executar apenas localmente.

**Importante:** a rota `/backtest` é GET por compatibilidade com o frontend existente, embora provoque escrita na base de dados. Numa futura versão será convertida para POST.

## v0.5 — Cache persistente de candles
`GET /api/v1/research/candles?symbol=BTCUSDT&interval=15m&limit=500` devolve candles fechados, quantidade descarregada (`fetched`), total armazenado (`stored`) e `complete` (se existem todos os intervalos esperados). Os candles são gravados em `market_candles` no PostgreSQL com unicidade `(symbol, candle_interval, open_time)` e INSERT idempotente. Os backtests passam a utilizar este histórico persistido. O endpoint pode voltar a consultar a Binance quando faltam candles; `complete=false` indica lacunas, por exemplo por falta de negociação/listagem. Requer PostgreSQL em execução e conectividade à Binance na primeira consulta. Para produção, substituir `ddl-auto: update` por migrações Flyway e introduzir rate limiting, retries e controlo de concorrência. Não existe recolha automática em background nesta versão.
