package cn.jrymos.configserver.util;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 加载资源配置
 */
@Slf4j
public class ExtensionNativePropertySourceLoaderUtils {

    private static final Set<String> ignoreFileExtensions = ImmutableSet.of(
        ".txt", ".md", ".TXT", "MD"
    );

    private static volatile Table<String, String, ClassPathResource> applicationToProfileToFiles = HashBasedTable.create();

    private static final List<PropertySourceLoader> loaders = ImmutableList.of(
        new YamlPropertySourceLoader(), new PropertiesPropertySourceLoader()
    );


    public static Table<String, String, ClassPathResource> getApplicationToProfileToFiles() {
        return HashBasedTable.create(applicationToProfileToFiles);
    }

    public static synchronized void initApplicationToProfileToFiles(String basePath) throws IOException {
        Table<String, String, ClassPathResource> applicationToProfileToFiles = getApplicationToProfileToFiles(basePath);
        List<ClassPathResource> warningFiles = new ArrayList<>();
        for (Table.Cell<String, String, ClassPathResource> appProfileFile : applicationToProfileToFiles.cellSet()) {
            loadEnvironment(applicationToProfileToFiles, appProfileFile.getRowKey(), appProfileFile.getColumnKey(), warningFiles);
        }
        log.info("check result, warningFiles:{}", warningFiles);
        if (!warningFiles.isEmpty()) {
            log.error("warning files:{}", warningFiles);
            throw new IllegalArgumentException("exists warning files, number:" + warningFiles.size());
        }
        log.info("initApplicationToProfileToFiles#end, applicationToProfileToFiles:{}", applicationToProfileToFiles);
        ExtensionNativePropertySourceLoaderUtils.applicationToProfileToFiles = applicationToProfileToFiles;
    }

    public static Environment loadEnvironment(Table<String, String, ClassPathResource> applicationToProfileToFiles,
                                              String application, String profile, List<ClassPathResource> warningFiles) throws IOException {
        ClassPathResource file = getAndCheckEnvironmentFile(applicationToProfileToFiles, application, profile, warningFiles);
        Environment environment = new Environment(application, profile);
        if (file != null) {
            loadFile(null, file, environment, new HashedMap<>(), warningFiles);
        }
        return environment;
    }


    private static Environment loadEnvironment(String application, String profile, List<ClassPathResource> warningFiles) throws IOException {
        Environment environment = loadEnvironment(applicationToProfileToFiles, application, profile, warningFiles);
        log.info("loadEnvironment#application:{}, profile:{}, environment:{}, warningFiles:{}", application, profile, environment, warningFiles);
        return environment;
    }

    public static Environment loadEnvironment(String application, String profile, String label) {
        List<ClassPathResource> warningFiles = new ArrayList<>();
        Environment environment = null;
        try {
            environment = loadEnvironment(application, profile, warningFiles);
        } catch (IOException e) {
            log.error("findOneError#application:{}, profile:{}, label:{}, warningFiles:{}", application, profile, label, warningFiles);
        }
        if (!warningFiles.isEmpty()) {
            log.warn("unload files:{}", warningFiles);
        } else {
            log.info("load success:{}, {}", application, profile);
        }
        return environment;
    }

    private static ClassPathResource getAndCheckEnvironmentFile(Table<String, String, ClassPathResource> applicationToProfileToFiles,
                                                                String application, String profile, List<ClassPathResource> warningFiles) {
        Map<String, ClassPathResource> applicationToProfile = applicationToProfileToFiles.column(profile);
        List<ClassPathResource> files = new ArrayList<>();
        for (Map.Entry<String, ClassPathResource> applicationNameToFile : applicationToProfile.entrySet()) {
            String applicationName = applicationNameToFile.getKey();
            if (application.startsWith(applicationName)) {
                ClassPathResource file = applicationToProfile.get(applicationName);
                if (file == null || !file.exists()) {
                    log.warn("file is not exist:{}, application:{}, profile:{}", file, application, profile);
                } else if (file.isReadable()) {
                    log.warn("directory is not exist:{}, application:{}, profile:{}", file, application, profile);
                } else {
                    files.add(file);
                }
            }
        }
        if (files.isEmpty()) {
            return null;
        } else if (files.size() == 1) {
            return files.get(0);
        } else {
            log.error("getAndCheckEnvironmentFile#duplicate applicationName:{} files:{}", application, files);
            warningFiles.addAll(files);
            return null;
        }
    }

