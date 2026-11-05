package org.example.notification.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/api/users")
public interface UserClient {

    @GetExchange("/{id}")
    UserInfo getUser(@PathVariable("id") Long id);
}
