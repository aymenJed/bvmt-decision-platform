export type InstrumentType = 'EQUITY' | 'BOND' | 'SICAV' | 'FCP' | 'INDEX';

export interface Instrument {
  id: number;
  isin: string;
  ticker: string;
  name: string;
  instrumentType: InstrumentType;
  sector?: string;
  market: string;
  currency: string;
  listingDate?: string;
  nominalValue?: number;
  active: boolean;
}
