package com.bitauto.ep.fx.webapi.configure;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 路由配置类 支持本地配置  和 配置文件配置
 */
@Configuration
public class RouteLocatorBean {
    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        String httpUriC = "lb:service-hi";
        return builder.routes()
                .route(p -> p
                        .path("/get/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(httpUriC))
                .build();
    }
    @Bean
    public RequestTimeGatewayFilterFactory elapsedGatewayFilterFactory() {
        return new RequestTimeGatewayFilterFactory();
    }
}
