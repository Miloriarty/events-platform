package org.example.reminder;

import org.example.reminder.client.EventClient;
import org.example.reminder.client.EventInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EventClientTest {

    @Test
    void sendsDateInIsoFormat() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://event-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://event-service/api/events/starting?date=2026-11-07"))
                .andRespond(withSuccess("[{\"id\":1,\"title\":\"Meetup\",\"startDate\":\"2026-11-07\",\"extra\":1}]",
                        MediaType.APPLICATION_JSON));
        EventClient client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build()))
                .build().createClient(EventClient.class);

        List<EventInfo> events = client.findStarting(LocalDate.of(2026, 11, 7));

        assertEquals(List.of(new EventInfo(1L, "Meetup", LocalDate.of(2026, 11, 7))), events);
        server.verify();
    }
}
