package dev.primeants.gametest;

import java.nio.file.Path;

/** Public evidence paths are independent of the client's working directory. */
public final class CapturePaths {
    private CapturePaths() {}

    private static Path repository() {
        var root = System.getProperty("prime_ants.repositoryRoot");
        if (root == null) throw new IllegalStateException("Capture repository root is required");
        return Path.of(root).toAbsolutePath().normalize();
    }

    public static String repositoryRelative(Path path) {
        var root = repository();
        var absolute = path.toAbsolutePath().normalize();
        if (!absolute.startsWith(root)) throw new IllegalArgumentException("Capture path is outside the repository");
        return portable(root.relativize(absolute));
    }

    public static String publicPath(Path path) {
        var root = repository();
        var absolute = path.toAbsolutePath().normalize();
        if (absolute.startsWith(root)) return portable(root.relativize(absolute));
        var turnloop = root.getParent().resolve("turnloop");
        if (absolute.startsWith(turnloop)) return "<turnloop>/" + portable(turnloop.relativize(absolute));
        var profile = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        if (absolute.startsWith(profile)) return "%USERPROFILE%/" + portable(profile.relativize(absolute));
        throw new IllegalArgumentException("Evidence path requires a public placeholder");
    }

    private static String portable(Path path) { return path.toString().replace('\\', '/'); }
}
