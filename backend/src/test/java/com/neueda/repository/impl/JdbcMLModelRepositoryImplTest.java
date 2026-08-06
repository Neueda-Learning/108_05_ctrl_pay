package com.neueda.repository.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.neueda.domain.MLModelRecord;

@ExtendWith(MockitoExtension.class)
class JdbcMLModelRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcMLModelRepositoryImpl repository;
    private MLModelRecord sampleModel;

    @BeforeEach
    void setUp() {
        repository = new JdbcMLModelRepositoryImpl(jdbcTemplate);
        sampleModel = MLModelRecord.create(
            "Model1", "1.0.0", "Desc", "XGBOOST", "/path", LocalDateTime.now(),
            "dataset", 1000, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
            BigDecimal.ONE, BigDecimal.ONE, "PROD", "ADMIN"
        );
    }

    @Test
    @DisplayName("findActiveModel: Returns active model")
    void findActiveModel_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(List.of(sampleModel));

        Optional<MLModelRecord> model = repository.findActiveModel();

        assertTrue(model.isPresent());
        assertEquals("Model1", model.get().modelName());
    }

    @Test
    @DisplayName("findByModelName: Returns models by name")
    void findByModelName_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("Model1")))
            .thenReturn(List.of(sampleModel));

        List<MLModelRecord> models = repository.findByModelName("Model1");

        assertNotNull(models);
        assertEquals(1, models.size());
    }
}
