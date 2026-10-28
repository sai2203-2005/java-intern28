import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class MultiThreadFileSearch {

    static class SearchResult {
        Path file;
        List<String> matches = new ArrayList<>();
        String error;

        SearchResult(Path file) {
            this.file = file;
        }
    }

    // One task searches one file.
    static class FileSearchTask implements Callable<SearchResult> {
        private final Path file;
        private final String keyword;

        FileSearchTask(Path file, String keyword) {
            this.file = file;
            this.keyword = keyword.toLowerCase(Locale.ROOT);
        }

        @Override
        public SearchResult call() {
            SearchResult result = new SearchResult(file);

            try (BufferedReader reader = Files.newBufferedReader(file)) {
                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;

                    if (line.toLowerCase(Locale.ROOT)
                            .contains(keyword)) {
                        result.matches.add(
                            "Line " + lineNumber + ": " + line
                        );
                    }
                }
            } catch (IOException e) {
                result.error = e.getMessage();
            }

            return result;
        }
    }

    public static void search(
            String directory,
            String keyword,
            int workerCount) throws IOException {

        if (keyword == null || keyword.isEmpty()
                || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Keyword cannot be empty."
            );
        }

        if (workerCount <= 0) {
            throw new IllegalArgumentException(
                "Worker count must be greater than zero."
            );
        }

        Path root = Paths.get(directory);

        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException(
                "Invalid directory: " + root
            );
        }

        List<Path> files = new ArrayList<>();

        // Find files recursively inside the directory.
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                 .forEach(files::add);
        }

        ExecutorService executor =
                Executors.newFixedThreadPool(workerCount);

        List<Future<SearchResult>> futures = new ArrayList<>();

        try {
            // Submit one task per file.
            for (Path file : files) {
                futures.add(
                    executor.submit(
                        new FileSearchTask(file, keyword)
                    )
                );
            }

            int totalMatches = 0;

            // Collect the results.
            for (Future<SearchResult> future : futures) {
                try {
                    SearchResult result = future.get();

                    if (result.error != null) {
                        System.out.println(
                            "Could not read " + result.file
                            + ": " + result.error
                        );
                        continue;
                    }

                    if (!result.matches.isEmpty()) {
                        System.out.println(
                            "\nFile: " + result.file
                        );

                        for (String match : result.matches) {
                            System.out.println("  " + match);
                        }

                        totalMatches += result.matches.size();
                    }
                } catch (ExecutionException e) {
                    System.out.println(
                        "A search task failed: " + e.getCause()
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Search was interrupted.");
                    return;
                }
            }

            System.out.println("\nFiles scanned: " + files.size());
            System.out.println("Total matching lines: "
                    + totalMatches);

        } finally {
            executor.shutdownNow();
        }
    }

    public static void main(String[] args) throws IOException {
        String directory = args.length > 0
                ? args[0] : "documents";

        String keyword = args.length > 1
                ? args[1] : "Java";

        int workers = args.length > 2
                ? Integer.parseInt(args[2]) : 4;

        search(directory, keyword, workers);
    }
}
