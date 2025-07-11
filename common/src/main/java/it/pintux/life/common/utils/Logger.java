package it.pintux.life.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Enhanced logging utility for BedrockGUI
 * Provides structured logging with different levels and proper formatting
 */
public class Logger {
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String name;
    private final java.util.logging.Logger platformLogger;
    
    private Logger(String name, java.util.logging.Logger platformLogger) {
        this.name = name;
        this.platformLogger = platformLogger;
    }
    
    /**
     * Creates a logger for the specified class
     */
    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz.getSimpleName(), java.util.logging.Logger.getLogger(clazz.getName()));
    }
    
    /**
     * Creates a logger with the specified name
     */
    public static Logger getLogger(String name) {
        return new Logger(name, java.util.logging.Logger.getLogger(name));
    }
    
    /**
     * Logs an info message
     */
    public void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }
    
    /**
     * Logs a warning message
     */
    public void warn(String message, Object... args) {
        log(Level.WARNING, message, args);
    }
    
    /**
     * Logs a warning message with exception
     */
    public void warn(String message, Throwable throwable, Object... args) {
        log(Level.WARNING, message, throwable, args);
    }
    
    /**
     * Logs an error message
     */
    public void error(String message, Object... args) {
        log(Level.SEVERE, message, args);
    }
    
    /**
     * Logs an error message with exception
     */
    public void error(String message, Throwable throwable, Object... args) {
        log(Level.SEVERE, message, throwable, args);
    }
    
    /**
     * Logs a debug message
     */
    public void debug(String message, Object... args) {
        log(Level.FINE, message, args);
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
        
        // Simple placeholder replacement for {} style logging
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