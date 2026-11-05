package org.example.notification.config;

import org.example.notification.client.UserClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfig {

    // Балансировщик подключается только к этому клиенту, а не ко всем RestClient в приложении
    // Иначе его получит и сам клиент Eureka, и ему придётся искать Eureka через Eureka
    @Bean
    UserClient userClient(LoadBalancerClient loadBalancerClient) {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://user-service")
                .requestInterceptor(new LoadBalancerInterceptor(
                        loadBalancerClient,
                        new LoadBalancerRequestFactory(loadBalancerClient)))
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(UserClient.class);
    }
}
