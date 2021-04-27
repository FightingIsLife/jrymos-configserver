package cn.jrymos.configserver.spi;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class RefreshMessage {
    private String messageId;
    private String appName;
    private String profile;
    private String description;


    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}