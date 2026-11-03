package org.gradle.wrapper;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Minimal open-source bootstrap for the teaching prototype, not the official Gradle wrapper.
 * Downloads the declared Gradle release, then invokes its regular gradle launcher.
 * Replace via `gradle wrapper --gradle-version 8.11.1` if desired.
 */
public final class GradleWrapperMain {
    public static void main(String[] args) throws Exception {
        Path jar = Path.of(GradleWrapperMain.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path propertiesPath = jar.getParent().resolve("gradle-wrapper.properties");
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(propertiesPath)) { props.load(in); }
        URI url = URI.create(props.getProperty("distributionUrl"));
        if (!"https".equalsIgnoreCase(url.getScheme()) || !"services.gradle.org".equalsIgnoreCase(url.getHost())) {
            throw new IllegalArgumentException("Only official HTTPS services.gradle.org distributions are allowed");
        }
        String fileName = Path.of(url.getPath()).getFileName().toString();
        if (!fileName.matches("gradle-[0-9.]+-bin\\.zip")) {
            throw new IllegalArgumentException("Unexpected Gradle distribution: " + fileName);
        }
        String gradleName = fileName.substring(0, fileName.length() - "-bin.zip".length());
        Path cache = Path.of(System.getProperty("user.home"), ".gradle", "wrapper", "dists", gradleName + "-healtheat");
        Path install = cache.resolve(gradleName);
        Path launcher = install.resolve("bin").resolve(isWindows() ? "gradle.bat" : "gradle");
        if (!Files.exists(launcher)) {
            Files.createDirectories(cache);
            Path zipPath = cache.resolve(fileName);
            if (!Files.isRegularFile(zipPath)) {
                System.out.println("Downloading official Gradle " + gradleName + " ...");
                HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS)
                    .connectTimeout(Duration.ofSeconds(30)).build();
                HttpRequest request = HttpRequest.newBuilder(url).timeout(Duration.ofMinutes(10)).GET().build();
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) throw new IllegalStateException("Gradle download failed: HTTP " + response.statusCode());
                Path tempZip = cache.resolve(fileName + ".part");
                try (InputStream in = response.body()) { Files.copy(in, tempZip, StandardCopyOption.REPLACE_EXISTING); }
                Files.move(tempZip, zipPath, StandardCopyOption.REPLACE_EXISTING);
            }
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipPath))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    Path target = cache.resolve(entry.getName()).normalize();
                    if (!target.startsWith(cache)) throw new SecurityException("Unsafe ZIP path");
                    if (entry.isDirectory()) Files.createDirectories(target);
                    else {
                        Files.createDirectories(target.getParent());
                        Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zip.closeEntry();
                }
            }
        }
        if (!Files.exists(launcher)) throw new IllegalStateException("Gradle launcher missing: " + launcher);
        List<String> command = new ArrayList<>();
        if (isWindows()) {
            command.add("cmd"); command.add("/c"); command.add(launcher.toString());
        } else {
            command.add("sh"); command.add(launcher.toString());
        }
        for (String arg : args) command.add(arg);
        Process process = new ProcessBuilder(command).directory(jar.getParent().getParent().getParent().toFile())
            .inheritIO().start();
        System.exit(process.waitFor());
    }
    private static boolean isWindows() { return System.getProperty("os.name").toLowerCase().contains("win"); }
}
