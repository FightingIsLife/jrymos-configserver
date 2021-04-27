package cn.jrymos.configserver.environment;

import cn.jrymos.configserver.util.ExtensionNativePropertySourceLoaderUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DependsOn("extensionNativeEnvironmentCommonRepository")
public class ExtensionNativeEnvironmentRepository implements EnvironmentRepository, Ordered {

    @Override
    public Environment findOne(String application, String profile, String label) {
        return ExtensionNativePropertySourceLoaderUtils.loadEnvironment(application, profile, label);
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
