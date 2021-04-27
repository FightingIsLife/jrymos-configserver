package cn.jrymos.configserver.notify;

import cn.jrymos.configserver.spi.RefreshMessage;

/**
 * 通知客户端刷新config配置
 */
public interface RefreshClientConfigNotifier {
    void notice(RefreshMessage message);
}
