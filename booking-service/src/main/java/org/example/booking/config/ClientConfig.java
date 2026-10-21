package org.example.booking.config;

import org.example.booking.client.EventClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfig {

    // @LoadBalanced: имя "event-service" в URL заменяется реальным адресом из Eureka
    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    EventClient eventClient(@LoadBalanced RestClient.Builder builder) {
        RestClient restClient = builder.baseUrl("http://event-service").build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(EventClient.class);
    }
}