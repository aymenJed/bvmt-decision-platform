package com.bvmt.decision.service.signal;

import com.bvmt.decision.entity.Instrument;
import com.bvmt.decision.entity.PriceDaily;
import com.bvmt.decision.entity.TradingSignal;
import com.bvmt.decision.service.indicator.SmaIndicator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Death Cross : SMA courte traverse SMA longue par le BAS → signal de vente.
 */
@Component
public class SmaDeathCrossRule implements TradingRule {

    @Value("${bvmt.indicator.sma-short:20}") private int shortPeriod;
    @Value("${bvmt.indicator.sma-long:50}")  private int longPeriod;

    @Override public String code() { return "SMA_DEATH_CROSS"; }

    @Override public TradingSignal.SignalType signalType() {
        return TradingSignal.SignalType.SELL;
    }

    @Override
    public Optional<TradingSignal> evaluate(Instrument instrument, LocalDate asOf,
                                            List<PriceDaily> priceSeries) {
        if (priceSeries.size() < longPeriod + 1) return Optional.empty();

        List<BigDecimal> closes = priceSeries.stream().map(PriceDaily::getClosePrice).toList();
        int n = closes.size();

        BigDecimal smaShortNow  = new SmaIndicator(shortPeriod).compute(closes).value();
        BigDecimal smaLongNow   = new SmaIndicator(longPeriod).compute(closes).value();
        BigDecimal smaShortPrev = new SmaIndicator(shortPeriod).compute(closes.subList(0, n - 1)).value();
        BigDecimal smaLongPrev  = new SmaIndicator(longPeriod).compute(closes.subList(0, n - 1)).value();

        boolean crossedDown = smaShortPrev.compareTo(smaLongPrev) >= 0
                           && smaShortNow.compareTo(smaLongNow)   < 0;
        if (!crossedDown) return Optional.empty();

        return Optional.of(TradingSignal.builder()
                .instrument(instrument)
                .signalDate(asOf)
                .signalType(TradingSignal.SignalType.SELL)
                .strength(TradingSignal.Strength.STRONG)
                .ruleCode(code())
                .triggeringValue(smaShortNow)
                .referencePrice(closes.get(n - 1))
                .rationale(String.format(
                        "Death Cross : SMA(%d)=%s vient de passer sous SMA(%d)=%s",
                        shortPeriod, smaShortNow, longPeriod, smaLongNow))
                .confidence(new BigDecimal("75.00"))
                .build());
    }
}
