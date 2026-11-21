import {useMemo, useState} from 'react';
import {formatMetric} from './metrics';

export type Candle = {openTime:string; open:number; high:number; low:number; close:number};
export type Operation = {
  entryIndex:number; exitIndex:number; entryTime:string; exitTime:string;
  entry:number; stopLoss:number; takeProfit:number; exit:number; quantity:number;
  entryFee:number; exitFee:number; netPnl:number; reason:string;
  durationSeconds:number;
};

type Props = {trades:Operation[]; candles:Candle[]};

function duration(seconds:number) {
  const minutes = Math.floor(seconds / 60);
  return minutes < 60 ? `${minutes}m` : `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
}

function OperationChart({trades, candles, selected, onSelect}:{trades:Operation[]; candles:Candle[]; selected:number; onSelect:(index:number)=>void}) {
  const width = 1100, height = 360, pad = 28;
  const values = candles.flatMap(c => [c.high, c.low]);
  const active = trades[selected];
  if (!candles.length || !values.length) return <p className="muted">Candles indisponíveis para o gráfico.</p>;
  const min = Math.min(...values, ...(active ? [active.stopLoss, active.takeProfit] : []));
  const max = Math.max(...values, ...(active ? [active.stopLoss, active.takeProfit] : []));
  const y = (price:number) => pad + (max - price) / Math.max(max - min, Number.EPSILON) * (height - pad * 2);
  const x = (index:number) => pad + index / Math.max(candles.length - 1, 1) * (width - pad * 2);
  const candleWidth = Math.max(1.5, Math.min(8, (width - pad * 2) / candles.length * .65));
  return <svg className="candle-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Gráfico de candles e operações">
    {active && <>
      <line x1={pad} x2={width-pad} y1={y(active.stopLoss)} y2={y(active.stopLoss)} className="chart-stop" />
      <line x1={pad} x2={width-pad} y1={y(active.takeProfit)} y2={y(active.takeProfit)} className="chart-target" />
    </>}
    {candles.map((c,i) => { const up=c.close>=c.open; return <g key={c.openTime}>
      <line x1={x(i)} x2={x(i)} y1={y(c.high)} y2={y(c.low)} className={up?'candle-up':'candle-down'} />
      <rect x={x(i)-candleWidth/2} y={y(Math.max(c.open,c.close))} width={candleWidth}
        height={Math.max(1, Math.abs(y(c.open)-y(c.close)))} className={up?'candle-up':'candle-down'} />
    </g>; })}
    {trades.map((t,i) => <g key={`${t.entryIndex}-${t.exitIndex}`} onClick={() => onSelect(i)} className="trade-marker">
      <circle cx={x(t.entryIndex)} cy={y(t.entry)} r={i===selected?5:3} className="entry-marker" />
      <circle cx={x(t.exitIndex)} cy={y(t.exit)} r={i===selected?5:3} className="exit-marker" />
    </g>)}
  </svg>;
}

export default function OperationAnalysis({trades, candles}:Props) {
  const [selected, setSelected] = useState(0);
  const [filter, setFilter] = useState('ALL');
  const filtered = useMemo(() => trades.filter(t => filter === 'ALL' || t.reason === filter), [trades, filter]);
  const active = filtered[selected] ?? filtered[0];
  const activeIndex = active ? trades.indexOf(active) : 0;
  return <section className="panel operation-analysis">
    <div className="panelhead"><div><h2>Análise de Operações</h2><p className="muted">Investigue a execução, custos e contexto visual de cada trade.</p></div>
      <select value={filter} onChange={e=>{setFilter(e.target.value);setSelected(0);}}><option value="ALL">Todas as saídas</option>{[...new Set(trades.map(t=>t.reason))].map(r=><option key={r} value={r}>{r}</option>)}</select>
    </div>
    {!filtered.length ? <p className="muted">Não existem operações fechadas para analisar.</p> : <>
      <div className="tablewrap"><table><thead><tr><th>Entrada</th><th>Saída</th><th>Entrada</th><th>SL</th><th>TP</th><th>Saída</th><th>Motivo</th><th>P&amp;L líquido</th><th>Comissões</th><th>Duração</th></tr></thead><tbody>
        {filtered.map((t,i)=><tr key={`${t.entryIndex}-${t.exitIndex}`} className={i===selected?'selected-row':''} onClick={()=>setSelected(i)}>
          <td>{new Date(t.entryTime).toLocaleString('pt-PT')}</td><td>{new Date(t.exitTime).toLocaleString('pt-PT')}</td>
          <td>{t.entry.toFixed(4)}</td><td>{t.stopLoss.toFixed(4)}</td><td>{t.takeProfit.toFixed(4)}</td><td>{t.exit.toFixed(4)}</td><td>{t.reason}</td>
          <td className={t.netPnl>=0?'positive':'negative'}>{formatMetric(t.netPnl,2,' USDT')}</td><td>{formatMetric(t.entryFee+t.exitFee,2,' USDT')}</td><td>{duration(t.durationSeconds)}</td>
        </tr>)}
      </tbody></table></div>
      <OperationChart trades={filtered} candles={candles} selected={selected} onSelect={setSelected} />
      {active && <p className="muted chart-caption">Trade selecionado: entrada {active.entryIndex}, saída {active.exitIndex}. SL e TP são os níveis definidos no momento da entrada.</p>}
    </>}
  </section>;
}
