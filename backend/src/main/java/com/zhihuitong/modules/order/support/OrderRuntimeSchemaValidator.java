package com.zhihuitong.modules.order.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderRuntimeSchemaValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderRuntimeSchemaValidator.class);

    private final JdbcTemplate jdbcTemplate;

    public OrderRuntimeSchemaValidator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        validateSchema();
    }

    void validateSchema() {
        List<String> missingArtifacts = new ArrayList<>();
        requireTable(
                "orderinfo",
                List.of("orderId", "orderNo", "customerName", "orderDate", "deliveryDate", "status", "createTime"),
                List.of("uk_orderinfo_orderNo"),
                missingArtifacts
        );
        requireTable(
                "orderitem",
                List.of("orderItemId", "orderId", "productCode", "productName", "quantity"),
                List.of("idx_orderitem_orderId"),
                missingArtifacts
        );
        requireTable(
                "orderbatchlink",
                List.of("id", "orderId", "orderItemId", "batchId", "allocatedWeight", "allocatedQuantity"),
                List.of("idx_orderbatchlink_orderId", "idx_orderbatchlink_orderItemId", "idx_orderbatchlink_batchId"),
                missingArtifacts
        );
        requireTable(
                "productionplan",
                List.of("planId", "batchId", "routeId", "orderId", "orderItemId"),
                List.of("batchId", "routeId", "idx_productionplan_orderId", "idx_productionplan_orderItemId"),
                missingArtifacts
        );
        if (!missingArtifacts.isEmpty()) {
            throw new IllegalStateException(
                    "Order-domain runtime schema validation failed. Missing artifacts: "
                            + String.join("; ", missingArtifacts)
                            + ". Apply docs/order_schema_upgrade.sql to the current database and restart the backend."
            );
        }
        log.info("Order-domain runtime schema validation passed.");
    }

    private void requireTable(String tableName,
                              List<String> requiredColumns,
                              List<String> requiredIndexes,
                              List<String> missingArtifacts) {
        if (!tableExists(tableName)) {
            missingArtifacts.add("table `" + tableName + "`");
            return;
        }
        requiredColumns.stream()
                .filter(columnName -> !columnExists(tableName, columnName))
                .map(columnName -> "column `" + tableName + "." + columnName + "`")
                .forEach(missingArtifacts::add);
        requiredIndexes.stream()
                .filter(indexName -> !indexExists(tableName, indexName))
                .map(indexName -> "index `" + tableName + "." + indexName + "`")
                .forEach(missingArtifacts::add);
    }

    private boolean tableExists(String tableName) {
        return queryCount(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                tableName
        ) > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        return queryCount(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                tableName,
                columnName
        ) > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        return queryCount(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                tableName,
                indexName
        ) > 0;
    }

    private int queryCount(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }
}
