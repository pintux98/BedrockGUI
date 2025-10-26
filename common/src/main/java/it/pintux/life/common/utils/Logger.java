package it.pintux.life.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;


public class Logger {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String name;
    private final java.util.logging.Logger platformLogger;

    private Logger(String name, java.util.logging.Logger platformLogger) {
        this.name = name;
        this.platformLogger = platformLogger;
    }


    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz.getSimpleName(), java.util.logging.Logger.getLogger(clazz.getName()));
    }


    public static Logger getLogger(String name) {
        return new Logger(name, java.util.logging.Logger.getLogger(name));
    }


    public void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }


    public void warn(String message, Object... args) {
        log(Level.WARNING, message, args);
    }


    public void warn(String message, Throwable throwable, Object... args) {
        log(Level.WARNING, message, throwable, args);
    }


    public void error(String message, Object... args) {
        log(Level.SEVERE, message, args);
    }


    public void error(String message, Throwable throwable, Object... args) {
        log(Level.SEVERE, message, throwable, args);
    }


    public void debug(String message, Object... args) {
        log(Level.FINE, message, args);
    }


    public boolean isDebugEnabled() {
        return platformLogger.isLoggable(Level.FINE);
    }

    private void log(Level level, String message, Object... args) {
        if (platformLogger.isLoggable(level)) {
            String formattedMessage = formatMessage(message, args);
            platformLogger.log(level, formattedMessage);
        }
    }

    private void log(Level level, String message, Throwable throwable, Object... args) {
        if (platformLogger.isLoggable(level)) {
            String formattedMessage = formatMessage(message, args);
            platformLogger.log(level, formattedMessage, throwable);
        }
    }

    private String formatMessage(String message, Object... args) {
        if (args.length == 0) {
            return message;
        }


        String result = message;
        for (Object arg : args) {
            int index = result.indexOf("{}");
            if (index != -1) {
                String replacement = arg != null ? arg.toString() : "null";
                result = result.substring(0, index) + replacement + result.substring(index + 2);
            }
        }
        return result;
    }
}
