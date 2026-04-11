package com.wipro.fraud.aiassistant.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DatabaseQueryService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> executeQuery(String sql) {
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        return jdbcTemplate.queryForList(sql, params);
    }
}
