package cn.jrymos.configserver.notify;

import cn.jrymos.configserver.spi.RefreshMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * 通知客户端刷新配置消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRefreshClientConfigMqNotifier implements RefreshClientConfigNotifier, InitializingBean {

    @Override
    public void afterPropertiesSet() {
        notice(RefreshMessage.builder()
            .messageId(String.valueOf(System.currentTimeMillis()))
            .build());
    }


    @Override
    public void notice(RefreshMessage message) {
        log.info("notice, start:{}", message);
        // do nothing
        log.info("notice, failed:{}", message);
    }

}
