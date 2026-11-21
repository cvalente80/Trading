# Desenvolvimento

## Requisitos

- JDK 25
- Maven 3.9+
- Node.js 24.x
- Docker Desktop, se forem necessários PostgreSQL e Redis

O backend usa Spring Boot 4.1.2. O frontend fixa Node 24 em
`frontend/.nvmrc`; versões Node 25 não são suportadas pelo `package.json`.

## Backend

Executar todos os testes unitários e de integração:

```bash
cd backend
mvn -o -Daether.syncContext.named.factory=rwlock-local -q test
```

Os testes HTTP de `/api/v1/research/backtest` e dos endpoints de histórico estão
em `src/test/java/dev/cvalente/trading/api/ResearchControllerIntegrationTest.java`.
O teste usa `MockMvc` e não requer PostgreSQL ou Binance.

Se o cache Maven ainda não tiver as dependências, remover `-o` na primeira
execução. O parâmetro `rwlock-local` é recomendado no macOS para evitar erros de
lock no repositório local.

## Frontend

```bash
cd frontend
nvm use
npm ci --include=dev --ignore-scripts --no-audit --no-fund
npm test -- --run
npm run build
```

`npm test -- --run` executa o Vitest uma vez. `npm run build` executa primeiro o
TypeScript (`tsc -b`) e depois o build de produção do Vite.

### Diagnóstico de `SecItemCopyMatching failed -50`

Esse erro é causado por configurações npm herdadas do ambiente do agente ou do
Keychain do macOS. Não é um erro da aplicação. Execute os comandos npm com um
ambiente mínimo:

```bash
env -i \
  PATH="/caminho/para/node-24/bin:/opt/homebrew/bin:/usr/bin:/bin" \
  HOME="$HOME" \
  NPM_CONFIG_CACHE=/private/tmp/crypto-npm-cache \
  npm ci --include=dev --ignore-scripts --no-audit --no-fund
```

Depois execute `npm test -- --run` e `npm run build` no mesmo ambiente. O cache
temporário pode ser substituído por um diretório persistente do utilizador.

## Arranque local

```bash
docker compose up -d
cd backend && mvn spring-boot:run
```

Noutro terminal:

```bash
cd frontend
nvm use
npm run dev
```

Dashboard: `http://localhost:5174`

Backend: `http://localhost:8081`

O parâmetro `avoidBearishRegime=true` é a proteção long-only recomendada:
bloqueia novas entradas durante regimes bearish, sem abrir posições short nem
fechar automaticamente posições existentes.

Para investigar a versão short, usar `enableShort=true` no endpoint de backtest.
O modo short é separado do long-only e mantém o mesmo contrato de relatório,
sizing por risco, custos e benchmarks.

Para comparar os três modos, usar `GET /api/v1/research/backtest/compare`. O modo
combinado usa duas sleeves independentes de 50% do capital, evitando seleção
retrospectiva do melhor resultado.

Trailing stop e saída temporal são controlados por `trailingStopAtr` e
`maxHoldingCandles`. Ambos vêm desativados (`0`) por defeito e aplicam-se aos
replays long e short.

Não usar `docker compose down -v` salvo se for intencional apagar os dados
persistidos do PostgreSQL.

O endpoint `POST /api/v1/paper/candles` é o ponto de entrada do fluxo de candles
para o Paper Broker: cada candle pode gerar fills e atualiza o estado persistido.
O endpoint `POST /api/v1/paper/signals/evaluate` liga a decisão do
`SignalEngine` à criação de ordens; a ordem só é executada quando uma candle
posterior gera o fill.

## Integração PostgreSQL com Testcontainers

O teste `PaperBrokerPostgresIT` usa PostgreSQL 17 via Testcontainers para validar
persistência e recuperação de cash, ordens, fills e posições. Requer Docker
Desktop/Engine acessível:

```bash
cd backend
mvn -Dtest=PaperBrokerPostgresIT test
```

Sem Docker, o teste é automaticamente marcado como skipped; a suite normal não
falha por isso. Em CI, garantir que o runner tenha Docker disponível.

## Integração Redis/Kafka com Testcontainers

`RedisKafkaContainersIT` valida uma operação real de escrita/leitura no Redis e
um round-trip de tópico/mensagem no Kafka:

```bash
cd backend
mvn -Dtest=RedisKafkaContainersIT test
```

O teste requer Docker e usa Redis 8 e Kafka `confluentinc/cp-kafka:7.8.0`.

O teste integrado do Paper Broker sobe PostgreSQL, Redis e Kafka no mesmo cenário:

```bash
cd backend
mvn -Dtest=PaperBrokerInfrastructureIT test
```

Valida persistência/recuperação do broker em PostgreSQL, estado auxiliar em
Redis e publicação do evento de fill em Kafka.
