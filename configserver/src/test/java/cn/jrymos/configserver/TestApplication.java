package cn.jrymos.configserver;

import cn.jrymos.configserver.environment.ExtensionNativeEnvironmentRepository;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@ComponentScan(excludeFilters = {@ComponentScan.Filter(type = FilterType.REGEX, pattern = "cn.jrymos.configserver.spi.*")})
@SpringBootApplication
@EnableConfigServer
@EnableDiscoveryClient
public class TestApplication implements InitializingBean {

    @Autowired
    private ConfigurableEnvironment environment;

    public static void main(String[] args) throws IOException, URISyntaxException {
        copyConfigExtensionFiles();
        SpringApplication.run(TestApplication.class, args);
    }

    //拷贝configserver/src/main/resources/config/extension 到test resources
    public static void copyConfigExtensionFiles() throws IOException, URISyntaxException {
        Path path = Paths.get(System.getProperty("user.dir"), "configserver/src/main/resources");
        List<PropertySource<?>> propertySources = new PropertiesPropertySourceLoader().load("main", new FileSystemResource(path + "/application.properties"));
        String basePath = propertySources.stream().map(PropertySource::getSource)
            .filter(source -> source instanceof Map)
            .map(source -> (Map) source)
            .map((Function<Map, Set>) Map::entrySet)
            .flatMap(Set::stream)
            .map(o -> (Map.Entry) o)
            .filter(entry -> Objects.equals("spring.cloud.config.extension.bathPath", entry.getKey()))
            .map(Map.Entry::getValue)
            .map(Object::toString)
            .map(string -> string.replace("classpath:", ""))
            .findFirst()
            .orElse("/config/extension");
        String destPath = Paths.get(ExtensionNativeEnvironmentRepository.class.getResource("/").toURI()) + basePath;
        FileUtils.copyDirectory(new File(path + basePath), new File(destPath));
    }

    @Override
    public void afterPropertiesSet() {
        environment.getPropertySources().remove("applicationConfig: [classpath:/config/application-alpha.yml]");
    }
}
