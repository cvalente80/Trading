import {useEffect, useState} from 'react';
import {apiError, formatMetric, normalizeProfitFactor} from './metrics';
import OperationAnalysis, {type Candle, type Operation} from './OperationAnalysis';

type Config = {initialBalanceUsdt:number; riskPerTradePercent:number; mode:string; market:string; realOrdersEnabled:boolean; strategies:string[]};
type Ticker = {symbol:string; lastPrice:string; quoteVolume:string; priceChangePercent:string};
const API = 'http://localhost:8081/api/v1';

export default function App() {
  const [config,setConfig] = useState<Config|null>(null);
  const [tickers,setTickers] = useState<Ticker[]>([]);
  const [error,setError] = useState('');
  const [symbol,setSymbol] = useState('BTCUSDT');
  const [strategy,setStrategy] = useState('DAY');
  const [report,setReport] = useState<{finalEquity:number;netReturnPercent:number;buyHoldPercent:number;randomBenchmarkPercent:number;completedTrades:number;winRatePercent:number;profitFactor:number|null;maxDrawdownPercent:number;totalFees:number;expectancy:number;averageGain:number;averageLoss:number;averageDurationSeconds:number;maxConsecutiveLosses:number}|null>(null);
  const [runMeta,setRunMeta] = useState<{strategyVersion:string;configuration:string}|null>(null);
  const [study,setStudy] = useState<{strategy:string;requestedCandles:number;trainFraction:number;validationFraction:number;cells:Array<{symbol:string;interval:string;period:string;configuration:string;candles:number;netReturnPercent:number;buyHoldPercent:number;maxDrawdownPercent:number;profitFactor:number;expectancy:number;completedTrades:number;winRatePercent:number}>}|null>(null);
  const [studyBusy,setStudyBusy] = useState(false);
  const [studySymbols,setStudySymbols] = useState('BTCUSDT,ETHUSDT,SOLUSDT');
  const [studyCandles,setStudyCandles] = useState(1000);
  const [studyConservative,setStudyConservative] = useState(false);
  async function runStudy() {
    setStudyBusy(true); setError('');
    try {
      const params = new URLSearchParams({symbols:studySymbols,strategy,candles:String(studyCandles),fee:String(fee),slippage:String(slippage),riskPercent:String(riskPercent),conservative:String(studyConservative)});
      const response = await fetch(`${API}/research/walk-forward?${params}`);
      if (!response.ok) throw new Error(await apiError(response));
      setStudy(await response.json());
    } catch (e) { setError(`Estudo indisponível: ${String(e)}`); }
    finally { setStudyBusy(false); }
  }
  const [busy,setBusy] = useState(false);
  type SavedRun = {id:number; createdAt:string; symbol:string; strategy:string; netReturnPercent:number; completedTrades:number; maxDrawdownPercent:number; finalEquity:number; expectancy:number; randomBenchmarkPercent:number};
  const [history,setHistory] = useState<SavedRun[]>([]);
  const [savedId,setSavedId] = useState<number|null>(null);
  const [operations,setOperations] = useState<Operation[]>([]);
  const [candles,setCandles] = useState<Candle[]>([]);
  const [riskPercent,setRiskPercent] = useState(.5);
  const [candleCount,setCandleCount] = useState(500);
  const [fee,setFee] = useState(.001);
  const [slippage,setSlippage] = useState(.0005);
  const [trailingStopAtr,setTrailingStopAtr] = useState(0);
  const [maxHoldingCandles,setMaxHoldingCandles] = useState(0);
  const [minAdx,setMinAdx] = useState(20);
  const [minRsi,setMinRsi] = useState(45);
  const [maxRsi,setMaxRsi] = useState(68);
  const [filters,setFilters] = useState({higherTimeframeTrend:false,adxFilter:false,rsiFilter:false,volumeFilter:false,avoidBearishRegime:true,enableShort:false});
  const [maxEmaDistancePercent,setMaxEmaDistancePercent] = useState(3);
  async function refreshHistory() {
    try {const r=await fetch(`${API}/research/backtests`); if(r.ok) setHistory(await r.json());} catch { /* backend may be offline */ }
  }

  async function runBacktest() {
    setBusy(true); setError(''); setReport(null);
    try {
      const params = new URLSearchParams({symbol,strategy,candles:String(candleCount),riskPercent:String(riskPercent),fee:String(fee),slippage:String(slippage),trailingStopAtr:String(trailingStopAtr),maxHoldingCandles:String(maxHoldingCandles),minAdx:String(minAdx),minRsi:String(minRsi),maxRsi:String(maxRsi),maxEmaDistancePercent:String(maxEmaDistancePercent),...Object.fromEntries(Object.entries(filters).map(([k,v])=>[k,String(v)]))});
      const response = await fetch(`${API}/research/backtest?${params}`);
      if (!response.ok) throw new Error(await apiError(response));
      const saved = await response.json();
      setReport({...saved.report, profitFactor: normalizeProfitFactor(saved.report?.profitFactor)});
      setRunMeta({strategyVersion:saved.strategyVersion ?? 'legacy', configuration:saved.configuration ?? ''});
      setSavedId(saved.id);
      const details = await fetch(`${API}/research/backtests/${saved.id}`).then(async r => r.ok ? r.json() : Promise.reject(new Error(await apiError(r))));
      const summary = details.summary;
      const candleData = await fetch(`${API}/research/candles?symbol=${summary.symbol}&interval=${summary.interval}&limit=${summary.candleCount}`).then(async r => r.ok ? r.json() : Promise.reject(new Error(await apiError(r))));
      setOperations(details.trades ?? []); setCandles(candleData.candles ?? []); await refreshHistory();
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
    {report && <OperationAnalysis trades={operations} candles={candles} />}
    <section className="panel study-panel">
      <div className="panelhead"><div><h2>Estudo walk-forward</h2><p className="muted">Compara treino, validação e out-of-sample por par, mantendo a ordem temporal.</p></div><span className="pill">SEM LOOK-AHEAD</span></div>
      <div className="controls">
        <label>Pares <input className="symbols-input" value={studySymbols} onChange={e=>setStudySymbols(e.target.value)} /></label>
        <label>Candles <select value={studyCandles} onChange={e=>setStudyCandles(Number(e.target.value))}><option value={1000}>1000</option><option value={1500}>1500</option><option value={2000}>2000</option></select></label>
        <label><input type="checkbox" checked={studyConservative} onChange={e=>setStudyConservative(e.target.checked)}/> Configuração conservadora</label>
        <button disabled={studyBusy} onClick={runStudy}>{studyBusy?'A estudar…':'Executar estudo'}</button>
      </div>
      {study && <><div className="study-summary"><span>Estratégia <strong>{study.strategy}</strong></span><span>Treino <strong>{study.trainFraction*100}%</strong></span><span>Validação <strong>{study.validationFraction*100}%</strong></span><span>Configuração <strong>{study.cells[0]?.configuration ?? '—'}</strong></span></div>
        <div className="tablewrap"><table><thead><tr><th>Par</th><th>Período</th><th>Candles</th><th>Retorno</th><th>Buy &amp; Hold</th><th>Drawdown</th><th>PF</th><th>Expectancy</th><th>Trades</th><th>Win rate</th></tr></thead><tbody>
          {study.cells.map((c,i)=><tr key={`${c.symbol}-${c.period}-${i}`}><td>{c.symbol}</td><td><span className={`period period-${c.period.toLowerCase()}`}>{c.period.replace('_',' ')}</span></td><td>{c.candles}</td><td className={c.netReturnPercent>=0?'positive':'negative'}>{formatMetric(c.netReturnPercent,2,'%')}</td><td>{formatMetric(c.buyHoldPercent,2,'%')}</td><td>{formatMetric(c.maxDrawdownPercent,2,'%')}</td><td>{formatMetric(c.profitFactor,2)}</td><td>{formatMetric(c.expectancy,2,' USDT')}</td><td>{c.completedTrades}</td><td>{formatMetric(c.winRatePercent,1,'%')}</td></tr>)}
        </tbody></table></div></>}
    </section>
    <section className="panel backtest-panel">
      <div className="panelhead"><div><h2>Backtesting histórico</h2><p className="muted">Configura uma hipótese, executa-a com custos realistas e guarda uma versão reproduzível.</p></div><span className="pill">SEM ORDENS REAIS</span></div>
      <div className="controls">
        <select value={symbol} onChange={e=>setSymbol(e.target.value)}>{['BTCUSDT','ETHUSDT','SOLUSDT','BNBUSDT'].map(s=><option key={s}>{s}</option>)}</select>
        <select value={strategy} onChange={e=>setStrategy(e.target.value)}><option value="DAY">Day (15m)</option><option value="SWING">Swing (4h)</option></select>
        <label>Candles <select value={candleCount} onChange={e=>setCandleCount(Number(e.target.value))}><option value={500}>500</option><option value={1000}>1000</option><option value={2000}>2000</option></select></label>
        <label>Risco <select value={riskPercent} onChange={e=>setRiskPercent(Number(e.target.value))}><option value={.25}>0,25%</option><option value={.5}>0,5%</option></select></label>
        <label>Fee <input type="number" min="0" step="0.0001" value={fee} onChange={e=>setFee(Number(e.target.value))}/></label>
        <label>Slippage <input type="number" min="0" step="0.0001" value={slippage} onChange={e=>setSlippage(Number(e.target.value))}/></label>
        <label>Distância EMA <input type="number" min="0" step="0.5" value={maxEmaDistancePercent} onChange={e=>setMaxEmaDistancePercent(Number(e.target.value))}/> %</label>
        {Object.entries(filters).map(([key,value])=><label key={key}><input type="checkbox" checked={value} onChange={e=>setFilters({...filters,[key]:e.target.checked})}/>{key.replace('Filter','')}</label>)}
        {filters.adxFilter && <label>ADX mínimo <input type="number" min="0" value={minAdx} onChange={e=>setMinAdx(Number(e.target.value))}/></label>}
        {filters.rsiFilter && <><label>RSI mín. <input type="number" min="0" max="100" value={minRsi} onChange={e=>setMinRsi(Number(e.target.value))}/></label><label>RSI máx. <input type="number" min="0" max="100" value={maxRsi} onChange={e=>setMaxRsi(Number(e.target.value))}/></label></>}
        <label>Trailing ATR <input type="number" min="0" step="0.1" value={trailingStopAtr} onChange={e=>setTrailingStopAtr(Number(e.target.value))}/></label>
        <label>Máx. candles <input type="number" min="0" value={maxHoldingCandles} onChange={e=>setMaxHoldingCandles(Number(e.target.value))}/></label>
        <button disabled={busy} onClick={runBacktest}>{busy?'A simular…':'Executar backtest'}</button>
      </div>
      {savedId !== null && <div className="run-meta"><span>Resultado guardado: <strong>#{savedId}</strong></span><span>Versão: <code>{runMeta?.strategyVersion ?? '—'}</code></span></div>}
      {report && <div className="metrics">
        <div>Equity final<strong>{formatMetric(report.finalEquity, 2, ' USDT')}</strong></div>
        <div>Retorno líquido<strong>{formatMetric(report.netReturnPercent, 2, '%')}</strong></div>
        <div>Buy & Hold<strong>{formatMetric(report.buyHoldPercent, 2, '%')}</strong></div>
        <div>Estratégia aleatória<strong>{formatMetric(report.randomBenchmarkPercent, 2, '%')}</strong></div>
        <div>Trades fechados<strong>{report.completedTrades}</strong></div>
        <div>Win rate<strong>{formatMetric(report.winRatePercent, 1, '%')}</strong></div>
        <div>Profit factor<strong>{formatMetric(report.profitFactor)}</strong><small>Sem perdas ou sem trades</small></div>
        <div>Max drawdown<strong>{formatMetric(report.maxDrawdownPercent, 2, '%')}</strong></div>
        <div>Comissões<strong>{formatMetric(report.totalFees, 2, ' USDT')}</strong></div>
        <div>Expectancy<strong>{formatMetric(report.expectancy, 2, ' USDT')}</strong></div>
        <div>Ganho / perda médio<strong>{formatMetric(report.averageGain, 2)} / {formatMetric(report.averageLoss, 2)}</strong></div>
        <div>Duração média<strong>{formatMetric(report.averageDurationSeconds / 3600, 1, ' h')}</strong></div>
        <div>Perdas consecutivas<strong>{report.maxConsecutiveLosses}</strong></div>
      </div>}
      {runMeta && <details className="config-details"><summary>Ver configuração persistida</summary><code>{runMeta.configuration}</code></details>}
    </section>
    <section className="panel"><div className="panelhead"><div><h2>Histórico de backtests guardados</h2><p className="muted">Últimas 50 simulações persistidas no PostgreSQL</p></div><button onClick={refreshHistory}>Atualizar</button></div>
      <div className="tablewrap"><table><thead><tr><th>ID</th><th>Data UTC</th><th>Par</th><th>Estratégia</th><th>Retorno</th><th>Benchmark aleatório</th><th>Expectancy</th><th>Trades</th><th>Drawdown</th></tr></thead><tbody>
      {history.map(h=><tr key={h.id}><td>#{h.id}</td><td>{new Date(h.createdAt).toLocaleString('pt-PT')}</td><td>{h.symbol}</td><td>{h.strategy}</td><td className={h.netReturnPercent>=0?'positive':'negative'}>{h.netReturnPercent.toFixed(2)}%</td><td>{h.randomBenchmarkPercent.toFixed(2)}%</td><td>{h.expectancy.toFixed(2)}</td><td>{h.completedTrades}</td><td>{h.maxDrawdownPercent.toFixed(2)}%</td></tr>)}
      {history.length===0 && <tr><td colSpan={9} className="muted">Ainda não existem backtests guardados.</td></tr>}
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
