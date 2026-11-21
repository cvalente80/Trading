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

Não usar `docker compose down -v` salvo se for intencional apagar os dados
persistidos do PostgreSQL.
