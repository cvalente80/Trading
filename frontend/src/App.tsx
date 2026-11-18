import {useEffect, useState} from 'react';
import {apiError, formatMetric, normalizeProfitFactor} from './metrics';

type Config = {initialBalanceUsdt:number; riskPerTradePercent:number; mode:string; market:string; realOrdersEnabled:boolean; strategies:string[]};
type Ticker = {symbol:string; lastPrice:string; quoteVolume:string; priceChangePercent:string};
const API = 'http://localhost:8081/api/v1';

export default function App() {
  const [config,setConfig] = useState<Config|null>(null);
  const [tickers,setTickers] = useState<Ticker[]>([]);
  const [error,setError] = useState('');
  const [symbol,setSymbol] = useState('BTCUSDT');
  const [strategy,setStrategy] = useState('DAY');
  const [report,setReport] = useState<{finalEquity:number;netReturnPercent:number;buyHoldPercent:number;completedTrades:number;winRatePercent:number;profitFactor:number|null;maxDrawdownPercent:number;totalFees:number}|null>(null);
  const [busy,setBusy] = useState(false);
  type SavedRun = {id:number; createdAt:string; symbol:string; strategy:string; netReturnPercent:number; completedTrades:number; maxDrawdownPercent:number; finalEquity:number};
  const [history,setHistory] = useState<SavedRun[]>([]);
  const [savedId,setSavedId] = useState<number|null>(null);
  async function refreshHistory() {
    try {const r=await fetch(`${API}/research/backtests`); if(r.ok) setHistory(await r.json());} catch { /* backend may be offline */ }
  }

  async function runBacktest() {
    setBusy(true); setError(''); setReport(null);
    try {
      const response = await fetch(`${API}/research/backtest?symbol=${encodeURIComponent(symbol)}&strategy=${strategy}&candles=500`);
      if (!response.ok) throw new Error(await apiError(response));
      const saved = await response.json();
      setReport({...saved.report, profitFactor: normalizeProfitFactor(saved.report?.profitFactor)});
      setSavedId(saved.id); await refreshHistory();
    } catch (e) { setError(`Backtest indisponível: ${String(e)}`); }
    finally { setBusy(false); }
  }
  useEffect(() => {
    void refreshHistory();
    Promise.all([fetch(`${API}/lab/config`).then(r=>r.json()),fetch(`${API}/market/universe`).then(r=>r.json())])
      .then(([c,t])=>{setConfig(c);setTickers(t);}).catch(()=>setError('Não foi possível contactar o backend. Confirma se o Spring Boot está a correr.'));
  },[]);
  return <main className="shell">
    <header><div><p className="eyebrow">RESEARCH • PAPER TRADING • SPOT</p><h1>Crypto Trading Lab</h1><p className="muted">Laboratório de estratégias Day Trading e Swing Trading</p></div><span className="pill">PAPER ONLY</span></header>
    {error && <div className="error">{error}</div>}
    <section className="cards">
      <article><label>Saldo virtual</label><strong>{config ? `${Number(config.initialBalanceUsdt).toLocaleString('en-US')} USDT` : '—'}</strong></article>
      <article><label>Risco planeado / trade</label><strong>{config ? `${config.riskPerTradePercent}%` : '—'}</strong></article>
      <article><label>Mercado</label><strong>{config?.market ?? '—'}</strong></article>
      <article><label>Ordens reais</label><strong className="safe">{config?.realOrdersEnabled ? 'Ativadas' : 'Desativadas'}</strong></article>
    </section>
    <section className="panel">
      <h2>Backtesting histórico</h2><p className="muted">500 candles fechadas da Binance. Resultados simulados, não são sinais em tempo real.</p>
      <div className="controls">
        <select value={symbol} onChange={e=>setSymbol(e.target.value)}>{['BTCUSDT','ETHUSDT','SOLUSDT','BNBUSDT'].map(s=><option key={s}>{s}</option>)}</select>
        <select value={strategy} onChange={e=>setStrategy(e.target.value)}><option value="DAY">Day (15m)</option><option value="SWING">Swing (4h)</option></select>
        <button disabled={busy} onClick={runBacktest}>{busy?'A simular…':'Executar backtest'}</button>
      </div>
      {savedId !== null && <p className="muted">Resultado guardado na base de dados: #{savedId}</p>}
      {report && <div className="metrics">
        <div>Equity final<strong>{formatMetric(report.finalEquity, 2, ' USDT')}</strong></div>
        <div>Retorno líquido<strong>{formatMetric(report.netReturnPercent, 2, '%')}</strong></div>
        <div>Buy & Hold<strong>{formatMetric(report.buyHoldPercent, 2, '%')}</strong></div>
        <div>Trades fechados<strong>{report.completedTrades}</strong></div>
        <div>Win rate<strong>{formatMetric(report.winRatePercent, 1, '%')}</strong></div>
        <div>Profit factor<strong>{formatMetric(report.profitFactor)}</strong><small>Sem perdas ou sem trades</small></div>
        <div>Max drawdown<strong>{formatMetric(report.maxDrawdownPercent, 2, '%')}</strong></div>
        <div>Comissões<strong>{formatMetric(report.totalFees, 2, ' USDT')}</strong></div>
      </div>}
    </section>
    <section className="panel"><div className="panelhead"><div><h2>Histórico de backtests guardados</h2><p className="muted">Últimas 50 simulações persistidas no PostgreSQL</p></div><button onClick={refreshHistory}>Atualizar</button></div>
      <div className="tablewrap"><table><thead><tr><th>ID</th><th>Data UTC</th><th>Par</th><th>Estratégia</th><th>Retorno</th><th>Trades</th><th>Drawdown</th></tr></thead><tbody>
      {history.map(h=><tr key={h.id}><td>#{h.id}</td><td>{new Date(h.createdAt).toLocaleString('pt-PT')}</td><td>{h.symbol}</td><td>{h.strategy}</td><td className={h.netReturnPercent>=0?'positive':'negative'}>{h.netReturnPercent.toFixed(2)}%</td><td>{h.completedTrades}</td><td>{h.maxDrawdownPercent.toFixed(2)}%</td></tr>)}
      {history.length===0 && <tr><td colSpan={7} className="muted">Ainda não existem backtests guardados.</td></tr>}
      </tbody></table></div>
    </section>
    <section className="panel"><div className="panelhead"><div><h2>Universo de mercado</h2><p className="muted">Pares USDT com volume cotado 24h superior a 5 milhões USDT</p></div><span className="pill">{tickers.length} ativos</span></div>
      <div className="tablewrap"><table><thead><tr><th>Par</th><th>Último preço</th><th>Variação 24h</th><th>Volume 24h (USDT)</th></tr></thead><tbody>
      {tickers.map(t=><tr key={t.symbol}><td>{t.symbol}</td><td>{Number(t.lastPrice).toLocaleString('en-US',{maximumFractionDigits:8})}</td><td className={Number(t.priceChangePercent)>=0?'positive':'negative'}>{Number(t.priceChangePercent).toFixed(2)}%</td><td>{(Number(t.quoteVolume)/1e6).toFixed(1)}M</td></tr>)}
      {!tickers.length && <tr><td colSpan={4} className="muted">A carregar dados públicos da Binance…</td></tr>}
      </tbody></table></div>
    </section>
    <section className="strategies"><article className="panel"><h2>Day Trading</h2><p className="muted">EMA 9/21 • RSI 14 • volume</p><p>Horizonte de análise curto. A implementação do scanner de candles e ranking está na próxima etapa.</p></article><article className="panel"><h2>Swing Trading</h2><p className="muted">EMA 20/50 • RSI 14 • volume</p><p>Horizonte de análise alargado. Parâmetros independentes para evitar misturar estratégias.</p></article></section>
    <footer>Dados públicos de mercado. Sem execução de ordens. Scores não representam probabilidades de lucro.</footer>
  </main>;
}
