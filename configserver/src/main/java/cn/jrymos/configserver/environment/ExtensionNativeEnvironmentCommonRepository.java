package cn.jrymos.configserver.environment;


import cn.jrymos.configserver.util.ClassPathResourceUtils;
import cn.jrymos.configserver.util.ExtensionNativePropertySourceLoaderUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 从文件中
 */
@Slf4j
@Component
public class ExtensionNativeEnvironmentCommonRepository implements EnvironmentRepository, Ordered {

    private static final String COMMON_PROFILE = "common";

    public ExtensionNativeEnvironmentCommonRepository(@Value("${spring.cloud.config.extension.bathPath}") String basePath,
                                                      @Value("${spring.profiles.active}") String actives) throws IOException {
        try {
            if (!actives.contains("alpha") && !actives.contains("test")) {
                ClassPathResourceUtils.initResourcesByJar();
            }
            ExtensionNativePropertySourceLoaderUtils.initApplicationToProfileToFiles(basePath);
        } catch (IOException e) {
            log.error("start failed:{}", basePath, e);
            throw e;
        }
    }


    @Override
    public Environment findOne(String application, String profile, String label) {
        return ExtensionNativePropertySourceLoaderUtils.loadEnvironment(application, COMMON_PROFILE, label);
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
