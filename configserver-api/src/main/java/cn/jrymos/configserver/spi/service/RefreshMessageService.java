package cn.jrymos.configserver.spi.service;

import cn.jrymos.configserver.spi.RefreshMessage;

/**
 * 刷新配置
 * 允许业务方重新实现RefreshMessageService，来满足自己的需求
 * @see DefaultRefreshMessageService 默认的实现
 */
public interface RefreshMessageService {
    void refresh(RefreshMessage refreshMessage);
}
