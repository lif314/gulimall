package com.lif314.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Autowired
    OSSClient ossClient;

    @Test
    void contextLoads() throws FileNotFoundException {
        ossClient.putObject("gulimall-lif314", "gulimall-3th.png", new FileInputStream("C:\\Users\\lilinfei\\Pictures\\github.png"));
        System.out.println("上传成功 ------");
    }

    @Test
    public void uploadFileOssClient() throws FileNotFoundException {
        ossClient.putObject("gulimall-lif314", "gulimall-3th.png", new FileInputStream("C:\\Users\\lilinfei\\Pictures\\github.png"));
        System.out.println("上传成功 ------");
    }

}
