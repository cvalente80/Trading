export function formatMetric(value: number | null | undefined, digits = 2, suffix = ''): string {
  return value == null || !Number.isFinite(value) ? 'Indefinido' : `${value.toFixed(digits)}${suffix}`;
}

export function normalizeProfitFactor(value: unknown): number | null {
  if (typeof value === 'number') return Number.isFinite(value) ? value : null;
  if (value === 'Infinity' || value === '+Infinity') return null;
  const parsed = typeof value === 'string' ? Number(value) : NaN;
  return Number.isFinite(parsed) ? parsed : null;
}

export async function apiError(response: Response): Promise<string> {
  try {
    const body = await response.json() as {error?: unknown};
    if (typeof body.error === 'string' && body.error.length > 0) return body.error;
  } catch { /* fall back to HTTP status */ }
  return `HTTP ${response.status}`;
}
