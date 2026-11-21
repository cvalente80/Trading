---
name: crypto-trading-lab
description: Desenvolver, testar e analisar o Crypto Trading Lab com foco em backtesting reproduzível, paper trading e segurança antes de capital real.
---

# Crypto Trading Lab

## Objetivo

A aplicação é um laboratório de investigação para estratégias de trading de criptomoedas. Priorizar reprodutibilidade, controlo de risco, validação fora da amostra, paper trading e prevenção de ordens reais acidentais.

Nunca assumir que uma estratégia lucrativa no backtest está pronta para produção.

## Estrutura do projeto

- `backend/`: API Spring Boot, pesquisa, backtesting e persistência.
- `frontend/`: aplicação React/Vite para configuração, resultados e análise.
- `backend/src/main/java/.../research/`: backtesting e métricas.
- `backend/src/main/java/.../paper/`: paper trading.
- `backend/src/main/java/.../persistence/`: entidades e repositórios.
- `frontend/src/App.tsx`: painel principal.
- `frontend/src/OperationAnalysis.tsx`: análise das operações.
- `frontend/src/style.css`: estilos da aplicação.

## Regras de trading

- Não ativar ordens reais por defeito.
- Preservar `real-orders-enabled: false`.
- Não remover o modo `PAPER` sem pedido explícito.
- Não aumentar o risco por trade automaticamente.
- Nunca alterar sizing sem incluir fees, slippage e stop-loss.
- Todas as decisões devem usar candles fechados.
- Evitar look-ahead bias.
- Signals devem ser executados no candle seguinte quando essa for a convenção do backtester.

## Backtesting

Cada backtest deve guardar símbolo, timeframe, número de candles, fees, slippage, risco por trade, filtros, parâmetros de stop/take-profit, versão determinística e configuração completa.

Separar sempre:

- treino;
- validação;
- out-of-sample;
- paper trading;
- produção.

Nunca escolher uma configuração apenas pelo maior retorno histórico.

## Walk-forward

Estudos walk-forward devem manter a ordem cronológica, não misturar dados futuros, usar pelo menos 70 candles por período, comparar treino/validação/out-of-sample, testar vários pares e distinguir baseline de configuração conservadora.

Apresentar, no mínimo, retorno, drawdown, trades, expectancy e profit factor.

## Métricas

Sempre que possível apresentar:

- retorno líquido;
- buy-and-hold;
- profit factor;
- expectancy;
- win rate;
- fees totais;
- drawdown máximo;
- perdas consecutivas;
- Sharpe;
- Sortino;
- Calmar;
- exposição média e máxima;
- VaR/CVaR.

Interpretar retorno juntamente com drawdown e número de trades.

## Backend

Ao adicionar endpoints:

- validar parâmetros;
- rejeitar símbolos vazios ou em excesso;
- rejeitar históricos incompletos;
- devolver erros claros;
- preservar endpoints existentes;
- adicionar testes unitários e de integração.

Ao alterar entidades JPA, adicionar migração SQL, definir defaults para dados antigos e verificar compatibilidade com `ddl-auto: update`.

## Frontend

O frontend deve mostrar claramente que os resultados são simulados, indicar dados incompletos, apresentar loading e erros, usar unidades nos números, destacar drawdown e perdas, identificar a versão da estratégia e nunca sugerir garantia de lucro.

## Testes

Executar, quando o ambiente permitir:

```bash
cd backend
mvn test
```

```bash
cd frontend
npm test
npm run build
```

Se o ambiente bloquear Maven, npm, rede ou base de dados, reportar o erro e distinguir problemas de ambiente de falhas do código.

## Commits

Usar commits pequenos e descritivos com prefixos `feat:`, `fix:`, `test:`, `refactor:` ou `docs:`.

Antes de criar um commit:

```bash
git diff --check
git status
```

Nunca fazer `git push` sem autorização explícita.

## Segurança

- Não guardar chaves API no código.
- Não imprimir segredos nos logs.
- Não ativar ordens reais automaticamente.
- Não executar operações destrutivas sem confirmação.
- Antes de produção, exigir paper trading aprovado e kill switch funcional.

## Promoção para capital real

Uma estratégia só pode avançar se tiver resultados positivos out-of-sample, pelo menos 4–8 semanas de paper trading, custos realistas, drawdown dentro do limite, kill switch testado e divergência aceitável entre paper trading e backtest.

Começar com capital reduzido e risco inferior a 0,25% por trade.
