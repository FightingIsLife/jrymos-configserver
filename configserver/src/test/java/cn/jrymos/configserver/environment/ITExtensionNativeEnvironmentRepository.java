package cn.jrymos.configserver.environment;

import com.alibaba.fastjson.JSON;
import cn.jrymos.configserver.test.AbstractITTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.environment.Environment;

public class ITExtensionNativeEnvironmentRepository extends AbstractITTest {

    @Autowired
    private ExtensionNativeEnvironmentRepository extensionNativeEnvironmentRepository;

    @Test
    public void findOne() {
        Environment environment = extensionNativeEnvironmentRepository.findOne("test", "dev", "master");
        String result = "{\"label\":\"master\",\"name\":\"test\",\"profiles\":[\"dev\"],\"propertySources\":" +
            "[{\"name\":\"train\",\"source\":{\"train.test\":\"a hello\",\"train.table[0].title\":\"other-test1\"," +
            "\"train.table[1].title\":\"other-ttest2\"}},{\"name\":\"vip\",\"source\":{\"vip.test\":\"vip\",\"vip.table.title\":" +
            "\"vip\"}},{\"name\":\"word.a\",\"source\"" +
            ":{\"word.a.wumii\":\"非常强\"}},{\"name\":\"word.c\",\"source\":{\"word.c.wumii\":\"很强\"}}]}";
        Assert.assertEquals(result, JSON.toJSONString(environment));
    }
}