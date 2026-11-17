package com.leapmotorguia.c10nativa;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CostCalculator {
    private CostCalculator() {}
    public static String calculate(String consumption, String price) {
        try {
            BigDecimal c = new BigDecimal(consumption.trim().replace(',', '.'));
            BigDecimal p = new BigDecimal(price.trim().replace(',', '.'));
            if (c.signum() < 0 || p.signum() < 0 || c.compareTo(new BigDecimal("10000")) > 0 || p.compareTo(new BigDecimal("10000")) > 0) return null;
            return c.multiply(p).setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}
