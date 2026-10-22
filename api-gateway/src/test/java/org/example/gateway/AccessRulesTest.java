package org.example.gateway;

import org.example.gateway.security.AccessRules;
import org.example.gateway.security.AccessRules.Access;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessRulesTest {

    private final AccessRules rules = new AccessRules();

    private Access required(HttpMethod method, String path) {
        return rules.required(method, PathContainer.parsePath(path));
    }

    @Test
    void publicPaths() {
        assertEquals(Access.PUBLIC, required(HttpMethod.POST, "/api/auth/login"));
        assertEquals(Access.PUBLIC, required(HttpMethod.GET, "/api/events"));
        assertEquals(Access.PUBLIC, required(HttpMethod.GET, "/api/events/5"));
        assertEquals(Access.PUBLIC, required(HttpMethod.GET, "/api/categories"));
        assertEquals(Access.PUBLIC, required(HttpMethod.GET, "/event-service/v3/api-docs"));
    }

    @Test
    void myEventsNeedUser() {
        assertEquals(Access.AUTHENTICATED, required(HttpMethod.GET, "/api/events/my"));
    }

    @Test
    void catalogChangesNeedManager() {
        assertEquals(Access.MANAGER, required(HttpMethod.POST, "/api/events"));
        assertEquals(Access.MANAGER, required(HttpMethod.DELETE, "/api/locations/1"));
        assertEquals(Access.MANAGER, required(HttpMethod.PUT, "/api/tickets/1"));
        assertEquals(Access.AUTHENTICATED, required(HttpMethod.GET, "/api/tickets"));
    }

    @Test
    void usersOnlyForAdmin() {
        assertEquals(Access.ADMIN, required(HttpMethod.GET, "/api/users"));
        assertFalse(rules.allows(Access.ADMIN, "MANAGER"));
        assertTrue(rules.allows(Access.ADMIN, "ADMIN"));
    }

    @Test
    void bookingNeedsUser() {
        assertEquals(Access.AUTHENTICATED, required(HttpMethod.POST, "/api/orders"));
        assertEquals(Access.AUTHENTICATED, required(HttpMethod.GET, "/api/registrations/event/3"));
        assertTrue(rules.allows(Access.MANAGER, "MANAGER"));
        assertFalse(rules.allows(Access.MANAGER, "USER"));
    }

    @Test
    void unknownPathNeedsUser() {
        assertEquals(Access.AUTHENTICATED, required(HttpMethod.GET, "/something"));
    }
}
