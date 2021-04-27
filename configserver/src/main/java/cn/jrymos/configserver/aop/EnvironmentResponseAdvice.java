package cn.jrymos.configserver.aop;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.HashedMap;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentController;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;

/**
 * 拦截Environment的返回结果，需要对多处的配置进行覆盖合并
 */
@ControllerAdvice
@Slf4j
public class EnvironmentResponseAdvice implements ResponseBodyAdvice {

    private final Function<Environment, Map<String, Object>> convertToProperties;

    public EnvironmentResponseAdvice() throws NoSuchMethodException {
        EnvironmentController environmentController = new EnvironmentController(null);
        Method convertToProperties = environmentController.getClass().getDeclaredMethod("convertToProperties", Environment.class);
        convertToProperties.setAccessible(true);
        this.convertToProperties = environment -> {
            try {
                return (Map<String, Object>) convertToProperties.invoke(environmentController, environment);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("init EnvironmentResponseAdvice error", e);
            }
            return new HashedMap<>();
        };
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Class clazz) {
        return methodParameter.getContainingClass().equals(EnvironmentController.class);
    }

    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class clazz, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        if (o instanceof Environment) {
            Environment environment = (Environment) o;
            // 按照优先级合并所有配置
            Map<String, Object> result = convertToProperties.apply(environment);
            environment.getPropertySources().clear();
            environment.add(new PropertySource("configServer", result));
        }
        return o;
    }
}
