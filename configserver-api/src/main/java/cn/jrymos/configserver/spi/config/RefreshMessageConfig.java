package cn.jrymos.configserver.spi.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RefreshMessageConfig {
    private String appName = "sky";
    private String profile = "alpha";
    private volatile boolean canRefresh = false; //需要在应用启动完成之后打开
    private boolean openTaskRefresh = true;
}
