# Multithreaded File Search Utility

A high-performance command-line utility built in Java that searches for keywords across multiple files concurrently using Java Concurrency (`ExecutorService`, `Callable`, and `Future`).

---

## Features

- **Concurrent Processing**: Distributes file-level searches across a pool of worker threads.
- **Recursive File Discovery**: Traverses directories and subdirectories using `java.nio.file.Files.walk()`.
- **Case-Insensitive Matching**: Matches keywords regardless of letter casing.
- **Detailed Match Reporting**: Outputs matching filenames, line numbers, and line contents.
- **Robust Error Handling**: Safely handles unreadable files and unexpected I/O errors without terminating the search.
- **Graceful Lifecycle Management**: Shuts down executor thread pools reliably using `try-finally`.

---

## Project Structure

```
.
├── MultiThreadFileSearch.java   # Main Java source code
├── README.md                    # Project documentation
└── documents/                   # Sample search directory
    ├── file1.txt
    ├── file2.txt
    ├── file3.txt
    ├── file4.txt
    └── file5.txt
```

---

## Prerequisites

- **Java Development Kit (JDK)**: Version 8 or higher (JDK 11+ recommended).
- Verify installation by running:
  ```bash
  javac -version
  java -version
  ```

---

## Getting Started

### 1. Compile the Program

Open a terminal in the project directory and run:

```bash
javac MultiThreadFileSearch.java
```

### 2. Run with Default Settings

By default, the program searches for `"Java"` inside the `documents` folder using `4` worker threads:

```bash
java MultiThreadFileSearch
```

### 3. Run with Custom Arguments

Pass custom command-line arguments in the following order:
1. `directory`: Path to directory to scan.
2. `keyword`: Search term.
3. `workers`: Number of worker threads.

```bash
# Example: Search for "database" in documents using 2 workers
java MultiThreadFileSearch "documents" "database" 2

# Example: Search for "exception" in an absolute folder path using 6 workers
java MultiThreadFileSearch "C:\path\to\documents" "exception" 6
```

---

## Example Output

```text
File: documents\file1.txt
  Line 1: Welcome to the Java file search demo.
  Line 2: Java is a versatile object-oriented language.
  Line 3: Concurrency in Java makes processing faster.

File: documents\file3.txt
  Line 1: Java Virtual Machine (JVM) executes Java bytecode.
  Line 2: Enterprise applications rely heavily on Java frameworks like Spring.
  Line 3: Learning Java opens up many backend engineering opportunities.

File: documents\file5.txt
  Line 1: Multithreading in Java allows multiple threads of execution.
  Line 2: The java.util.concurrent package provides powerful high-level concurrency utilities.
  Line 3: ExecutorService simplifies thread lifecycle management in modern java applications.

Files scanned: 5
Total matching lines: 9
```

---

## Architecture & How Concurrency Works

```
Main Thread
 │
 ├── 1. Discovers files recursively (Files.walk)
 ├── 2. Creates ExecutorService (FixedThreadPool)
 ├── 3. Submits FileSearchTask per file -> Future<SearchResult>
 │
 ├── Worker Pool (Thread 1, Thread 2, ... Thread N)
 │    └── Each worker processes assigned file line-by-line
 │
 └── 4. Main Thread collects results (Future.get()) and prints summary
```

### Key Concurrency Concepts

| Class / Method | Description |
|---|---|
| `ExecutorService` | Manages thread lifecycle and thread pool allocation. |
| `Executors.newFixedThreadPool(n)` | Creates an active pool capped at `n` threads. |
| `Callable<SearchResult>` | Task abstraction allowing workers to return search results or errors. |
| `executor.submit(task)` | Dispatches task to worker pool and returns a `Future`. |
| `Future.get()` | Blocks until the worker completes its file search and retrieves the result. |
| `executor.shutdownNow()` | Stops accepting tasks and requests interruption for active tasks. |

---

## Complexity Analysis

- **Time Complexity**: $\mathcal{O}(B)$ where $B$ is the total byte size across all files scanned, divided across $W$ worker threads (subject to disk I/O throughput and CPU cores).
- **Auxiliary Space**: $\mathcal{O}(F + M)$ where $F$ is the number of files (futures and paths in memory) and $M$ is the number of matched lines collected.
