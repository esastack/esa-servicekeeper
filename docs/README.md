# ServiceKeeper

ServiceKeeper is a lightweight service governance framework that provides many awesome features such as rate limit, concurrent limit, circuit breaker,
retry and fallback... You can get start and customize the configuration easily with `annotation`.

# Features
- Concurrent Limit
- Rate Limit
- Circuit Breaker
- Fallback
- Manual fallback
- Parameter-level concurrent limit, circuit breaker and such on

# Quick Start
Step one: Add maven dependency
```xml
<dependency>
    <groupId>io.esastack</groupId>
    <artifactId>servicekeeper-springboot-adapter</artifactId>
    <version>${servicekeeper.version}</version>
</dependency>
```

Step two: Add customize configuration by annotation
```java
@SpringBootApplication
public class AppMain {

    @Bean
    public HelloService helloService() {
        return new HelloService();
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(AppMain.class);

        final HelloService service = ctx.getBean(HelloService.class);
        int errorCount = 0;
        for (int i = 0 ; i < 20; i++) {
            try {
                service.hello();
            } catch (RateLimitOverFlowException ex) {
                errorCount++;
            }
        }

        System.out.println("RateLimitOverFlowException count: " + errorCount);
        ctx.close();
    }

    public class HelloService {

        @RateLimiter(limitForPeriod = 10)
        public String hello() {
            return "Hello World!";
        }

    }
}
```
