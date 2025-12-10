import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Pr5_2 {

    public static void main(String[] args) {
        System.out.println("=== Завдання 2 (Pr5_2): Система бронювання ===");

        // 1. Асинхронна перевірка наявності місць
        CompletableFuture<Boolean> checkAvailability = CompletableFuture.supplyAsync(() -> {
            System.out.println("[Booking] Перевірка наявності місць...");
            sleep(1500);
            return true; // Місця є
        });

        // 2. Асинхронний пошук найкращої ціни
        CompletableFuture<Double> findBestPrice = CompletableFuture.supplyAsync(() -> {
            System.out.println("[Booking] Пошук найкращої ціни...");
            sleep(1000);
            return 199.99; // Ціна знайдена
        });

        // 3. Об'єднання результатів (thenCombine)
        // Виконується тільки коли обидва попередні методи завершені
        CompletableFuture<TicketOrder> prepareOrder = checkAvailability.thenCombine(findBestPrice, (available, price) -> {
            if (available) {
                System.out.println("[Booking] Місця є, ціна актуальна. Формуємо замовлення.");
                return new TicketOrder("Kyiv-Lviv", price);
            } else {
                throw new RuntimeException("Місць немає!");
            }
        });

        // 4. Оплата (thenCompose)
        // Залежна операція: запускається тільки після успішного формування замовлення
        CompletableFuture<String> finalProcess = prepareOrder.thenCompose(order -> processPayment(order));

        // Виведення фінального результату
        finalProcess.handle((res, ex) -> {
            if (ex != null) {
                System.err.println("Помилка: " + ex.getMessage());
            } else {
                System.out.println(">> Результат: " + res);
            }
            return null;
        }).join(); // Чекаємо завершення всіх процесів

        System.out.println("=== Програма завершена ===");
    }

    // --- Допоміжні методи ---

    private static CompletableFuture<String> processPayment(TicketOrder order) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("[Payment] Оплата суми $" + order.price + "...");
            sleep(2000);
            return "Квиток на рейс " + order.route + " успішно оплачено!";
        });
    }

    private static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Внутрішній клас для даних замовлення
    static class TicketOrder {
        String route;
        double price;

        public TicketOrder(String route, double price) {
            this.route = route;
            this.price = price;
        }
    }
}