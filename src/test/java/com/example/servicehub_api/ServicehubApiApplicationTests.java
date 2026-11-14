package com.example.servicehub_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ServicehubApiApplicationTests {
    @org.springframework.test.context.DynamicPropertySource
    static void configureDatabase(org.springframework.test.context.DynamicPropertyRegistry properties) {
        PostgresTestDatabase.configure(properties);
    }


	@Test
	void contextLoads() {
	}

}
