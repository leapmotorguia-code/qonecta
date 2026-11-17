import com.leapmotorguia.c10nativa.PerformanceTimer;

public class PerformanceTimerCheck {
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        PerformanceTimer a = new PerformanceTimer(PerformanceTimer.Mode.FROM_STOP);
        a.accept(1000, 0); a.accept(2000, 0); a.accept(3000, 0); a.accept(4000, 0);
        require(a.getState() == PerformanceTimer.State.ARMED, "3 segundos detenido");
        a.accept(5000, 12); a.accept(6000, 50); a.accept(7000, 80); a.accept(8000, 100);
        require(a.elapsedAt(50) != null && a.elapsedAt(100) != null, "registra 50 y 100");
        require(a.elapsedAt(100) > a.elapsedAt(50), "tiempos ordenados");
        require(a.elapsedAt(100) == 3750, "interpolacion de arranque y final");

        PerformanceTimer b = new PerformanceTimer(PerformanceTimer.Mode.RECOVERY);
        b.accept(1000, 79); b.accept(2000, 79); b.accept(3000, 79); b.accept(4000, 79);
        require(b.getState() == PerformanceTimer.State.ARMED, "recuperacion armada");
        b.accept(5000, 81); b.accept(6000, 84); b.accept(7000, 100); b.accept(8000, 120);
        require(b.elapsedAt(120) != null && b.elapsedAt(120) == 3500, "salida interpolada a 80");

        PerformanceTimer missed = new PerformanceTimer(PerformanceTimer.Mode.RECOVERY);
        missed.accept(1000, 81); missed.accept(2000, 81); missed.accept(3000, 81); missed.accept(4000, 81);
        missed.accept(5000, 85);
        require(missed.getState() == PerformanceTimer.State.WAITING && missed.elapsedAt(120) == null, "no inventa paso por 80");

        PerformanceTimer interrupted = new PerformanceTimer(PerformanceTimer.Mode.FROM_STOP);
        interrupted.accept(1000, 0); interrupted.accept(2000, 0); interrupted.accept(3000, 0); interrupted.accept(4000, 0);
        interrupted.accept(5000, 10); interrupted.accept(9001, 100);
        require(interrupted.elapsedAt(100) == null && interrupted.getState() == PerformanceTimer.State.WAITING, "rechaza salto de señal");
        require(!interrupted.accept(10000, Double.NaN), "rechaza velocidad invalida");
        require(!interrupted.accept(10000, -2), "rechaza velocidad negativa");
        System.out.println("Mediciones GPS: pruebas superadas");
    }
}
