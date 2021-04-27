package cn.jrymos.configserver.controller;

import cn.jrymos.configserver.notify.RefreshClientConfigNotifier;
import cn.jrymos.configserver.spi.RefreshMessage;
import cn.jrymos.configserver.util.ExtensionNativePropertySourceLoaderUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.map.LazyMap;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
public class RefreshController {

    private final List<RefreshClientConfigNotifier> notifiers;
    private final ServerProperties serverProperties;
    private static final String HTTP_HOST = "http://localhost:";

    /**
     * 手动调用接口通知client刷新配置
     */
    @GetMapping("/internal/refresh")
    public Object refresh(@RequestParam(value = "appName", required = false) String appName,
                          @RequestParam(value = "profile", required = false) String profile,
                          @RequestParam(value = "operator", required = false) String operator,
                          @RequestParam(value = "description", required = false, defaultValue = "for test") String description) {
        RefreshMessage message = RefreshMessage.builder()
            .messageId(String.valueOf(System.currentTimeMillis()))
            .appName(appName)
            .profile(profile)
            .description(operator + ":" + description + ":" + "/internal/refresh")
            .build();
        notifiers.forEach(refreshClientConfigNotifier -> refreshClientConfigNotifier.notice(message));
        return Collections.singletonMap("success", message);
    }

    /**
     * 获取配置信息链接
     */
    @GetMapping(value = {"/", "/configs"})
    public Map<String, Map<String, List<String>>> getConfigs() {
        Table<String, String, ClassPathResource> table = ExtensionNativePropertySourceLoaderUtils.getApplicationToProfileToFiles();
        Map<String, Map<String, List<String>>> configs = LazyMap.lazyMap(new HashMap<>(), (Factory<Map<String, List<String>>>) HashMap::new);
        table.cellSet().forEach(cell -> configs.get(cell.getRowKey()).put(cell.getColumnKey(), getUrls(cell)));
        return configs;
    }

    private List<String> getUrls(Table.Cell<String, String, ClassPathResource> cell) {
        String prefixUrl = (HTTP_HOST + serverProperties.getPort()) + "/" + cell.getRowKey() + "-" + cell.getColumnKey();
        return ImmutableList.of(
            prefixUrl + ".json",
            //prefixUrl + ".yml",
            //prefixUrl + ".properties",
            (HTTP_HOST + serverProperties.getPort()) + "/" + cell.getRowKey() + "/" + cell.getColumnKey()
        );
    }
}
