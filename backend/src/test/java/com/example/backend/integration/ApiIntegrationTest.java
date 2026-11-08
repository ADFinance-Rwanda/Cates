package com.example.backend.integration;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public abstract class ApiIntegrationTest extends AbstractIntegrationTest {

    static final String SUPER = "4809e900-e596-47bb-9f2d-9225536bd8a6";
    static final String ALICE = "21b9a0ed-3d21-46ff-8bc4-83225f36dfa4";
    static final String BOB = "d6773c12-5e75-4741-8913-36bb8a0e46cc";
    static final String CARA = "30fc848c-06c5-4ef3-95e9-44bb31ed6b3c";
    static final String DAVE = "d79a9fb0-750b-47d5-9c19-95a92a7eb9da";
    static final String MERIDIAN = "c2f0c8b6-ab19-475e-ae57-09a4c2c329b6";
    static final String NORTHLINE = "3d5c8b81-9c1e-4751-b52c-d9d8d791d28e";
    static final String MERIDIAN_PROJECT = "31b5edc0-ba2b-45d8-88a0-7938f4d3ddfa";
    static final String NORTHLINE_PROJECT = "8d35bc9f-ff06-4d82-9f70-b9846af084bc";
    static final String MERIDIAN_TASK = "d12c4aa9-8412-4d02-9ebd-7ae53edb979d";
    static final String NORTHLINE_TASK = "02d6b8d2-d8f7-4ac4-9864-40b1f7fd4a8d";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbc;
}
