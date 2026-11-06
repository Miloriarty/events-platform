package org.example.notification.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

// Hibernate создал в уже существующей таблице CHECK со списком значений NotificationType, а ddl-auto: update
// его не обновляет, поэтому новый тип падал бы на вставке. Выполняется до запуска слушателей очереди
@Component
public class NotificationSchemaFix {

    private final JdbcTemplate jdbc;

    public NotificationSchemaFix(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    void dropTypeCheck() {
        jdbc.execute("alter table if exists notifications drop constraint if exists notifications_type_check");
    }
}
