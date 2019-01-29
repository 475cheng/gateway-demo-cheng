>springcloud gateway 网关 [gateway 官方文档](http://cloud.spring.io/spring-cloud-gateway/single/spring-cloud-gateway.html)

# 网关组件对比 zuul VS gateway
zuul:  
zuul1 同步阻塞，线程开销大，连接数受限，开源时间6年多，稳定成熟  
zuul2 异步非阻塞，线程开销少，连接数可扩展，刚刚开源不到1年,spring没有集成zuul2 而是用自己开源的gateway（当然如果量级没有达到很大，使用zuul1也完全可以）  

gateway:  
springcloud子项目与spring无缝整合，上手简单，异步非阻塞，性能比zuul1好一点  
功能与zuul类似：权限认证，限流，路由分发，熔断，响应处理  
总结一下gateway的特性:
	1).动态路由
	2).易于编写的 Predicates(断言) 和 Filters(过滤器)
	3).限流
	4).集成 Eureka 默认路由
	5).集成 Hystrix 断路器
	6).路由超时
# springboot接入gateway
1.引入pom.xml
```xml
<project>
<parent>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-parent</artifactId>
	<version>2.0.5.RELEASE</version>
</parent>
<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-dependencies</artifactId>
			<version>Finchley.SR1</version>
			<type>pom</type>
			<scope>import</scope>
		</dependency>
	</dependencies>
</dependencyManagement>
<dependencies>
	<dependency>
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-starter-gateway</artifactId>
	</dependency>
	<dependency>
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
	</dependency>
</dependencies>
</project>
```  

2.添加路由规则，有两种形式，代码或者配置文件  
   1).代码形式
```java
@Configuration
public class RouteLocatorBean {
    public static String httpUriA = "http://www.baidu.com";
    public static String httpUriB = "http://www.baidu.com/endpoint/endpointB";
    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        
        return builder.routes()
                .route(p -> p
                        .path("/getmessage")										
                        .filters(f -> f.hystrix(config -> config            //404不会触发断路，服务停止才会触发forward
                                .setName("myCommandName")                   
                                .setFallbackUri("forward:/fallback")))		//1.对请求路径为/getmessage的，路由到httpUriB这个地址，如果这个地址服务挂掉，进行路由熔断，重定到这个地址/fallback
                        .uri(httpUriB))										//uri这里可以是域名，它会自动将请求路径追加到域名后面
                .route(p -> p
                        .path("/endpointA")
                        .filters(f -> f.prefixPath("/endpoint"))
                        .uri(httpUriA))										//2.对于请求路径为/endpointA的，对于请求路径包装，前缀添加/endpoint,最终访问路径为http://www.baidu.com/endpoint/endpointA
                .route(p -> p
                        .query("name", "shen")
                         .filters(f -> f.addRequestHeader("username","test"))
                        .uri(httpUriB))										//3.对于请求中携带的参数为http://baidu.com?name=shen的,最终路由到httpUriB这个地址，并且在请求头中添加username=test
                .route(p -> p
                        .path("/fallback")                                  //4.如果gateway服务没有fallback端点，但是别的服务有fallback端点，可以这样配置
                        .uri("http://localhost:8888"))                      //那么上面熔断的路由都会请求带这个地址http://localhost:8888/fallback
                .build();
    }
}
```  

   2). 配置文件形式 以application.yml格式为例  

```application.yml
spring:
  cloud:
    gateway:
      enabled: true		//默认为true启动网关，如果项目中引用的jar包而不想启用网关，可以改为false
      routes:
      - id: path_route_hys
        uri: http://localhost:8888/endpoint/endpointB
        predicates:
        - Path=/getmessage  //特别注意：- Path =/getmessage 这样格式的话，gateway的所有配置都会失效
        filters:
        - name: Hystrix
          args:
            name: test_hystrix
            fallbackUri: forward:/fallback
      - id: path_route_pre
        uri: http://localhost:8888
        predicates:
        - Path=/endpointA
        filters:
        - PrefixPath=/endpoint
      - id: query_route
        uri: http://localhost:8888/endpoint/endpointB
        predicates:
        - Query=name, shen
        filters:
        - AddRequestHeader=username, test

hystrix:
  command:
    test_hystrix:              //这个地方是上面hystrix的name,timeout默认是1s,   default为全局配置默认
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 9000
```  

