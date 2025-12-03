import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Pr4_2 {
    public static void main(String[] args) throws Exception {
        long totalStart = System.nanoTime();

        CompletableFuture.supplyAsync(() -> {
                    double[] seq = new double[20];
                    Random r = new Random();
                    for (int i = 0; i < 20; i++) {
                        seq[i] = r.nextDouble(-100, 100);
                    }
                    System.out.println("\n[supplyAsync] Генерація послідовності завершена");
                    System.out.println("Послідовність (20 елементів): " + Arrays.toString(seq));
                    return seq;
                })
                .thenApplyAsync(seq -> {
                    double maxDiff = Double.NEGATIVE_INFINITY;
                    for (int i = 0; i < seq.length - 1; i++) {
                        double diff = Math.abs(seq[i] - seq[i + 1]);
                        if (diff > maxDiff) maxDiff = diff;
                    }
                    System.out.println("\n[thenApplyAsync] Обчислення максимальної різниці завершено");
                    System.out.println("Максимальна різниця між сусідніми елементами: " + String.format("%.6f", maxDiff));
                    return maxDiff;
                })
                .thenAcceptAsync(result -> {
                    System.out.println("\n[thenAcceptAsync] Фінальний результат:");
                    System.out.println("max(|aᵢ - aᵢ₊₁|) = " + String.format("%.6f", result));
                })
                .thenRunAsync(() -> {
                    long totalTimeMs = (System.nanoTime() - totalStart) / 1_000_000;
                    System.out.println("\n[thenRunAsync] Час роботи усіх асинхронних операцій: " + totalTimeMs + " мс");
                })
                .join();
    }
}