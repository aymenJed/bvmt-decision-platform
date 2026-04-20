export interface PriceBar {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  variationPct: number;
}

export type SignalType = 'BUY' | 'SELL' | 'HOLD';
export type SignalStrength = 'WEAK' | 'MEDIUM' | 'STRONG';

export interface TradingSignal {
  id: number;
  instrumentId: number;
  ticker: string;
  name: string;
  type: SignalType;
  strength: SignalStrength;
  ruleCode: string;
  triggeringValue?: number;
  referencePrice: number;
  rationale: string;
  confidence: number;
  signalDate: string;
  createdAt: string;
}

export interface SignalNotification {
  id: number;
  ticker: string;
  name: string;
  type: SignalType;
  strength: SignalStrength;
  ruleCode: string;
  price: number;
  rationale: string;
  confidence: number;
  signalDate: string;
  publishedAt: string;
}

export interface RsiAlert {
  instrumentId: number;
  ticker: string;
  name: string;
  level: 'OVERSOLD' | 'OVERBOUGHT';
  rsiValue: number;
  closePrice: number;
  tradeDate: string;
  publishedAt: string;
}

export interface IndicatorPoint {
  date: string;
  code: string;
  value: number;
  meta?: Record<string, any>;
}
