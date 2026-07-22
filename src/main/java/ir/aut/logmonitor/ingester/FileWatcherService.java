package ir.aut.logmonitor.ingester;

import ir.aut.logmonitor.common.model.LogEntry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * Watches the configured log folder for log files, parses them line by line,
 * publishes each parsed entry to Kafka, and deletes the file once fully
 * processed and confirmed sent.
 *
 * On startup, any pre-existing files in the folder are processed first.
 * After that, new files are detected in real time via the Java NIO
 * {@link WatchService} API.
 */
@Service
public class FileWatcherService {

    private static final Logger log = LoggerFactory.getLogger(FileWatcherService.class);

    // How often to re-check a file's size while waiting for it to stabilize.
    private static final long STABILITY_CHECK_INTERVAL_MS = 500;
    // Max number of checks before giving up waiting and processing anyway (~5s).
    private static final int MAX_STABILITY_CHECKS = 10;

    private final LogFileParser parser;
    private final KafkaTemplate<String, LogEntry> kafkaTemplate;
    private final Path logsFolder;
    private final String topic;

    public FileWatcherService(LogFileParser parser,
                              KafkaTemplate<String, LogEntry> kafkaTemplate,
                              @Value("${app.logs.folder}") String logsFolder,
                              @Value("${app.kafka.topic.logs}") String topic) {
        this.parser = parser;
        this.kafkaTemplate = kafkaTemplate;
        this.logsFolder = Paths.get(logsFolder);
        this.topic = topic;
    }

    /**
     * Runs automatically once the bean is constructed (i.e. on application startup).
     */
    @PostConstruct
    public void start() {
        try {
            Files.createDirectories(logsFolder);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create logs folder: " + logsFolder, e);
        }

        processExistingFiles();
        startWatchingInBackground();
    }

    private void processExistingFiles() {
        try (Stream<Path> files = Files.list(logsFolder)) {
            files.filter(Files::isRegularFile).forEach(this::processFile);
        } catch (IOException e) {
            log.error("Failed to list existing files in {}", logsFolder, e);
        }
    }

    private void startWatchingInBackground() {
        // Runs on its own daemon thread so it doesn't block app startup
        // and doesn't prevent the JVM from shutting down.
        Thread watcherThread = new Thread(this::watchLoop, "log-file-watcher");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    private void watchLoop() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            logsFolder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take(); // blocks until something happens

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path fileName = (Path) event.context();
                        Path fullPath = logsFolder.resolve(fileName);
                        if (Files.isRegularFile(fullPath)) {
                            processFile(fullPath);
                        }
                    }
                }

                boolean stillValid = key.reset();
                if (!stillValid) {
                    log.warn("Watch key no longer valid, stopping file watcher");
                    break;
                }
            }
        } catch (IOException e) {
            log.error("File watcher stopped due to an I/O error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("File watcher interrupted, shutting down");
        }
    }

    private void processFile(Path file) {
        try {
            waitUntilStable(file);

            String componentName = extractComponentName(file);
//          اینجا برای فایل های بزرگ میشه از استریمینگ استفاده کرد که توی اپدیت های بعدی میتونم پیاده سازیش کنم.
            List<String> lines = Files.readAllLines(file);

            for (String line : lines) {
                parser.parseLine(line, componentName).ifPresent(this::sendToKafka);
            }

            Files.delete(file);
            log.info("Processed and deleted log file: {}", file.getFileName());
        } catch (IOException e) {
            log.error("Failed to process log file {}", file, e);
        }
    }

    /**
     * Waits until the file's size stops changing between checks, which we
     * treat as a signal that the writer has finished producing the file.
     */
    private void waitUntilStable(Path file) throws IOException {
        long previousSize = -1;

        for (int attempt = 0; attempt < MAX_STABILITY_CHECKS; attempt++) {
            long currentSize = Files.size(file);
            if (currentSize == previousSize) {
                return; // size unchanged since last check -> stable
            }
            previousSize = currentSize;

            try {
                Thread.sleep(STABILITY_CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        log.warn("File {} did not stabilize after {} checks, processing it anyway",
                file.getFileName(), MAX_STABILITY_CHECKS);
    }


//    خطای ارسال به کافکا داریم
//    اگر ارسال به Kafka خطای unchecked بده،  ممکنه watcher thread بمیره
//    اینم باید مدیریت بشه
    private void sendToKafka(LogEntry entry) {
        try {
            // .get() blocks until Kafka confirms the send, so we only delete
            // the file after we're sure the data made it to the broker.
            kafkaTemplate.send(topic, entry.getComponentName(), entry).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending log entry to Kafka", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to send log entry to Kafka", e);
        }
    }

    /**
     * Extracts the component name from a log file name.
     * Expected convention: "{componentName}_{timestamp}[.extension]"
     */
    private String extractComponentName(Path file) {
        String fileName = file.getFileName().toString();
        String withoutExtension = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;

        int underscoreIndex = withoutExtension.indexOf('_');
        return underscoreIndex > 0
                ? withoutExtension.substring(0, underscoreIndex)
                : withoutExtension;
    }
}

// اگه برنامه بعد از ارسال ولی قبل از پاک کردن فایل crash کنن، ممکنه لاگ‌ها دوباره فرستاده بشن



// برای scale کردن، اول داده را shard یا claim کن، بعد workerها را زیاد کن. بدون coordination، چند watcher روی یک
//پوشه باعث duplicate و race condition می‌شوند. برای حجم بالا هم بهتر است ingestion را به ابزارهایی مثل Filebeat یا
// Fluent Bit بسپاری.