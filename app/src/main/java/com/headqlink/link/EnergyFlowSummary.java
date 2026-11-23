package com.headqlink.link;

/**
 * Balance energético de un trayecto, separado en magnitudes no negativas.
 * <p>
 * IMPORTANTE: EnergyBreakdown proporciona un modelo físico. Ajustarlo al
 * consumo NETO de la nube no convierte la energía BRUTA consumida ni la
 * regenerada en mediciones de BMS. La interfaz debe mostrar la procedencia.
 */
final class EnergyFlowSummary {
    final double consumedKwh;
    final double recoveredKwh;
    /** Consumo neto: consumed - recovered; puede ser negativo al bajar. */
    final double netKwh;
    /** Energía recuperada / consumida * 100 (si consumed > 0). */
    final double recoveryPercent;
    /** Consumo bruto por 100 km. */
    final double consumedPer100;
    /** Recuperación por 100 km. */
    final double recoveredPer100;
    /** Neto por 100 km. */
    final double netPer100;
    final double distanceKm;
    final boolean ready;
    final String provenance;

    private EnergyFlowSummary(double consumed, double recovered, double km, boolean ready, String provenance) {
        this.consumedKwh = consumed;
        this.recoveredKwh = recovered;
        this.netKwh = ready ? consumed - recovered : Double.NaN;
        this.distanceKm = km;
        this.ready = ready;
        this.recoveryPercent = ready && consumed > 0 ? recovered / consumed * 100 : Double.NaN;
        this.consumedPer100 = ready && km > 0.1 ? consumed * 100 / km : Double.NaN;
        this.recoveredPer100 = ready && km > 0.1 ? recovered * 100 / km : Double.NaN;
        this.netPer100 = ready && km > 0.1 ? netKwh * 100 / km : Double.NaN;
        this.provenance = provenance;
    }

    /**
     * @param consumed energía gastada modelizada (kWh)
     * @param recovered energía recuperada modelizada (kWh)
     * @param km distancia del trayecto (km)
     * @param calibratedWithCloud true si el modelo ha sido ajustado al neto de nube
     */
    static EnergyFlowSummary of(double consumed, double recovered, double km, boolean calibratedWithCloud) {
        boolean valid = Double.isFinite(consumed) && Double.isFinite(recovered)
                && consumed >= 0 && recovered >= 0;
        boolean ready = valid && consumed + recovered >= 0.001;
        String provenance = calibratedWithCloud
                ? "ESTIMACIÓN: modelo ajustado con consumo neto del coche; gasto y regeneración no medidos"
                : "ESTIMACIÓN: modelo de movimiento y pérdidas; sin medición BMS";
        return new EnergyFlowSummary(ready ? consumed : Double.NaN,
                ready ? recovered : Double.NaN, km, ready, provenance);
    }
}
