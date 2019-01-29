package com.bitauto.ep.fx.webapi.configure;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;



@Configuration
public class MyRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        requestTemplate.header("md5", "MyRequestInterceptor");       //header中添加token
        requestTemplate.query("token", "MyRequestInterceptor");//param中添加token 如 www.baidu.com?token=
    }
}
