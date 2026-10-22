package org.example.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Set;

// Правила по путям и ролям. Проверяются сверху вниз, срабатывает первое подходящее
@Component
public class AccessRules {

    public enum Access { PUBLIC, AUTHENTICATED, MANAGER, ADMIN }

    private record Rule(HttpMethod method, PathPattern pattern, Access access) {
        boolean matches(HttpMethod m, PathContainer path) {
            return (method == null || method.equals(m)) && pattern.matches(path);
        }
    }

    private static final Set<String> MANAGER_ROLES = Set.of("MANAGER", "ADMIN");

    private final List<Rule> rules = List.of(
            rule(null, "/api/auth/**", Access.PUBLIC),
            rule(HttpMethod.GET, "/*/v3/api-docs/**", Access.PUBLIC),

            // список своих мероприятий требует X-User-Id, поэтому он не публичный
            rule(HttpMethod.GET, "/api/events/my", Access.AUTHENTICATED),
            rule(HttpMethod.GET, "/api/events/**", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/categories/**", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/locations/**", Access.PUBLIC),
            rule(null, "/api/events/**", Access.MANAGER),
            rule(null, "/api/categories/**", Access.MANAGER),
            rule(null, "/api/locations/**", Access.MANAGER),

            rule(null, "/api/users/**", Access.ADMIN),

            rule(HttpMethod.GET, "/api/tickets/**", Access.AUTHENTICATED),
            rule(null, "/api/tickets/**", Access.MANAGER),
            rule(null, "/api/orders/**", Access.AUTHENTICATED),
            rule(null, "/api/registrations/**", Access.AUTHENTICATED)
    );

    public Access required(HttpMethod method, PathContainer path) {
        return rules.stream()
                .filter(r -> r.matches(method, path))
                .map(Rule::access)
                .findFirst()
                .orElse(Access.AUTHENTICATED);
    }

    public boolean allows(Access access, String role) {
        return switch (access) {
            case PUBLIC, AUTHENTICATED -> true;
            case MANAGER -> MANAGER_ROLES.contains(role);
            case ADMIN -> "ADMIN".equals(role);
        };
    }

    private static Rule rule(HttpMethod method, String pattern, Access access) {
        return new Rule(method, PathPatternParser.defaultInstance.parse(pattern), access);
    }
}
