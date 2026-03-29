package com.yayfolk.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class ModerationSchemaInitializer {
    private static final String TABLE_NAME = "post_reports";
    private final JdbcTemplate jdbcTemplate;

    public ModerationSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensurePostReportSchema() {
        try {
            createTableIfMissing();
            ensureColumn("reason", "VARCHAR(500) NULL");
            ensureColumn("status", "VARCHAR(20) NOT NULL DEFAULT 'pending'");
            ensureColumn("handler_id", "BIGINT NULL");
            ensureColumn("handle_time", "DATETIME NULL");
            ensureColumn("create_time", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureIndex("idx_post_reports_post_status", "(post_id, status)");
            ensureIndex("idx_post_reports_reporter_status", "(reporter_id, status)");
        } catch (Exception e) {
            log.error("Failed to initialize moderation schema", e);
        }
    }

    private void createTableIfMissing() {
        if (tableExists(TABLE_NAME)) {
            return;
        }
        String sql = "CREATE TABLE " + TABLE_NAME + " ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "post_id BIGINT NOT NULL,"
                + "reporter_id BIGINT NOT NULL,"
                + "reason VARCHAR(500) NULL,"
                + "status VARCHAR(20) NOT NULL DEFAULT 'pending',"
                + "handler_id BIGINT NULL,"
                + "handle_time DATETIME NULL,"
                + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX idx_post_reports_post_status (post_id, status),"
                + "INDEX idx_post_reports_reporter_status (reporter_id, status)"
                + ")";
        jdbcTemplate.execute(sql);
        log.info("Created table: {}", TABLE_NAME);
    }

    private void ensureColumn(String columnName, String columnDefinition) {
        if (columnExists(TABLE_NAME, columnName)) {
            return;
        }
        String sql = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + columnName + " " + columnDefinition;
        jdbcTemplate.execute(sql);
        log.info("Added column {}.{}", TABLE_NAME, columnName);
    }

    private void ensureIndex(String indexName, String expression) {
        if (indexExists(TABLE_NAME, indexName)) {
            return;
        }
        String sql = "CREATE INDEX " + indexName + " ON " + TABLE_NAME + " " + expression;
        jdbcTemplate.execute(sql);
        log.info("Added index {} on {}", indexName, TABLE_NAME);
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
