package com.msb.mall.thirdparty.utils;


import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse;
import com.google.gson.Gson;
import darabonba.core.client.ClientOverrideConfiguration;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class SmsComponent {
    @Value("${spring.cloud.alicloud.access-key}")
    private String accessId;
    @Value("${spring.cloud.alicloud.secret-key}")
    private String accessSecret;
    @Value("${spring.cloud.alicloud.sms.region-id}")
    private String regionId;
    @Value("${spring.cloud.alicloud.sms.endpoint}")
    private String endpoint;
    @Value("${spring.cloud.alicloud.sms.sign-name}")
    private String signName;
    @Value("${spring.cloud.alicloud.sms.template-code}")
    private String templateCode;

    /**
     * 发送短信验证码
     *
     * @param phone 发送的手机号
     * @param code  发送的短信验证码
     */
    public void sendSmsCode(String phone, String code) throws ExecutionException, InterruptedException {
        StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                .accessKeyId(accessId)
                .accessKeySecret(accessSecret)
                .build());

        AsyncClient client = AsyncClient.builder()
                .region(regionId) // Region ID
                .credentialsProvider(provider)
                .overrideConfiguration(
                        ClientOverrideConfiguration.create()
                                .setEndpointOverride(endpoint)
                )
                .build();
        SendSmsRequest sendSmsRequest = SendSmsRequest.builder()
                .signName(signName)
                .templateCode(templateCode)
                .phoneNumbers(phone)
                .templateParam("{\"code\":\"" + code + "\"}")
                .build();
        CompletableFuture<SendSmsResponse> response = client.sendSms(sendSmsRequest);
        SendSmsResponse resp = response.get();
        System.out.println(new Gson().toJson(resp));
        client.close();
    }
}
