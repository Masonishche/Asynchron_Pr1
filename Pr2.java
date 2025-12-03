import java.util.*;
import java.util.concurrent.*;

/**
 * Задача (Callable), яка шукає прості числа у заданому числовому діапазоні [start, end].
 */
class PrimeRangeTask implements Callable<List<Integer>> {
    private final int start;
    private final int end;
    private final String taskName;

    public PrimeRangeTask(int start, int end, int taskIndex) {
        this.start = start;
        this.end = end;
        this.taskName = "Потік-" + taskIndex;
    }

    @Override
    public List<Integer> call() throws Exception {
        List<Integer> primesInRange = new ArrayList<>();

        // Перебір чисел у довіреному діапазоні
        for (int i = start; i <= end; i++) {
            // Перевірка на скасування (вимога завдання)
            if (Thread.currentThread().isInterrupted()) {
                System.out.println(taskName + " перервано.");
                return primesInRange;
            }

            if (isPrime(i)) {
                primesInRange.add(i);
            }
        }
        return primesInRange;
    }

    // Метод перевірки числа на простоту
    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n == 2 || n == 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }
}

/**
 * Головний клас Pr2.
 * Реалізує асинхронний пошук простих чисел шляхом розбиття діапазону [0...N] на частини.
 */
public class Pr2 {

    // Потокобезпечний список для збору фінального результату
    private static final CopyOnWriteArrayList<Integer> totalResultList = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 1. Отримання N від користувача
        System.out.print("Введіть число N (кінець діапазону пошуку): ");
        int N = 0;
        try {
            N = scanner.nextInt();
        } catch (InputMismatchException e) {
            System.out.println("Помилка: введіть коректне ціле число.");
            return;
        }

        if (N < 0) {
            System.out.println("Число N має бути додатним.");
            return;
        }

        long startTime = System.currentTimeMillis();

        // 2. Конфігурація пулу потоків
        // Кількість потоків. Можна взяти 4 або за кількістю ядер: Runtime.getRuntime().availableProcessors()
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        System.out.println("Пошук простих чисел у діапазоні [0; " + N + "] у " + threadCount + " потоках...");

        // 3. Розбиття діапазону на під-діапазони
        List<Future<List<Integer>>> futures = new ArrayList<>();

        // Розмір одного шматка діапазону
        int rangeSize = N / threadCount;
        int currentStart = 0;

        for (int i = 0; i < threadCount; i++) {
            // Визначаємо кінець поточного діапазону
            // Якщо це останній потік, він бере все до кінця (щоб не загубити залишок від ділення)
            int currentEnd = (i == threadCount - 1) ? N : (currentStart + rangeSize);

            // Створюємо задачу для конкретного діапазону (наприклад: 0-250)
            PrimeRangeTask task = new PrimeRangeTask(currentStart, currentEnd, i + 1);

            // Віддаємо на виконання
            futures.add(executor.submit(task));

            // Зсуваємо початок для наступного потоку
            currentStart = currentEnd + 1;
        }

        // 4. Моніторинг виконання (вимога використати isDone / isCancelled)
        boolean allFinished = false;
        while (!allFinished) {
            allFinished = true;
            for (Future<List<Integer>> f : futures) {
                if (f.isCancelled()) {
                    // Логіка, якщо задачу скасовано
                } else if (!f.isDone()) {
                    allFinished = false; // Якщо хоча б один не готовий, чекаємо далі
                }
            }
            // Коротка пауза, щоб розвантажити процесор під час циклу перевірки
            try { Thread.sleep(10); } catch (InterruptedException e) { }
        }

        // 5. Збір результатів (сумуємо в один масив)
        try {
            for (Future<List<Integer>> f : futures) {
                // Отримуємо список від кожного потоку
                List<Integer> partialResult = f.get();
                // Додаємо в загальний CopyOnWriteArrayList
                totalResultList.addAll(partialResult);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        long endTime = System.currentTimeMillis();

        // 6. Вивід результатів
        // Сортуємо для красивого вигляду, бо потоки могли закінчити роботу в різний час
        List<Integer> sortedOutput = new ArrayList<>(totalResultList);
        Collections.sort(sortedOutput);

        System.out.println("--------------------------------------------------");
        System.out.println("Знайдено простих чисел: " + sortedOutput.size());
        if (sortedOutput.size() <= 100) {
            System.out.println("Список: " + sortedOutput);
        } else {
            System.out.println("Список (перші 20): " + sortedOutput.subList(0, 20) + " ...");
        }
        System.out.println("Час роботи програми: " + (endTime - startTime) + " мс.");
    }
}