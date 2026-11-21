# Crypto Trading Lab

Laboratório de investigação e paper trading para estratégias **Spot** de Day Trading e Swing Trading.
Não envia ordens reais. Não pede API keys. A Binance é usada apenas como fonte pública de dados.

## Configuração inicial
- Saldo virtual: 10.000 USDT
- Mercado: Spot
- Risco recomendado nos estudos: 0,25–0,5% do equity
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

### Testes no macOS

Backend, usando o cache Maven já preenchido:

```bash
cd backend
mvn -o -Daether.syncContext.named.factory=rwlock-local -q test
```

O `rwlock-local` evita problemas de file locking do Maven no macOS. Se for
necessário descarregar dependências, use Maven normalmente uma vez com acesso à
rede e depois volte a usar `-o`.

Frontend, com Node 24:

```bash
cd frontend
nvm use
npm ci --include=dev --ignore-scripts --no-audit --no-fund
npm test -- --run
npm run build
```

Em ambientes macOS que injetem configurações próprias no `npm` (por exemplo,
`CODEX_MANAGED_BY_NPM`) e apresentem `SecItemCopyMatching failed -50`, execute
com um ambiente limpo:

```bash
env -i \
  PATH="$HOME/.nvm/versions/node/v24.22.1/bin:/opt/homebrew/bin:/usr/bin:/bin" \
  HOME="$HOME" \
  NPM_CONFIG_CACHE=/private/tmp/crypto-npm-cache \
  npm ci --include=dev --ignore-scripts --no-audit --no-fund

env -i \
  PATH="$HOME/.nvm/versions/node/v24.22.1/bin:/opt/homebrew/bin:/usr/bin:/bin" \
  HOME="$HOME" \
  NPM_CONFIG_CACHE=/private/tmp/crypto-npm-cache \
  npm test -- --run

env -i \
  PATH="$HOME/.nvm/versions/node/v24.22.1/bin:/opt/homebrew/bin:/usr/bin:/bin" \
  HOME="$HOME" \
  NPM_CONFIG_CACHE=/private/tmp/crypto-npm-cache \
  npm run build
```

Se o Node 24 for gerido pelo runtime do IntelliJ/Codex, substitua o primeiro
elemento de `PATH` pelo diretório `bin` desse runtime. O ponto importante é
remover as variáveis `CODEX_MANAGED_*`, `npm_config_local_prefix` e
`npm_package_json` do ambiente do processo.

Backend: `http://localhost:8081`
- `GET /api/v1/lab/config`
- `GET /api/v1/market/universe`

## Estado desta entrega
O laboratório inclui backtesting persistente, filtros configuráveis, walk-forward,
benchmarks, métricas de risco, endpoints de investigação, dashboard e testes de
integração HTTP. A implementação atual usa uma política comum de sinais entre o
`SignalEngine` e o `ReplayBacktester`, incluindo saída em cruzamento bearish.

O replay suporta filtros opcionais de tendência numa timeframe superior, ADX, RSI,
volume e distância máxima à EMA. Os relatórios incluem buy-and-hold, benchmark
aleatório determinístico, expectancy, médias de ganho/perda, duração média e
perdas consecutivas. O endpoint e o dashboard permitem controlar o risco por
operação, com opções recomendadas de 0,25% e 0,5%, e os custos de execução são
parametrizáveis. Existe também um replay short-only ativável com `enableShort=true`,
com sizing, stop/target ATR, custos, métricas e benchmarks equivalentes ao modo
long-only. O endpoint `/api/v1/research/backtest/compare` compara automaticamente
long-only, short-only e uma carteira combinada 50/50.

O estado de validação desta entrega está registado em
[`docs/TEST-REPORT.md`](docs/TEST-REPORT.md). Não interpretar os resultados como
garantia de lucro.

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
- Os comandos de validação para macOS estão documentados acima.

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
validado com `npm test -- --run` e `npm run build`. Existem também testes HTTP de
integração para os endpoints de backtest e histórico.

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
