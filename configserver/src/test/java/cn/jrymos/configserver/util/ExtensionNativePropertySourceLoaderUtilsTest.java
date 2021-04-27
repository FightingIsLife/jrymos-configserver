package cn.jrymos.configserver.util;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;


public class ExtensionNativePropertySourceLoaderUtilsTest {

    /**
     * 验证获取 /config/extension 目录下的 test 应用的 dev 环境配置
     */
    @Test
    public void loadEnvironment() throws IOException {
        ArrayList<ClassPathResource> warningFiles = new ArrayList<>();
        Table<String, String, ClassPathResource> applicationToProfileToFiles = HashBasedTable.create();
        String basePath = "classpath:/config/extension";
        applicationToProfileToFiles.put("test", "dev", ClassPathResourceUtils.get(basePath, "test", "dev"));
        applicationToProfileToFiles.put("ttest2", "dev", ClassPathResourceUtils.get(basePath, "ttest2", "dev"));
        Environment environment = ExtensionNativePropertySourceLoaderUtils.loadEnvironment(applicationToProfileToFiles, "test", "dev", warningFiles);
        Assert.assertEquals(0, warningFiles.size());
        Assert.assertEquals("test", environment.getName());
        Assert.assertEquals(4, environment.getPropertySources().size());
        Assert.assertEquals("train", environment.getPropertySources().get(0).getName());
        Assert.assertEquals("a hello", environment.getPropertySources().get(0).getSource().get("train.test"));
        Assert.assertEquals("other-test1", environment.getPropertySources().get(0).getSource().get("train.table[0].title"));
        Assert.assertEquals("other-ttest2", environment.getPropertySources().get(0).getSource().get("train.table[1].title"));
        Assert.assertEquals("vip", environment.getPropertySources().get(1).getName());
        Assert.assertEquals("word.a", environment.getPropertySources().get(2).getName());
        Assert.assertEquals("word.c", environment.getPropertySources().get(3).getName());
        Assert.assertEquals("非常强", environment.getPropertySources().get(2).getSource().get("word.a.wumii"));
    }

    @Test
    public void loadEnvironmentWarningFiles() throws IOException {
        final ArrayList<ClassPathResource> warningFiles = new ArrayList<>();
        Table<String, String, ClassPathResource> applicationToProfileToFiles = HashBasedTable.create();
        String basePath = "classpath:/config/extension2";
        applicationToProfileToFiles.put("test", "dev", ClassPathResourceUtils.get(basePath, "test", "dev"));
        applicationToProfileToFiles.put("test", "gamma", ClassPathResourceUtils.get(basePath, "test", "gamma"));
        applicationToProfileToFiles.put("test", "gamma.dev", ClassPathResourceUtils.get(basePath, "test", "gamma.dev"));
        applicationToProfileToFiles.put("test-test", "dev", ClassPathResourceUtils.get(basePath, "test-test", "dev"));
        ExtensionNativePropertySourceLoaderUtils.loadEnvironment(applicationToProfileToFiles,"test-test", "dev", warningFiles);
        Assert.assertEquals(2, warningFiles.size());
        Assert.assertEquals(ClassPathResourceUtils.get(basePath, "test", "dev"), warningFiles.get(0));
        Assert.assertEquals(ClassPathResourceUtils.get(basePath, "test-test", "dev"), warningFiles.get(1));
    }

    @Test
    public void initApplicationToProfileToFiles() throws IOException {
        String basePath = "classpath:/config/extension";
        ExtensionNativePropertySourceLoaderUtils.initApplicationToProfileToFiles(basePath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void initApplicationToProfileToFiles2() throws IOException {
        String basePath = "classpath:/config/extension2";
        ExtensionNativePropertySourceLoaderUtils.initApplicationToProfileToFiles(basePath);
    }

}