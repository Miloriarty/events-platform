package org.example.gateway.filter;

import org.example.gateway.security.AccessRules;
import org.example.gateway.security.AccessRules.Access;
import org.example.gateway.security.AuthUser;
import org.example.gateway.security.JwtVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtVerifier jwt;
    private final AccessRules rules;

    public AuthFilter(JwtVerifier jwt, AccessRules rules) {
        this.jwt = jwt;
        this.rules = rules;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        Optional<AuthUser> user = Optional.empty();
        if (header != null) {
            if (!header.startsWith(BEARER)) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Expected Bearer token");
            }
            user = jwt.verify(header.substring(BEARER.length()));
            if (user.isEmpty()) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            }
        }

        Access access = rules.required(request.getMethod(), request.getPath().pathWithinApplication());
        if (access != Access.PUBLIC) {
            if (user.isEmpty()) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Authentication required");
            }
            if (!rules.allows(access, user.get().role())) {
                log.info("Access denied: user {} with role {} to {} {}",
                        user.get().id(), user.get().role(), request.getMethod(), request.getPath());
                return reject(exchange, HttpStatus.FORBIDDEN, "Access denied");
            }
        }

        // Заголовки от клиента удаляются всегда, иначе можно прислать X-User-Role: ADMIN самому
        Optional<AuthUser> identity = user;
        ServerHttpRequest forwarded = request.mutate()
                .headers(h -> {
                    h.remove(USER_ID);
                    h.remove(USER_ROLE);
                    identity.ifPresent(u -> {
                        h.set(USER_ID, u.id());
                        h.set(USER_ROLE, u.role());
                    });
                })
                .build();
        return chain.filter(exchange.mutate().request(forwarded).build());
    }

    // до маршрутизации и балансировщика
    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String detail) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        String body = "{\"status\":%d,\"title\":\"%s\",\"detail\":\"%s\",\"instance\":\"%s\"}"
                .formatted(status.value(), status.getReasonPhrase(), detail, exchange.getRequest().getPath());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
