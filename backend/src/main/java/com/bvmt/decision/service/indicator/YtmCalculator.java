package com.bvmt.decision.service.indicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Yield to Maturity (taux de rendement actuariel) pour obligations à coupons fixes.
 *
 * Spécifique au marché obligataire tunisien (BTA = Bons du Trésor Assimilables,
 * BTC = Bons du Trésor à Court terme). On résout par Newton-Raphson :
 *
 *   Prix = Σ [ C / (1+y)^t_i ] + [ FV / (1+y)^T ]
 *
 * où C = coupon annuel, FV = valeur nominale, T = maturité en années.
 *
 * Utilitaire statique — pas un Indicator au sens OHLCV.
 */
public final class YtmCalculator {

    private static final int MAX_ITERATIONS = 100;
    private static final double TOLERANCE   = 1e-8;

    private YtmCalculator() {}

    /**
     * @param dirtyPrice    prix plein (cours + coupon couru), en unités monétaires
     * @param faceValue     valeur nominale (généralement 1000 TND pour les BTA)
     * @param couponRate    taux de coupon annuel (ex: 0.0750 pour 7.50%)
     * @param couponsPerYear fréquence annuelle (1, 2, 4)
     * @param settlementDate date de règlement (typiquement J+3)
     * @param maturityDate   date d'échéance
     * @return YTM annualisé (ex: 0.0812 pour 8.12%), ou NaN si non convergé
     */
    public static BigDecimal computeYtm(BigDecimal dirtyPrice,
                                        BigDecimal faceValue,
                                        BigDecimal couponRate,
                                        int couponsPerYear,
                                        LocalDate settlementDate,
                                        LocalDate maturityDate) {
        double price    = dirtyPrice.doubleValue();
        double fv       = faceValue.doubleValue();
        double coupon   = fv * couponRate.doubleValue() / couponsPerYear;
        double years    = ChronoUnit.DAYS.between(settlementDate, maturityDate) / 365.25;
        int nbCoupons   = (int) Math.ceil(years * couponsPerYear);
        if (nbCoupons <= 0) return null;

        // Estimation initiale : rendement courant
        double y = couponRate.doubleValue();

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            double pv = 0d;
            double dPv = 0d;          // dérivée (pour Newton)
            for (int i = 1; i <= nbCoupons; i++) {
                double t = (double) i / couponsPerYear;
                double discount = Math.pow(1 + y / couponsPerYear, i);
                pv  += coupon / discount;
                dPv -= i * coupon / (couponsPerYear * Math.pow(1 + y / couponsPerYear, i + 1));
            }
            // Flux final = remboursement du nominal
            double discountFinal = Math.pow(1 + y / couponsPerYear, nbCoupons);
            pv  += fv / discountFinal;
            dPv -= nbCoupons * fv / (couponsPerYear * Math.pow(1 + y / couponsPerYear, nbCoupons + 1));

            double diff = pv - price;
            if (Math.abs(diff) < TOLERANCE) {
                return BigDecimal.valueOf(y).setScale(6, RoundingMode.HALF_UP);
            }
            if (dPv == 0) return null;   // évite division par zéro
            y = y - diff / dPv;
            if (y < -0.99) y = -0.99;    // sécurité numérique
        }
        return null; // non convergé
    }
}
