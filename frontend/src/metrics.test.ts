import {describe, expect, it} from 'vitest';
import {formatMetric, normalizeProfitFactor} from './metrics';

describe('metric presentation', () => {
  it('formats finite metrics with the requested precision', () => {
    expect(formatMetric(12.3456, 2, '%')).toBe('12.35%');
  });

  it('renders undefined and non-finite metrics safely', () => {
    expect(formatMetric(null)).toBe('Indefinido');
    expect(formatMetric(Number.POSITIVE_INFINITY)).toBe('Indefinido');
    expect(formatMetric(Number.NaN)).toBe('Indefinido');
  });

  it('normalizes JSON-safe and legacy infinite profit factors', () => {
    expect(normalizeProfitFactor(2.5)).toBe(2.5);
    expect(normalizeProfitFactor('Infinity')).toBeNull();
    expect(normalizeProfitFactor(null)).toBeNull();
  });
});
