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
- `GET /api/v1/research/backtest`: executa e **guarda** a simulação. Parâmetros opcionais:
  `symbol`, `strategy=DAY|SWING`, `candles`, `fee`, `slippage`, `riskPercent`,
  `higherTimeframeTrend`, `adxFilter`, `rsiFilter`, `volumeFilter`, `avoidBearishRegime`, `enableShort`,
  `trailingStopAtr`, `maxHoldingCandles`, `maxEmaDistancePercent`, `minAdx`, `minRsi` e `maxRsi`. O sizing recomendado é
  `riskPercent=0.25` ou `0.5`. Retorna `id` e `report`.
- `GET /api/v1/research/backtests`: lista as últimas 50 simulações guardadas.
- `GET /api/v1/research/backtests/{id}`: detalhes de uma simulação e respetivas operações.
- `GET /api/v1/research/backtests/{id}/trades.csv`: exporta os trades concluídos em CSV, incluindo índices, preços, custos, PnL, razão e duração.
- `GET /api/v1/research/backtest/compare`: executa automaticamente long-only, short-only e uma carteira combinada 50/50. Aceita `symbol`, `strategy`, `candles`, `fee`, `slippage` e `riskPercent` e devolve os relatórios em `longOnly`, `shortOnly` e `combined`.

Cada operação no campo `trades` inclui `entryTime`, `exitTime`, `entry`, `stopLoss`,
`takeProfit`, `exit`, `quantity`, `entryFee`, `exitFee`, `netPnl`, `reason` e
`durationSeconds`, além dos índices das candles. O frontend usa estes dados para a
secção **Análise de Operações** e os candles do endpoint `/research/candles` para
desenhar os marcadores de entrada, saída, stop e target.

### Compatibilidade com o frontend

O dashboard React consome os campos `id` e `report` de `/research/backtest`, e os
campos `finalEquity`, `netReturnPercent`, `buyHoldPercent`,
`randomBenchmarkPercent`, `completedTrades`, `winRatePercent`, `profitFactor`,
`maxDrawdownPercent`, `totalFees`, `expectancy`, `averageGain`, `averageLoss`,
`averageDurationSeconds` e `maxConsecutiveLosses` do relatório.
Também consome `id`, `createdAt`, `symbol`, `strategy`, `netReturnPercent`,
`completedTrades`, `maxDrawdownPercent`, `expectancy` e `randomBenchmarkPercent`
de `/research/backtests`.

O frontend trata `profitFactor` não finito/indefinido como `Indefinido` e apresenta a
mensagem `error` devolvida pela API quando o histórico está incompleto. O runtime
obrigatório do frontend é Node.js 24.x, conforme `frontend/.nvmrc` e `package.json`.

### Regras de execução e métricas (Fases 4–6)

- O backtest usa apenas candles fechadas.
- DAY exige uma série contínua de candles de 15 minutos; SWING exige candles de 4 horas.
- Gaps, overlaps ou históricos incompletos impedem a execução de `/research/backtest` e devolvem `400` com um objeto `{ "error": "..." }`.
- A entrada ocorre na abertura da candle seguinte ao sinal.
- Stop loss e take profit são avaliados com dados OHLC. Se stop e target forem tocados na mesma candle, o stop tem prioridade conservadora.
- Um gap abaixo do stop é executado na abertura da candle, com slippage adverso.
- Um stop tocado sem gap é executado no preço do stop, com slippage adverso.
- Posições abertas no fim são marcadas a mercado e não contam como trades concluídos.
- `winRatePercent` considera apenas trades concluídos.
- `profitFactor` é a soma dos ganhos brutos dividida pela soma das perdas brutas. Sem trades ou sem ganhos/perdas suficientes, a métrica pode ser indefinida; o frontend apresenta `Indefinido`.
- `buyHoldPercent` e `randomBenchmarkPercent` usam o mesmo intervalo de candles do backtest. A estratégia aleatória é determinística (seed interno fixa) para permitir comparação reproduzível.
- `expectancy` é o PnL líquido médio por trade concluído; `averageGain` e `averageLoss` são médias condicionais aos trades vencedores/perdedores. `averageDurationSeconds` é a duração média dos trades concluídos.
- Os filtros são independentes e vêm desligados por defeito, exceto os controlos escolhidos pelo dashboard. `maxEmaDistancePercent` impede entradas quando o preço está demasiado afastado da EMA rápida.
- `avoidBearishRegime=true` bloqueia novas entradas long quando a tendência agregada da timeframe superior é bearish. Não fecha posições existentes nem abre posições short.
- `enableShort=true` executa o replay short-only, com sizing por risco, stop/target ATR, custos, métricas e os mesmos benchmarks. Não combina posições long e short no mesmo replay.
- `trailingStopAtr` ativa um trailing stop a `N` ATR do extremo favorável; `0` desativa. `maxHoldingCandles` força uma saída temporal após esse número de candles; `0` desativa.
- `/backtest/compare` combina duas sleeves independentes, cada uma com metade do capital inicial. O resultado combinado agrega equity, trades, custos e métricas; não escolhe retrospectivamente a melhor estratégia.

## GET /api/v1/research/candles?symbol=BTCUSDT&interval=15m&limit=500
Lê os candles fechados do PostgreSQL e descarrega apenas os intervalos em falta da API pública Binance. Retorna `candles`, `fetched`, `stored` e `complete`. `complete=true` só quando a quantidade, o primeiro timestamp e todos os intervalos esperados estão presentes. Intervalos: `5m`, `15m`, `1h`, `4h`, `1d`; 70–10000 candles por pedido. Os backtests reutilizam o mesmo cache e recusam históricos incompletos.
