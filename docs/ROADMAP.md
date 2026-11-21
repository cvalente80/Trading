# Roadmap de implementação

## Entrega 0 — Fundação (incluída)
- [x] Configuração paper: 10.000 USDT, Spot, 1% risco
- [x] Separação de tipos Day/Swing no domínio
- [x] Indicadores EMA, RSI e média de volume
- [x] Motor inicial de sinais explicáveis
- [x] Binance 24h ticker público e filtro inicial de liquidez
- [x] Dashboard React/TypeScript
- [x] Docker Compose PostgreSQL, Redis e Kafka
- [x] Teste unitário de position sizing

## Entrega 1 — Dados e backtesting (prioridade)
- [ ] Binance klines histórico com paginação, rate-limit e retries
- [ ] Persistência de candles com chave (symbol, interval, open_time)
- [ ] Backtest cronológico sem look-ahead
- [ ] Fees, spread e slippage configuráveis
- [x] Stop, target, trailing stop e saída temporal
- [x] Relatório Day vs Swing e export CSV

## Entrega 2 — Paper broker
- [x] Livro de ordens e fills simulados
- [x] Equity/cash/positions e P&L realizado/não realizado
- [ ] Regras de sizing com limites de notional e saldo
- [ ] Diário imutável de decisões e sinais

## Entrega 3 — Qualidade
- [ ] Testcontainers PostgreSQL/Redis/Kafka
- [ ] WireMock para Binance
- [ ] Testes de integração e contrato
- [ ] Playwright E2E
- [ ] Testes de carga k6/Gatling
- [ ] CI GitHub Actions, análise estática e cobertura

## Entrega 4 — Observabilidade e validação em tempo real
- [ ] OpenTelemetry, Prometheus e Grafana
- [ ] Scanner contínuo com WebSocket e reconciliação REST
- [ ] Alertas Telegram opcionais
- [ ] Paper trading durante período de observação definido
- [ ] Critérios objetivos para promoção/rejeição de estratégia
