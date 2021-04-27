package cn.jrymos.configserver.spi.service;

import cn.jrymos.configserver.spi.RefreshMessage;
import cn.jrymos.configserver.spi.config.RefreshMessageConfig;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Order
@Slf4j
public class DefaultRefreshMessageService implements DisposableBean {

    private static final String REFRESH_CONFIG_EXECUTOR = "refresh_config_executor";

    private final ContextRefresher contextRefresher;
    private final RefreshMessageConfig config;
    private final ConfigServicePropertySourceLocator configServicePropertySourceLocator;
    private final ConfigurableApplicationContext context;
    private final CircleList messageList = new CircleList(10);
    // 单线程
    private final ExecutorService synExecutorService = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MINUTES,
        // 每次刷新都是以最新的配置为准，队列最多只需要维护一个刷新任务
        new ArrayBlockingQueue<>(1),
        r -> new Thread(r, REFRESH_CONFIG_EXECUTOR),
        (r, executor) -> log.info("rejectedExecution#正在执行更新config操作"));

    //记录上一次更新的配置
    private volatile CompositePropertySource propertySource;


    /**
     * 每隔30s定时自动刷新
     */
    @Scheduled(fixedDelay = 30 * 1000)
    public synchronized void refreshTestTask() {
        if (!config.isOpenTaskRefresh()) {
            return;
        }
        CompositePropertySource newSource = (CompositePropertySource) configServicePropertySourceLocator.locate(context.getEnvironment());
        if (newSource == null) {
            return;
        }
        // 配置有更新，重新刷新配置
        if (propertySource == null || !Objects.equals(propertySource.getPropertySources(), newSource.getPropertySources())) {
            refresh(RefreshMessage.builder()
                .appName(config.getAppName())
                .profile(config.getProfile())
                .messageId("task:" + System.currentTimeMillis())
                .description("task scheduled")
                .build());
            propertySource = newSource;
        }
    }


    /**
     * 刷新配置
     */
    public void refresh(RefreshMessage refreshMessage) {
        if (refreshMessage.getAppName() != null && !refreshMessage.getAppName().equals(config.getAppName())) {
            log.info("un refresh, appName filter:{}", refreshMessage);
            return;
        }
        if (refreshMessage.getProfile() != null && !refreshMessage.getProfile().equalsIgnoreCase(config.getProfile())) {
            log.info("un refresh, profile filter:{}", refreshMessage);
            return;
        }
        // 服务未完全启动，不进行配置刷新
        if (!config.isCanRefresh()) {
            log.info("un refresh, app is not running filter:{}", refreshMessage);
            return;
        }
        synExecutorService.execute(() -> {
            String messageId = refreshMessage.getMessageId();
            if (messageList.add(messageId)) {
                log.info("refresh, message:{}, messageList:{} start", messageId, messageList);
                contextRefresher.refreshEnvironment();
                log.info("refresh success, message:{}, messageList:{} end", messageId, messageList);
            } else {
                log.info("already handle messageId:{}, messageList:{}", messageId, messageList);
            }
        });
    }


    @Override
    public void destroy() {
        synExecutorService.shutdown();
    }

    @ToString
    public static class CircleList {
        private String[] elements;
        private int index;
        CircleList(int size) {
            this.elements = new String[size];
        }

        boolean add(String message) {
            if (elements.length == 0) {
                return false;
            }
            for (String element : elements) {
                if (Objects.equals(element, message)) {
                    return false;
                }
            }
            index = (index + 1) % elements.length;
            elements[index] = message;
            return true;
        }
    }
}
