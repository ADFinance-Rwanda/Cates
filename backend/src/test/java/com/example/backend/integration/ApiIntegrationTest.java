package com.example.backend.integration;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public abstract class ApiIntegrationTest extends AbstractIntegrationTest {

    static final String SUPER = "11111111-1111-1111-1111-111111111111";
    static final String ALICE = "22222222-2222-2222-2222-222222222222";
    static final String BOB = "33333333-3333-3333-3333-333333333333";
    static final String CARA = "44444444-4444-4444-4444-444444444444";
    static final String DAVE = "55555555-5555-5555-5555-555555555555";
    static final String ACME = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    static final String GLOBEX = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    static final String ACME_PROJECT = "66666666-6666-6666-6666-666666666666";
    static final String ACME_TASK = "88888888-8888-8888-8888-888888888888";
    static final String GLOBEX_TASK = "99999999-9999-9999-9999-999999999999";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbc;
}
