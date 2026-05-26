package com.zhihuitong.modules.order.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRuntimeSchemaValidatorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private OrderRuntimeSchemaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OrderRuntimeSchemaValidator(jdbcTemplate);
    }

    @Test
    void validateSchemaShouldPassWhenAllArtifactsExist() {
        mockSchemaState(null, null, null);

        assertThatCode(() -> validator.validateSchema()).doesNotThrowAnyException();
    }

    @Test
    void validateSchemaShouldFailFastWhenProductionPlanOrderItemIdIsMissing() {
        mockSchemaState(null, "orderItemId", null);

        assertThatThrownBy(() -> validator.validateSchema())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("column `productionplan.orderItemId`")
                .hasMessageContaining("docs/order_schema_upgrade.sql");
    }

    private void mockSchemaState(String missingTable, String missingColumn, String missingIndex) {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenAnswer(invocation -> {
                    Object[] parameters = {invocation.getArguments()[2]};
                    return answerQuery(
                            invocation.getArgument(0, String.class),
                            missingTable,
                            missingColumn,
                            missingIndex,
                            parameters
                    );
                });
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
                .thenAnswer(invocation -> {
                    Object[] parameters = {invocation.getArguments()[2], invocation.getArguments()[3]};
                    return answerQuery(
                            invocation.getArgument(0, String.class),
                            missingTable,
                            missingColumn,
                            missingIndex,
                            parameters
                    );
                });
    }

    private int answerQuery(String sql,
                            String missingTable,
                            String missingColumn,
                            String missingIndex,
                            Object... args) {
        if (sql.contains("information_schema.TABLES")) {
            String tableName = String.valueOf(args[0]);
            return tableName.equals(missingTable) ? 0 : 1;
        }
        if (sql.contains("information_schema.COLUMNS")) {
            String tableName = String.valueOf(args[0]);
            String columnName = String.valueOf(args[1]);
            return (missingTable != null && tableName.equals(missingTable))
                    || (tableName.equals("productionplan") && columnName.equals(missingColumn)) ? 0 : 1;
        }
        if (sql.contains("information_schema.STATISTICS")) {
            String tableName = String.valueOf(args[0]);
            String indexName = String.valueOf(args[1]);
            return (missingTable != null && tableName.equals(missingTable))
                    || (missingIndex != null && indexName.equals(missingIndex)) ? 0 : 1;
        }
        return 1;
    }
}
