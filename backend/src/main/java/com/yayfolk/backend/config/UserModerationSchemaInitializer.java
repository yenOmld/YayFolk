package com.yayfolk.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class UserModerationSchemaInitializer {
    private static final String USERS_TABLE = "users";
    private static final String UNBAN_TABLE = "user_unban_applications";

    private final JdbcTemplate jdbcTemplate;

    public UserModerationSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureSchema() {
        try {
            ensureUserColumns();
            ensureUnbanApplicationTable();
            ensureUnbanApplicationColumns();
            ensureUnbanApplicationIndexes();
        } catch (Exception e) {
            log.error("Failed to initialize user moderation schema", e);
        }
    }

    private void ensureUserColumns() {
        ensureColumn(USERS_TABLE, "ban_reason", "VARCHAR(500) NULL");
        ensureColumn(USERS_TABLE, "ban_admin_id", "BIGINT NULL");
        ensureColumn(USERS_TABLE, "ban_time", "DATETIME NULL");
    }

    private void ensureUnbanApplicationTable() {
        if (tableExists(UNBAN_TABLE)) {
            return;
        }
        String sql = "CREATE TABLE " + UNBAN_TABLE + " ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "user_id BIGINT NOT NULL,"
                + "apply_reason VARCHAR(500) NOT NULL,"
                + "status VARCHAR(20) NOT NULL DEFAULT 'pending',"
                + "admin_id BIGINT NULL,"
                + "admin_remark VARCHAR(500) NULL,"
                + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "handle_time DATETIME NULL,"
                + "update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP"
                + ")";
        jdbcTemplate.execute(sql);
        log.info("Created table: {}", UNBAN_TABLE);
    }

    private void ensureUnbanApplicationColumns() {
        ensureColumn(UNBAN_TABLE, "user_id", "BIGINT NOT NULL");
        ensureColumn(UNBAN_TABLE, "apply_reason", "VARCHAR(500) NOT NULL");
        ensureColumn(UNBAN_TABLE, "status", "VARCHAR(20) NOT NULL DEFAULT 'pending'");
        ensureColumn(UNBAN_TABLE, "admin_id", "BIGINT NULL");
        ensureColumn(UNBAN_TABLE, "admin_remark", "VARCHAR(500) NULL");
        ensureColumn(UNBAN_TABLE, "create_time", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
        ensureColumn(UNBAN_TABLE, "handle_time", "DATETIME NULL");
        ensureColumn(UNBAN_TABLE, "update_time", "DATETIME NULL ON UPDATE CURRENT_TIMESTAMP");
    }

    private void ensureUnbanApplicationIndexes() {
        ensureIndex(UNBAN_TABLE, "idx_unban_user_status", "(user_id, status)");
        ensureIndex(UNBAN_TABLE, "idx_unban_status_create_time", "(status, create_time)");
    }

    private void ensureColumn(String tableName, String columnName, String columnDefinition) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition;
        jdbcTemplate.execute(sql);
        log.info("Added column {}.{}", tableName, columnName);
    }

    private void ensureIndex(String tableName, String indexName, String expression) {
        if (indexExists(tableName, indexName)) {
            return;
        }
        String sql = "CREATE INDEX " + indexName + " ON " + tableName + " " + expression;
        jdbcTemplate.execute(sql);
        log.info("Added index {} on {}", indexName, tableName);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }
}
