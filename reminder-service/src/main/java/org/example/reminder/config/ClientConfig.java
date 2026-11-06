package org.example.reminder.config;

import org.example.reminder.client.BookingClient;
import org.example.reminder.client.EventClient;
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

    @Bean
    EventClient eventClient(LoadBalancerClient loadBalancerClient) {
        return client("http://event-service", EventClient.class, loadBalancerClient);
    }

    @Bean
    BookingClient bookingClient(LoadBalancerClient loadBalancerClient) {
        return client("http://booking-service", BookingClient.class, loadBalancerClient);
    }

    // Балансировщик подключается только к этим клиентам, а не ко всем RestClient в приложении
    // Иначе его получит и сам клиент Eureka, и ему придется искать Eureka через Eureka
    private <T> T client(String baseUrl, Class<T> type, LoadBalancerClient loadBalancerClient) {
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor(new LoadBalancerInterceptor(
                        loadBalancerClient,
                        new LoadBalancerRequestFactory(loadBalancerClient)))
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(type);
    }
}
