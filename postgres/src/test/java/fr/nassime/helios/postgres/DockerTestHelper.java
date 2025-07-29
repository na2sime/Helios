package fr.nassime.helios.postgres;

import org.junit.jupiter.api.condition.EnabledIf;

import java.lang.annotation.*;

/**
 * Helper class and annotations for Docker-dependent tests.
 */
public class DockerTestHelper {
    
    /**
     * Checks if Docker is available on the system.
     */
    public static boolean isDockerAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.out.println("Docker is not available: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Annotation to enable tests only when Docker is available.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @EnabledIf("fr.nassime.helios.postgres.DockerTestHelper#isDockerAvailable")
    public @interface EnabledIfDockerAvailable {
    }
}