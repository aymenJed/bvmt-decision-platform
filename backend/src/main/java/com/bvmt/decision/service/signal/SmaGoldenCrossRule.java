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
 * Règle Golden Cross : la SMA courte traverse la SMA longue par le HAUT entre
 * la barre précédente et la barre courante → signal d'achat.
 *
 * Conditions :
 *   SMA_court(j-1) <= SMA_long(j-1)  ET  SMA_court(j) > SMA_long(j)
 */
@Component
public class SmaGoldenCrossRule implements TradingRule {

    @Value("${bvmt.indicator.sma-short:20}") private int shortPeriod;
    @Value("${bvmt.indicator.sma-long:50}")  private int longPeriod;

    @Override public String code() { return "SMA_GOLDEN_CROSS"; }

    @Override public TradingSignal.SignalType signalType() {
        return TradingSignal.SignalType.BUY;
    }

    @Override
    public Optional<TradingSignal> evaluate(Instrument instrument, LocalDate asOf,
                                            List<PriceDaily> priceSeries) {
        if (priceSeries.size() < longPeriod + 1) return Optional.empty();

        List<BigDecimal> closes = priceSeries.stream().map(PriceDaily::getClosePrice).toList();
        int n = closes.size();

        // Calcul SMA j et j-1
        BigDecimal smaShortNow  = new SmaIndicator(shortPeriod).compute(closes).value();
        BigDecimal smaLongNow   = new SmaIndicator(longPeriod).compute(closes).value();
        BigDecimal smaShortPrev = new SmaIndicator(shortPeriod).compute(closes.subList(0, n - 1)).value();
        BigDecimal smaLongPrev  = new SmaIndicator(longPeriod).compute(closes.subList(0, n - 1)).value();

        boolean crossedUp = smaShortPrev.compareTo(smaLongPrev) <= 0
                         && smaShortNow.compareTo(smaLongNow)   > 0;
        if (!crossedUp) return Optional.empty();

        return Optional.of(TradingSignal.builder()
                .instrument(instrument)
                .signalDate(asOf)
                .signalType(TradingSignal.SignalType.BUY)
                .strength(TradingSignal.Strength.STRONG)
                .ruleCode(code())
                .triggeringValue(smaShortNow)
                .referencePrice(closes.get(n - 1))
                .rationale(String.format(
                        "Golden Cross : SMA(%d)=%s vient de passer au-dessus de SMA(%d)=%s",
                        shortPeriod, smaShortNow, longPeriod, smaLongNow))
                .confidence(new BigDecimal("75.00"))
                .build());
    }
}
