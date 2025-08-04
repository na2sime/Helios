package fr.nassime.helios.mariadb;

import org.junit.jupiter.api.condition.EnabledIf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Utility class for Docker-related test helpers.
 */
public class DockerTestHelper {
    
    /**
     * Annotation to enable tests only if Docker is available.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @EnabledIf("fr.nassime.helios.mariadb.DockerTestHelper#isDockerAvailable")
    public @interface EnabledIfDockerAvailable {
    }
    
    /**
     * Check if Docker is available on the system.
     * This is used by the @EnabledIfDockerAvailable annotation.
     */
    public static boolean isDockerAvailable() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"docker", "--version"});
            process.waitFor();
            boolean dockerAvailable = process.exitValue() == 0;
            if (!dockerAvailable) {
                System.out.println("Docker is not available. Skipping integration tests.");
            }
            return dockerAvailable;
        } catch (Exception e) {
            System.out.println("Docker is not available: " + e.getMessage() + ". Skipping integration tests.");
            return false;
        }
    }
}