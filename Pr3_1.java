import java.util.*;
import java.util.concurrent.*;

public class Pr3_1 {
    // Поріг кількості елементів, при якому задача перестає дробитися і виконується послідовно
    private static final int THRESHOLD = 200_000;

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        // Зчитування параметрів масиву від користувача
        System.out.print("Введіть кількість рядків (наприклад 5000): ");
        int rows = sc.nextInt();
        System.out.print("Введіть кількість стовпців (наприклад 5000): ");
        int cols = sc.nextInt();
        System.out.print("Мінімальне значення елементів: ");
        int minVal = sc.nextInt();
        System.out.print("Максимальне значення елементів: ");
        int maxVal = sc.nextInt();

        // Генерація двовимірного масиву з випадковими числами
        int[][] array = generateArray(rows, cols, minVal, maxVal);

        // Визначення умови пошуку: шукаємо мінімальне число, яке більше за (2 * array[0][0])
        long first = array[0][0];
        long target = 2 * first;

        System.out.printf("%nПерший елемент: %d (подвоєне значення = %d)%n", first, target);
        System.out.println("Масив згенеровано: " + rows + " × " + cols + " = " + (rows * (long)cols) + " елементів");

        // === Work Stealing (Використання Fork/Join Framework) ===
        // Цей підхід дозволяє вільним потокам "красти" задачі з черг завантажених потоків
        long start = System.nanoTime();
        Long resultStealing = forkJoinSearch(array, target);
        long timeStealing = System.nanoTime() - start;

        // === Work Dealing (Використання ExecutorService) ===
        // Цей підхід явно розподіляє (роздає) шматки роботи між потоками на початку
        start = System.nanoTime();
        Long resultDealing = executorSearch(array, target);
        long timeDealing = System.nanoTime() - start;

        // Виведення результатів
        System.out.println("=== РЕЗУЛЬТАТ ===");
        String text = (resultStealing == null)
                ? "Немає елементів, більших за подвоєне значення першого елемента"
                : "Знайдено мінімальний елемент = " + resultStealing;
        System.out.println(text);

        // Порівняння часу виконання
        System.out.printf("%nWork Stealing (Fork/Join):     %.3f мс%n", timeStealing / 1_000_000.0);
        System.out.printf("Work Dealing (ExecutorService): %.3f мс%n", timeDealing / 1_000_000.0);

        if (timeStealing < timeDealing) {
            System.out.printf("→ Work Stealing швидший на %.3f мс%n", (timeDealing - timeStealing) / 1_000_000.0);
        } else {
            System.out.printf("→ Work Dealing швидший на %.3f мс%n", (timeStealing - timeDealing) / 1_000_000.0);
        }
    }

    // Метод для заповнення масиву випадковими числами
    private static int[][] generateArray(int rows, int cols, int min, int max) {
        Random r = new Random();
        int[][] arr = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                arr[i][j] = r.nextInt(max - min + 1) + min;
        return arr;
    }

    // ====================== Work Stealing (Fork/Join) ======================

    // RecursiveTask використовується, коли задача повинна повернути результат
    static class MinTask extends RecursiveTask<Long> {
        private final int[][] array;
        private final int startRow, endRow;
        private final long target;

        MinTask(int[][] array, int startRow, int endRow, long target) {
            this.array = array;
            this.startRow = startRow;
            this.endRow = endRow;
            this.target = target;
        }

        @Override
        protected Long compute() {
            // Визначаємо кількість рядків для обробки в поточній підзадачі
            int rowsInPart = endRow - startRow;

            // Якщо задача достатньо мала (менше порогу), виконуємо її послідовно
            // Це базовий випадок рекурсії
            if (rowsInPart * array[0].length <= THRESHOLD) {
                return sequentialSearch(startRow, endRow);
            }

            // Якщо задача велика, ділимо її навпіл (Divide and Conquer)
            int mid = startRow + rowsInPart / 2;
            MinTask left = new MinTask(array, startRow, mid, target);
            MinTask right = new MinTask(array, mid, endRow, target);

            // fork() відправляє ліву частину задачі в чергу поточного потоку (асинхронно).
            // Інший вільний потік може "вкрасти" цю задачу.
            left.fork();

            // right.compute() виконує праву частину синхронно в поточному потоці
            Long rightRes = right.compute();

            // left.join() чекає завершення лівої задачі та отримує її результат
            Long leftRes = left.join();

            // Об'єднання результатів: вибираємо мінімальне з двох знайдених значень
            if (leftRes == null) return rightRes;
            if (rightRes == null) return leftRes;
            return Math.min(leftRes, rightRes);
        }

        // Послідовний пошук у заданому діапазоні рядків
        private Long sequentialSearch(int fromRow, int toRow) {
            long min = Long.MAX_VALUE;
            boolean found = false;
            for (int i = fromRow; i < toRow; i++) {
                for (int j = 0; j < array[0].length; j++) {
                    long val = array[i][j];
                    // Перевірка умови: елемент > target і менший за поточний мінімум
                    if (val > target && val < min) {
                        min = val;
                        found = true;
                    }
                }
            }
            return found ? min : null;
        }
    }

    // Запуск ForkJoinPool
    private static Long forkJoinSearch(int[][] array, long target) {
        // commonPool() використовує кількість потоків, рівну кількості ядер процесора - 1
        ForkJoinPool pool = ForkJoinPool.commonPool();
        return pool.invoke(new MinTask(array, 0, array.length, target));
    }

    // ====================== Work Dealing (ExecutorService) ======================

    private static Long executorSearch(int[][] array, long target) throws Exception {
        // Отримуємо кількість доступних ядер процесора
        int threads = Runtime.getRuntime().availableProcessors();

        // Створюємо пул з фіксованою кількістю потоків
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        List<Future<Long>> futures = new ArrayList<>();

        // Явно розбиваємо масив на рівні частини (Work Dealing)
        int rowsPerThread = array.length / threads;
        for (int i = 0; i < threads; i++) {
            int start = i * rowsPerThread;
            // Останній потік забирає всі залишкові рядки, якщо ділення не ціле
            int end = (i == threads - 1) ? array.length : start + rowsPerThread;

            // Створюємо задачу (Callable) і передаємо в пул
            futures.add(exec.submit(() -> sequentialSearch(array, start, end, target)));
        }

        Long globalMin = null;
        // Збираємо результати виконання всіх потоків
        for (Future<Long> f : futures) {
            // f.get() блокує поточний потік, поки результат не буде готовий
            Long res = f.get();
            if (res != null) {
                globalMin = (globalMin == null) ? res : Math.min(globalMin, res);
            }
        }

        // Завершуємо роботу пулу потоків
        exec.shutdown();
        return globalMin;
    }

    // Допоміжний метод для послідовного пошуку (використовується в Work Dealing)
    private static Long sequentialSearch(int[][] array, int startRow, int endRow, long target) {
        long min = Long.MAX_VALUE;
        boolean found = false;
        for (int i = startRow; i < endRow; i++) {
            for (int j = 0; j < array[0].length; j++) {
                long val = array[i][j];
                if (val > target && val < min) {
                    min = val;
                    found = true;
                }
            }
        }
        return found ? min : null;
    }
}