    private static Table<String, String, ClassPathResource> getApplicationToProfileToFiles(String basePath) throws IOException {
        HashBasedTable<String, String, ClassPathResource> applicationToProfileToFiles = HashBasedTable.create();
        ClassPathResource[] applicationFiles = getSubFiles(ClassPathResourceUtils.getResource(basePath));
        if (applicationFiles.length == 0) {
            log.warn("empty basePath:{}", basePath);
            return applicationToProfileToFiles;
        }
        for (ClassPathResource applicationFile : applicationFiles) {
            ClassPathResource[] profiles = getSubFiles(applicationFile);
            if (profiles.length == 0) {
                log.warn("empty applicationFile:{}", applicationFile);
                continue;
            }
            for (ClassPathResource profile : profiles) {
                ClassPathResource[] subFiles = ClassPathResourceUtils.getSubResources(profile);
                if (profile.isReadable() || subFiles.length == 0) {
                    log.warn("empty profile:{}", profile);
                    continue;
                }
                applicationToProfileToFiles.put(applicationFile.getFilename(), profile.getFilename(), profile);
            }
        }
        return applicationToProfileToFiles;
    }

    private static ClassPathResource[] getSubFiles(ClassPathResource basePath) throws IOException {
        if (basePath == null || !basePath.exists()) {
            throw new IllegalArgumentException("file not exists:" + basePath);
        }
        String fileExtension = Files.getFileExtension(basePath.getFilename());
        if (ignoreFileExtensions.contains(fileExtension)) {
            return new ClassPathResource[]{};
        }
        if (basePath.isReadable()) {
            throw new IllegalArgumentException("file is not directory:" + basePath);
        }
        if (basePath.getFilename().contains(".")) {
            throw new IllegalArgumentException("file contains '.' not support:" + basePath);
        }
        return ClassPathResourceUtils.getSubResources(basePath);
    }

    private static void loadFile(String parentName, ClassPathResource file, Environment environment,
                                 Map<String, ClassPathResource> loadFileNames, List<ClassPathResource> warningFiles) throws IOException {
        String nameWithoutExtension = Files.getNameWithoutExtension(file.getFilename());
        if (loadFileNames.containsKey(nameWithoutExtension)) {
            ClassPathResource file1 = loadFileNames.get(nameWithoutExtension);
            log.error("文件或目录重名:{},{}", file, file1);
            warningFiles.add(file);
            return;
        }
        loadFileNames.put(nameWithoutExtension, file);
        if (file.isReadable()) {
            String fileExtension = Files.getFileExtension(file.getFilename());
            if (ignoreFileExtensions.contains(fileExtension)) {
                return;
            }
            PropertySourceLoader loader = loaders.stream().filter(loader1 -> support(loader1, fileExtension)).findFirst().orElse(null);
            if (loader == null) {
                log.error("文件类型不支持加载:{}", file);
                warningFiles.add(file);
                return;
            }
            List<PropertySource<?>> propertySources = new ArrayList<>();
            String name = getNextParentName(parentName, file);
            try {
                propertySources = loader.load(name, file);
            } catch (IOException e) {
                log.error("load file error :{}", file, e);
                warningFiles.add(file);
            }
            add(name, environment, propertySources);
        } else if (file.exists()) {
            if (file.getFilename().contains(".")) {
                log.error("非法目录:{}", file);
                warningFiles.add(file);
                return;
            }
            ClassPathResource[] files = ClassPathResourceUtils.getSubResources(file);
            if (files.length == 0) {
                log.info("directory is empty:{}, parentName:{}", file, parentName);
                return;
            }
            parentName = getNextParentName(parentName, file);
            loadFileNames = new HashedMap<>();
            for (ClassPathResource subFile : files) {
                loadFile(parentName, subFile, environment, loadFileNames, warningFiles);
            }
        } else {
            log.warn("file not exist:{}, parentName:{}", file, parentName);
            warningFiles.add(file);
        }
    }

    private static String getNextParentName(String parentName, ClassPathResource file) {
        String nameWithoutExtension = Files.getNameWithoutExtension(file.getFilename());
        if (parentName == null) {
            return "";
        } else if ("".equals(parentName)) {
            return nameWithoutExtension;
        } else {
            return parentName + "." + nameWithoutExtension;
        }
    }

    private static void add(String parentName, Environment environment, List<PropertySource<?>> propertySources) {
        if (CollectionUtils.isNotEmpty(propertySources)) {
            for (PropertySource propertySource : propertySources) {
                environment.add(new org.springframework.cloud.config.environment.PropertySource(
                    propertySource.getName(), getMap(parentName, propertySource)));
            }
        }
    }

    private static Map<?, ?> getMap(String parentName, PropertySource<?> source) {
        Map<Object, Object> map = new LinkedHashMap<>();
        Map<?, ?> input = (Map<?, ?>) source.getSource();
        for (Object key : input.keySet()) {
            // Spring Boot wraps the property values in an "origin" detector, so we need
            // to extract the string values
            map.put(parentName + "." + key, source.getProperty(key.toString()));
        }
        return map;
    }

    private static boolean support(PropertySourceLoader loader, String fileExtension) {
        for (String extension : loader.getFileExtensions()) {
            if (extension.equalsIgnoreCase(fileExtension)) {
                return true;
            }
        }
        return false;
    }
}