3.集成Eureka  
   1).pom添加
```pom.xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
接口用于重试
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```  

   2).以配置文件形式为例 yml格式为例
```yml
spring:	
  cloud:
    loadbalancer:
      retry:
        enabled: true   //开启失败重试
    gateway:
      discovery:
        locator:
          enabled: true				//当访问http://网关地址/服务名称（大写）/**地址会自动转发到http://服务名称（大写）/**地址，如果为false就不会自动转发
          lowerCaseServiceId: true	//为true表示服务名称（小写）
      routes:
      - id: service-hi
        uri: lb://SERVICE-HI		//lb代表从注册中心获取服务，后面接的就是你需要转发到的服务名称 不能填uri只能是服务名称
        predicates:
        - Path=/demo/**
        filters:
        - name: Retry
          args:
            retries: 3              //在Eureka注册列表中有一个节点挂掉了，在短时间内，列表没有更新，还会调用挂掉的节点，可以通过失败重试调用其他节点
            statuses: BAD_GATEWAY
      
eureka:
  instance:
    prefer-ip-address: true
  client:
    service-url:
      defaultZone: http://123456@master:8761/eureka/
```  

4.gateway 自定义全局过滤器 （如：token过滤器）  

创建完全局过滤器，加上@Component注解就会生效  

```java
@Component
public class TokenFilter implements GlobalFilter, Ordered {

    Logger logger=LoggerFactory.getLogger( TokenFilter.class );
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getQueryParams().getFirst("token");
        if (token == null || token.isEmpty()) {
            logger.info( "token is empty..." );
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
```  

5.gateway 自定义过滤器filter  
  
   1).创建自定义过滤器  

```java
public class RequestTimeGatewayFilterFactory extends AbstractGatewayFilterFactory<RequestTimeGatewayFilterFactory.Config> {	//泛型用静态内部类
    private static final Log log = LogFactory.getLog(GatewayFilter.class);
    private static final String REQUEST_TIME_BEGIN = "requestTimeBegin";
    private static final String KEY = "withParams";

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList(KEY);
    }

    public RequestTimeGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            exchange.getAttributes().put(REQUEST_TIME_BEGIN, System.currentTimeMillis());
            return chain.filter(exchange).then(
                    Mono.fromRunnable(() -> {
                        Long startTime = exchange.getAttribute(REQUEST_TIME_BEGIN);
                        if (startTime != null) {
                            StringBuilder sb = new StringBuilder(exchange.getRequest().getURI().getRawPath())
                                    .append(": ")
                                    .append(System.currentTimeMillis() - startTime)
                                    .append("ms");
                            if (config.isWithParams()) {
                                sb.append(" params:").append(exchange.getRequest().getQueryParams());
                            }
                            log.info(sb.toString());
                        }
                    })
            );
        };
    }
    public static class Config {
        private boolean withParams;
        public boolean isWithParams() {
            return withParams;
        }
        public void setWithParams(boolean withParams) {
            this.withParams = withParams;
        }

    }
}
```  
2).添加bean中  

```java
@Configuration
public class FilterBeanFactory {
    @Bean
    public RequestTimeGatewayFilterFactory elapsedGatewayFilterFactory() {
        return new RequestTimeGatewayFilterFactory();
    }
}
```  

3).引用自定义过滤器  

```yml
spring:
  cloud:
    gateway:
      routes:
      - id: add_request_header_route
        uri: http://localhost:8888/endpoint/endpointA
        filters:
        - RequestTime=true						//此处为因为自定义过滤器
```

