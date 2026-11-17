# Arquitetura e decisões

## Contexto
Aplicação de investigação quantitativa para Spot. O sistema recomenda/simula; não envia ordens reais.

## Componentes
- **Market data adapter**: Binance public REST no primeiro incremento; WebSocket será adicionado após persistência e reconciliação de candles.
- **Strategy engine**: calcula indicadores e emite sinais explicáveis por estratégia.
- **Risk engine**: calcula quantidade a partir do equity, risco percentual e distância ao stop.
- **Backtest engine (próximo incremento)**: replay cronológico sem look-ahead, custos e fills simulados.
- **Paper broker (próximo incremento)**: saldo, posições, ordens, fills e P&L realizados/não realizados.
- **Persistence**: PostgreSQL para candles, sinais, ordens e snapshots; Redis para cache/locks.
- **Event streaming**: Kafka quando houver consumidores independentes (scanner, notificações, métricas); não é requisito para o primeiro arranque.
- **Frontend**: React/TypeScript, dashboard e relatórios.

## Módulos lógicos
`market-data`, `indicators`, `strategy`, `risk`, `backtesting`, `paper-trading`, `portfolio`, `reporting`, `api`.

## Segurança
- Não existem credenciais Binance no projeto.
- Não existe endpoint de criação de ordens reais.
- `realOrdersEnabled=false` por configuração.
- Nunca adicionar permissões de trading/withdrawal a uma API key para esta fase.

## Gestão de risco
Equity inicial virtual: 10.000 USDT. Risco planeado: 1% por operação (100 USDT no início).
Quantidade teórica = orçamento de risco / (preço de entrada - stop).
O valor final deve ainda ser limitado pelo cash disponível, notional máximo, step size e min notional do símbolo.
Risco por trade não equivale a perda máxima garantida: gaps, slippage e falhas de execução podem exceder o valor planeado.

## Estratégias iniciais (hipóteses, não estratégias validadas)
- Day: candles de 5m/15m, EMA 9/21, RSI 14, confirmação de volume; evitar operar contra tendência de timeframe superior.
- Swing: candles de 4h/1d, EMA 20/50, RSI 14, ATR e estrutura de tendência.
- Score é uma pontuação de regras, não uma probabilidade.

## Validação
- Chronological train/validation/test split.
- Walk-forward analysis e teste em bull, bear e sideways.
- Incluir maker/taker fees, spread e slippage parametrizáveis.
- Evitar survivorship bias: universo histórico, não apenas moedas atualmente listadas.
- Evitar look-ahead: indicadores usam apenas candles fechadas.
- Comparar com buy-and-hold e baseline sem sinal.
- Reportar número de trades, expectancy, profit factor, max drawdown, exposure, turnover e retorno líquido.
