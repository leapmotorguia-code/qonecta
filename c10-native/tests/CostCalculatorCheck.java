import com.leapmotorguia.c10nativa.CostCalculator;

public class CostCalculatorCheck {
    private static void check(String c, String p, String expected) {
        String actual = CostCalculator.calculate(c, p);
        if (expected == null ? actual != null : !expected.equals(actual)) throw new AssertionError(c + ", " + p + " => " + actual);
    }
    public static void main(String[] args) {
        check("20", "0,25", "5,00");
        check("5.5", "1.50", "8,25");
        check("0", "3", "0,00");
        check("-1", "3", null);
        check("", "3", null);
        check("NaN", "3", null);
        check("10001", "3", null);
        System.out.println("Costes: pruebas superadas");
    }
}
