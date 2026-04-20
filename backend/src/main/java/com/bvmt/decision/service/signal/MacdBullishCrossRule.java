package com.bvmt.decision.service.signal;

import com.bvmt.decision.entity.Instrument;
import com.bvmt.decision.entity.PriceDaily;
import com.bvmt.decision.entity.TradingSignal;
import com.bvmt.decision.service.indicator.MacdIndicator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * MACD Bullish Cross : ligne MACD traverse la ligne signal vers le haut.
 *   MACD(j-1) <= Signal(j-1)  ET  MACD(j) > Signal(j)
 */
@Component
public class MacdBullishCrossRule implements TradingRule {

    @Value("${bvmt.indicator.ema-short:12}") private int fastPeriod;
    @Value("${bvmt.indicator.ema-long:26}")  private int slowPeriod;
    @Value("${bvmt.indicator.macd-signal:9}") private int signalPeriod;

    @Override public String code() { return "MACD_BULLISH_CROSS"; }

    @Override public TradingSignal.SignalType signalType() {
        return TradingSignal.SignalType.BUY;
    }

    @Override
    public Optional<TradingSignal> evaluate(Instrument instrument, LocalDate asOf,
                                            List<PriceDaily> priceSeries) {
        MacdIndicator macd = new MacdIndicator(fastPeriod, slowPeriod, signalPeriod);
        if (priceSeries.size() < macd.minRequiredBars() + 1) return Optional.empty();

        List<BigDecimal> closes = priceSeries.stream().map(PriceDaily::getClosePrice).toList();
        int n = closes.size();

        var now  = macd.compute(closes);
        var prev = macd.compute(closes.subList(0, n - 1));
        if (now == null || prev == null) return Optional.empty();

        BigDecimal macdNow  = now.value();
        BigDecimal sigNow   = (BigDecimal) now.meta().get("signal");
        BigDecimal macdPrev = prev.value();
        BigDecimal sigPrev  = (BigDecimal) prev.meta().get("signal");

        boolean crossedUp = macdPrev.compareTo(sigPrev) <= 0
                         && macdNow.compareTo(sigNow) > 0;
        if (!crossedUp) return Optional.empty();

        BigDecimal histogram = (BigDecimal) now.meta().get("histogram");

        return Optional.of(TradingSignal.builder()
                .instrument(instrument)
                .signalDate(asOf)
                .signalType(TradingSignal.SignalType.BUY)
                .strength(TradingSignal.Strength.MEDIUM)
                .ruleCode(code())
                .triggeringValue(macdNow)
                .referencePrice(closes.get(n - 1))
                .rationale(String.format(
                        "MACD(%s) a croisé sa ligne signal (%s) vers le haut, histogramme=%s",
                        macdNow, sigNow, histogram))
                .confidence(new BigDecimal("70.00"))
                .build());
    }
}
