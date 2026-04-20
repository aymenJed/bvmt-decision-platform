package com.bvmt.decision.service.signal;

import com.bvmt.decision.entity.Instrument;
import com.bvmt.decision.entity.PriceDaily;
import com.bvmt.decision.entity.TradingSignal;
import com.bvmt.decision.service.indicator.RsiIndicator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class RsiOverboughtRule implements TradingRule {

    @Value("${bvmt.indicator.rsi-period:14}")
    private int period;
    @Value("${bvmt.indicator.rsi-overbought:70}")
    private int threshold;

    @Override public String code() { return "RSI_OVERBOUGHT"; }

    @Override public TradingSignal.SignalType signalType() {
        return TradingSignal.SignalType.SELL;
    }

    @Override
    public Optional<TradingSignal> evaluate(Instrument instrument, LocalDate asOf,
                                            List<PriceDaily> priceSeries) {
        RsiIndicator rsi = new RsiIndicator(period);
        if (priceSeries.size() < rsi.minRequiredBars()) return Optional.empty();

        List<BigDecimal> closes = priceSeries.stream().map(PriceDaily::getClosePrice).toList();
        var res = rsi.compute(closes);
        if (res == null) return Optional.empty();

        double rsiValue = res.value().doubleValue();
        if (rsiValue < threshold) return Optional.empty();

        TradingSignal.Strength strength;
        if      (rsiValue > 80) strength = TradingSignal.Strength.STRONG;
        else if (rsiValue > 75) strength = TradingSignal.Strength.MEDIUM;
        else                    strength = TradingSignal.Strength.WEAK;

        return Optional.of(TradingSignal.builder()
                .instrument(instrument)
                .signalDate(asOf)
                .signalType(TradingSignal.SignalType.SELL)
                .strength(strength)
                .ruleCode(code())
                .triggeringValue(res.value())
                .referencePrice(closes.get(closes.size() - 1))
                .rationale(String.format(
                        "RSI(%d) = %.2f ≥ seuil de surachat (%d) → prise de bénéfice recommandée",
                        period, rsiValue, threshold))
                .confidence(BigDecimal.valueOf(Math.min(100, (rsiValue - threshold) * 3 + 40)))
                .build());
    }
}
