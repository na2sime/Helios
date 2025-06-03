package fr.nassime.helios.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HeliosLogger {
    private static final Logger LOGGER = Logger.getLogger("Helios");

    public static void info(String message) {
        LOGGER.log(Level.INFO, "[Helios] " + message);
    }

    public static void warn(String message) {
        LOGGER.log(Level.WARNING, "[Helios] " + message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, "[Helios] " + message, throwable);
    }
}
