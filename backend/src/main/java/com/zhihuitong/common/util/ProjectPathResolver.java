package com.zhihuitong.common.util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves project-relative runtime paths without coupling the application to
 * a developer's local checkout directory.
 *
 * <p>Local development may start Spring Boot from either the repository root
 * or the {@code backend} directory. The resolver walks up from the JVM
 * working directory until it finds the repository layout, then resolves
 * relative paths from that root. Absolute paths remain supported for Docker
 * and server deployments.</p>
 */
public final class ProjectPathResolver {

    private ProjectPathResolver() {
    }

    public static Path resolve(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalArgumentException("Configured path must not be blank");
        }

        Path path = Path.of(configuredPath).normalize();
        if (path.isAbsolute()) {
            return path;
        }

        Path workingDirectory = Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath()
                .normalize();
        Path projectRoot = findProjectRoot(workingDirectory);
        return (projectRoot == null ? workingDirectory : projectRoot)
                .resolve(path)
                .normalize();
    }

    private static Path findProjectRoot(Path start) {
        Path candidate = start;
        while (candidate != null) {
            if (Files.isDirectory(candidate.resolve("backend"))
                    && Files.isDirectory(candidate.resolve("docs"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        return null;
    }
}
