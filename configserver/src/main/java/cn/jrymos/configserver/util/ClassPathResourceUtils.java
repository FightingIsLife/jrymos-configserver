package cn.jrymos.configserver.util;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.collections4.map.LazyMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 获取resources配置
 */
@Slf4j
public class ClassPathResourceUtils {

    public static final String JAR_ENTRY_PATH = "BOOT-INF/classes";
    private static ResourceLoader resourceLoader = new DefaultResourceLoader();
    private static volatile boolean initResourcesByJar = false;

    /**
     * init by configserver.jar
     * examples:
     * key=/, value=[config, test, application.properties...]
     * key=/config, value=[extension, application-gamma.yml...]
     * key=/config/extension, value=[athena, live-video-service...]
     */
    private static volatile Map<String, Set<String>> pathToResourceNames = LazyMap.lazyMap(new HashedMap<>(), (Factory<HashSet<String>>) HashSet::new);

    public static synchronized void initResourcesByJar() throws IOException {
        Map<String, Set<String>> pathToResourceNames = LazyMap.lazyMap(new HashedMap<>(), (Factory<HashSet<String>>) HashSet::new);
        URL url = ClassPathResourceUtils.class.getResource("/");
        log.info("url:{}", url);
        String jarPath = url.toString().substring(0, url.toString().indexOf("!/") + 2);
        log.info("jarPath:{}", jarPath);
        URL jarURL = new URL(jarPath);
        log.info("jarUrl:{}", jarURL);
        JarURLConnection jarCon = (JarURLConnection) jarURL.openConnection();
        log.info("jarCon:{}", jarCon);
        try (JarFile jarFile = jarCon.getJarFile()) {
            log.info("jarFile:{}", jarFile);
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                // jarEntry name such as:
                // BOOT-INF/classes/test/
                // BOOT-INF/classes/test/application-alpha.properties
                JarEntry jarEntry = jarEntries.nextElement();
                String name = jarEntry.getName();
                if (name.startsWith(JAR_ENTRY_PATH) && !name.equals(JAR_ENTRY_PATH + "/")) {
                    if (jarEntry.isDirectory()) {
                        name = name.substring(0, name.length() - 1);
                    }
                    int pathEndIndex = name.lastIndexOf("/");
                    log.debug("initResourceByJar#name:{}, pathEndIndex:{}", name, pathEndIndex);
                    String path = name.substring(JAR_ENTRY_PATH.length(), pathEndIndex);
                    path = StringUtils.isEmpty(path) ? "/" : path;
                    String fileName = name.substring(pathEndIndex + 1);
                    log.info("initResourceByJar#add path:{}, fileName:{}", path, fileName);
                    pathToResourceNames.get(path).add(fileName);
                }
            }
        }
        log.info("pathToResourceNames:{}", pathToResourceNames);
        ClassPathResourceUtils.pathToResourceNames = pathToResourceNames;
        initResourcesByJar = true;
    }

    public static ClassPathResource[] getSubResources(ClassPathResource resource) throws IOException {
        if (resource == null || !resource.exists() || resource.isReadable()) {
            return new ClassPathResource[]{};
        }
        if (initResourcesByJar) {
            String path = resource.getPath();
            Set<String> fileNames = pathToResourceNames.get(path.startsWith("/") ? path : "/" + path);
            log.debug("path:{}, fileNames:{}", path, fileNames);
            if (CollectionUtils.isEmpty(fileNames)) {
                return new ClassPathResource[]{};
            }
            return fileNames.stream()
                .map(fileName -> resourceLoader.getResource(path + "/" + fileName))
                .toArray(ClassPathResource[]::new);
        } else {
            File[] files = resource.getFile().listFiles();
            if (files == null || files.length == 0) {
                return new ClassPathResource[]{};
            }
            return Arrays.stream(files)
                .map(file -> resourceLoader.getResource(resource.getPath() + "/" + file.getName()))
                .toArray(ClassPathResource[]::new);
        }
    }

    public static ClassPathResource getResource(String location) {
        return (ClassPathResource) resourceLoader.getResource(location);
    }

    public static ClassPathResource get(String basePath, String...values) {
        String value = Joiner.on("/").join(values);
        return getResource(basePath + "/" + value);
    }
}
