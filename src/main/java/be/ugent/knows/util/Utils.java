package be.ugent.knows.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * <p>Copyright 2022 IDLab (Ghent University - imec)</p>
 *
 * @author Gerald Haesendonck
 */
public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static File getFile(String path) throws IOException {
        return Utils.getFile(path, null);
    }

    public static File getFile(String path, File basePath) throws IOException {
        // Absolute path?
        File f = new File(path);
        if (f.isAbsolute()) {
            if (f.exists()) {
                return f;
            } else {
                throw new FileNotFoundException();
            }
        }

        if (basePath == null) {
            try {
                basePath = new File(System.getProperty("user.dir"));
            } catch (Exception e) {
                throw new FileNotFoundException();
            }
        }

        logger.debug("Looking for file " + path + " in basePath " + basePath);

        // Relative from user dir?
        f = new File(basePath, path);
        if (f.exists()) {
            return f;
        }

        logger.debug("File " + path + " not found in " + basePath);
        logger.debug("Looking for file " + path + " in " + basePath + "/../");


        // Relative from parent of user dir?
        f = new File(basePath, "../" + path);
        if (f.exists()) {
            return f;
        }

        logger.debug("File " + path + " not found in " + basePath);
        logger.debug("Looking for file " + path + " in the resources directory");

        // Resource path?
        try {
            return getResourceAsFile(path);
        } catch (IOException e) {
            // Too bad
        }

        logger.debug("File " + path + " not found in the resources directory");

        throw new FileNotFoundException(path);
    }

    static File getResourceAsFile(String resource) throws IOException {
        ClassLoader cl = Utils.class.getClassLoader();
        //URL resourceUrl = URLClassLoader.getSystemResource(resource);
        //URLClassLoader classLoader = new URLClassLoader(new URL[]{new URL(resource)}, cl);
        URL resourceUrl = cl.getResource(resource);
        if (resourceUrl == null) {
            throw new IOException("Resource file " + resource + " doesn't exist");
        }
        if ("file".equals(resourceUrl.getProtocol())) {
            try {

                URI uri = resourceUrl.toURI();
                return new File(uri);
            } catch (URISyntaxException e) {
                throw new IOException("Unable to get file through class loader: " + cl, e);
            }

        } else {
            throw new IOException(
                    "Unable to get file through class loader: " + cl);

        }
    }

    public static String fileToString(final File file) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Files.copy(file.toPath(), os);
        return os.toString(StandardCharsets.UTF_8.name());
    }

    public static void deleteDirectory(final File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                boolean allFilesDeleted = true;
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    }
                    if (file.isFile()) {
                        if (!file.delete()) {
                            logger.warn("Could not delete {}", file.getCanonicalPath());
                            allFilesDeleted = false;
                        }
                    }
                }
                if (allFilesDeleted) {
                    if (!directory.delete()) {
                        allFilesDeleted = false;
                    }
                }
                if (!allFilesDeleted) {
                    throw new IOException("Could not delete directory " + directory);
                }
            }
        } else {
            logger.warn("{} is not a directory; deleting nothing.", directory);
        }
    }
}
