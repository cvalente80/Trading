# Relatório resumido de testes

Data da última execução: 21 de novembro de 2026

## Backend

Comando executado:

```bash
cd backend
mvn -o -Daether.syncContext.named.factory=rwlock-local -q test
```

Resultado: sucesso.

A suite inclui testes para:

- sizing e risco por operação;
- custos, slippage e sweep de fees;
- execução de stops, targets e gaps;
- cruzamentos bullish e bearish;
- configuração e integração dos filtros de tendência, ADX, RSI, volume e distância à EMA;
- métricas de expectancy, ganhos/perdas médios, duração e perdas consecutivas;
- benchmarks buy-and-hold e aleatório determinístico;
- walk-forward train/validation/out-of-sample;
- integridade temporal e prevenção de look-ahead;
- endpoints HTTP de backtest, histórico e validação de candles incompletas.

Nota: o Maven pode emitir um aviso do Mockito sobre self-attachment no JDK 27.
O aviso não causou falhas e a suite terminou com sucesso.

## Frontend

Comandos executados com Node 24 e ambiente npm limpo:

```bash
cd frontend
npm test -- --run
npm run build
```

Resultado dos testes: 1 ficheiro de testes, 3 testes aprovados.

Resultado do build: TypeScript e Vite concluídos com sucesso. Os artefactos foram
gerados em `frontend/dist`.

## Ambiente macOS

Quando o ambiente injeta configurações próprias no npm, usar Node 24 e um ambiente
limpo conforme [`DEVELOPMENT.md`](DEVELOPMENT.md). Isto evita o erro
`SecItemCopyMatching failed -50` associado ao Keychain/npm do ambiente, sem alterar
o código da aplicação.
