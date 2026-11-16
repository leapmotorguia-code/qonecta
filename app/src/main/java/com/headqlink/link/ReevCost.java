package com.headqlink.link;

/** Cálculo local a partir de datos que introduce la persona usuaria. */
final class ReevCost {
    private ReevCost() { }
    static double total(double kwh100, double litres100, double priceKwh, double priceLitre, double km) {
        double[] values = {kwh100, litres100, priceKwh, priceLitre, km};
        for (double v : values) if (Double.isNaN(v) || Double.isInfinite(v) || v < 0 || v > 100000) return Double.NaN;
        return (kwh100 * priceKwh + litres100 * priceLitre) * km / 100;
    }
}
