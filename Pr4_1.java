import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Pr4_1 {
    public static void main(String[] args) throws Exception {
        CompletableFuture<Void> runDemo = CompletableFuture.runAsync(() -> {
            System.out.println("\n>>> runAsync() виконується (потік: " +
                    Thread.currentThread().getName() + ")");
        });

        System.out.println("\n=== ЗАПУСК ОСНОВНОГО ЛАНЦЮЖКА ===");

        CompletableFuture<Void> chain = CompletableFuture
                .supplyAsync(() -> {
                    long start = System.nanoTime();
                    char[] array = generateRandomChars(20000);
                    long timeMs = (System.nanoTime() - start) / 1_000_000;
                    System.out.println("[supplyAsync] Генерація масиву завершена за " + timeMs + " мс");
                    System.out.println("Початковий масив (20000 символів): " + new String(array));
                    return array;
                })
                .thenApplyAsync(array -> {
                    long start = System.nanoTime();
                    StringBuilder letters = new StringBuilder();
                    StringBuilder whitespaces = new StringBuilder();
                    StringBuilder others = new StringBuilder();

                    for (char ch : array) {
                        if (Character.isLetter(ch)) {
                            letters.append(ch);
                        } else if (ch == ' ' || ch == '\t') {
                            whitespaces.append(ch);
                        } else {
                            others.append(ch);
                        }
                    }
                    long timeMs = (System.nanoTime() - start) / 1_000_000;
                    System.out.println("[thenApplyAsync] Класифікація завершена за " + timeMs + " мс");
                    return new ClassificationResult(
                            letters.toString(), whitespaces.toString(), others.toString());
                })
                .thenAcceptAsync(result -> {
                    long start = System.nanoTime();
                    System.out.println("Результати класифікації:");
                    System.out.println("  • Алфавітні символи     : " +
                            (result.letters.isEmpty() ? "немає" : result.letters));
                    System.out.println("  • Пробіли/табуляції     : " +
                            (result.whitespaces.isEmpty() ? "немає" : "'" + result.whitespaces + "'"));
                    System.out.println("  • Інші символи          : " +
                            (result.others.isEmpty() ? "немає" : result.others));
                    long timeMs = (System.nanoTime() - start) / 1_000_000;
                    System.out.println("[thenAcceptAsync] Виведення результатів завершено за " + timeMs + " мс");
                })
                .thenRunAsync(() -> {
                    System.out.println("\n>>> thenRunAsync() — весь ланцюжок завершено (потік: " +
                            Thread.currentThread().getName() + ")\n");
                });

        chain.join();
        runDemo.join();
        System.out.println("=== ПРОГРАМА ЗАВЕРШЕНА ===");
    }

    private static char[] generateRandomChars(int size) {
        Random r = new Random();
        char[] arr = new char[size];
        for (int i = 0; i < size; i++) {
            int type = r.nextInt(5);
            if (type == 0) arr[i] = ' ';
            else if (type == 1) arr[i] = '\t';
            else if (type == 2) arr[i] = (char)('0' + r.nextInt(10));
            else if (type == 3) arr[i] = (char)('A' + r.nextInt(52));
            else arr[i] = (char)(33 + r.nextInt(94));
        }
        return arr;
    }

    record ClassificationResult(String letters, String whitespaces, String others) {}
}