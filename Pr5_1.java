import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Pr5_1 {

    public static void main(String[] args) {
        System.out.println("=== Завдання 1 (Pr5_1): Перегони потоків ===");

        // Створюємо асинхронні завдання з різним часом виконання
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> performTask("Пошук у базі А (повільно)", 3));
        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> performTask("Пошук у кеші (швидко)", 1));
        CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> performTask("Пошук у базі Б (середньо)", 2));

        // Використовуємо anyOf, щоб отримати результат першого завершеного завдання
        CompletableFuture<Object> winner = CompletableFuture.anyOf(task1, task2, task3);

        // Обробляємо результат переможця
        winner.thenAccept(result -> {
            System.out.println(">> Переміг потік: " + result);
        }).join(); // Чекаємо завершення (join), щоб main не закрився раніше часу

        System.out.println("=== Програма завершена ===");
    }

    // Допоміжний метод для імітації роботи
    private static String performTask(String name, int durationSeconds) {
        System.out.println("Start: " + name);
        try {
            TimeUnit.SECONDS.sleep(durationSeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return name + " (час: " + durationSeconds + "с)";
    }
}