package de.bluecolored.bluemap.common.api;

import de.bluecolored.bluemap.api.WebApp;
import de.bluecolored.bluemap.common.plugin.Plugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class WebAppImpl implements WebApp {
    private static final String IMAGE_ROOT_PATH = "images";

    private final Plugin plugin;

    public WebAppImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Path getWebRoot() {
        return plugin.getConfigs().getWebappConfig().getWebroot();
    }

    @Override
    public void setPlayerVisibility(UUID player, boolean visible) {
        if (visible) {
            plugin.getPluginState().removeHiddenPlayer(player);
        } else {
            plugin.getPluginState().addHiddenPlayer(player);
        }
    }

    @Override
    public String createImage(BufferedImage image, String path) throws IOException {
        path = path.replaceAll("[^a-zA-Z0-9_.\\-/]", "_");

        Path webRoot = getWebRoot().toAbsolutePath();
        String separator = webRoot.getFileSystem().getSeparator();

        Path webDataRoot = webRoot.resolve("data");
        Path imagePath = webDataRoot.resolve(Path.of(IMAGE_ROOT_PATH, path.replace("/", separator) + ".png")).toAbsolutePath();

        Files.deleteIfExists(imagePath);
        Files.createFile(imagePath);

        if (!ImageIO.write(image, "png", imagePath.toFile()))
            throw new IOException("The format 'png' is not supported!");

        return webRoot.relativize(imagePath).toString().replace(separator, "/");
    }

    @Override
    public Map<String, String> availableImages() throws IOException {
        Path webRoot = getWebRoot().toAbsolutePath();
        String separator = webRoot.getFileSystem().getSeparator();

        Path imageRootPath = webRoot.resolve("data").resolve(IMAGE_ROOT_PATH).toAbsolutePath();

        Map<String, String> availableImagesMap = new HashMap<>();

        try (Stream<Path> fileStream = Files.walk(imageRootPath)){
            fileStream
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> p.getFileName().toString().endsWith(".png"))
                    .map(Path::toAbsolutePath)
                    .forEach(p -> {
                        try {
                            String key = imageRootPath.relativize(p).toString();
                            key = key
                                    .substring(0, key.length() - 4) //remove .png
                                    .replace(separator, "/");

                            String value = webRoot.relativize(p).toString()
                                    .replace(separator, "/");

                            availableImagesMap.put(key, value);
                        } catch (IllegalArgumentException ignore) {}
                    });
        }

        return availableImagesMap;
    }

}