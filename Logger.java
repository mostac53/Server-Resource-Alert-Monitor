import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class Logger {
    private final Path logFile;
    private final Consumer<String> uiAppender;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Logger(Path logFile, Consumer<String> uiAppender) {
        this.logFile = logFile;
        this.uiAppender = uiAppender;
    }

    public synchronized void info(String message) {
        uiAppender.accept(line("INFO", message));
    }

    public synchronized void alert(String message) {
        String out = line("ALERT", message);
        uiAppender.accept(out);
        try {
            Files.writeString(logFile, out + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            uiAppender.accept(line("ERROR", "Cannot write Alerts_Log.txt: " + ex.getMessage()));
        }
    }

    private String line(String level, String message) {
        return String.format("[%s] [%s] %s", LocalDateTime.now().format(timeFmt), level, message);
    }
}
