import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Pr3_2 {
    private static final Set<String> TEXT_EXT = Set.of(
            ".txt",".java",".kt",".cpp",".h",".md",".html",".css",".js",".json",".xml",".log",".csv"
    );

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Введіть шлях до директорії: ");
        Path dir = Path.of(sc.nextLine().trim());

        if (!Files.isDirectory(dir)) {
            System.out.println("Помилка: це не директорія!");
            return;
        }

        System.out.println("\nСканування розпочато (Work Stealing)...\n");
        long start = System.nanoTime();

        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new ScanTask(dir));

        long time = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("\nСканування завершено за %d мс%n", time);
    }

    static class ScanTask extends RecursiveAction {
        private final Path dir;

        ScanTask(Path dir) { this.dir = dir; }

        @Override
        protected void compute() {
            List<ScanTask> subtasks = new ArrayList<>();

            try (var stream = Files.list(dir)) {
                stream.forEach(path -> {
                    if (Files.isDirectory(path)) {
                        ScanTask sub = new ScanTask(path);
                        sub.fork();
                        subtasks.add(sub);
                    } else if (isTextFile(path)) {
                        long count = countChars(path);
                        System.out.printf("%s → %d символів%n", path.getFileName(), count);
                    }
                });
            } catch (IOException e) {
                System.err.println("Помилка доступу: " + dir);
            }

            for (ScanTask task : subtasks) task.join();
        }

        private boolean isTextFile(Path path) {
            String name = path.toString().toLowerCase();
            return TEXT_EXT.stream().anyMatch(name::endsWith);
        }

        private long countChars(Path path) {
            try {
                return Files.readString(path).length();
            } catch (Exception e) {
                return 0;
            }
        }
    }
}