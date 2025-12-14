/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.falkordb.core.DefaultFalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBEntityConverter;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBMappingContext;
import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
import org.springframework.data.falkordb.security.audit.AuditLogger;
import org.springframework.data.falkordb.security.model.AuditLog;
import org.springframework.data.mapping.model.EntityInstantiators;

import com.falkordb.Driver;
import com.falkordb.impl.api.DriverImpl;

class AuditLoggerIntegrationTest {

	private AnnotationConfigApplicationContext context;
	private FalkorDBTemplate template;
	private AuditLogger auditLogger;

	@BeforeEach
	void setUp() {
		context = new AnnotationConfigApplicationContext(TestConfig.class);
		template = context.getBean(FalkorDBTemplate.class);
		auditLogger = new AuditLogger(template, true);

		// Ensure clean state.
		template.deleteAll(AuditLog.class);
	}

	@AfterEach
	void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	void shouldPersistAndQueryAuditLogsWithFilters() {
		Instant start = Instant.now().minusSeconds(60);

		auditLogger.log(1L, "alice", "READ", "com.acme.Doc", 10L, true, null, "127.0.0.1");
		auditLogger.log(2L, "bob", "WRITE", "com.acme.Doc", 20L, false, "denied", null);

		List<AuditLog> all = auditLogger.queryLogs(null, null, null, null, null, 100);
		assertThat(all).hasSize(2);

		Instant end = Instant.now().plusSeconds(60);

		List<AuditLog> aliceLogs = auditLogger.queryLogs("alice", null, null, null, null, 100);
		assertThat(aliceLogs).hasSize(1);
		assertThat(aliceLogs.get(0).getUsername()).isEqualTo("alice");
		assertThat(aliceLogs.get(0).getUserId()).isEqualTo(1L);
		assertThat(aliceLogs.get(0).getResourceId()).isEqualTo(10L);
		assertThat(aliceLogs.get(0).getIpAddress()).isEqualTo("127.0.0.1");

		List<AuditLog> deniedWrites = auditLogger.queryLogs(null, "WRITE", null, null, Boolean.FALSE, 100);
		assertThat(deniedWrites).hasSize(1);
		assertThat(deniedWrites.get(0).getUsername()).isEqualTo("bob");
		assertThat(deniedWrites.get(0).isGranted()).isFalse();
		assertThat(deniedWrites.get(0).getReason()).isEqualTo("denied");

		List<AuditLog> window = auditLogger.queryLogs(null, null, start, end, null, 100);
		assertThat(window).hasSize(2);
		assertThat(window).extracting(AuditLog::getUsername).containsExactlyInAnyOrder("alice", "bob");
	}

	@Configuration
	static class TestConfig {

		@Bean
		public Driver falkorDBDriver() {
			return new DriverImpl("localhost", 6379);
		}

		@Bean
		public FalkorDBClient falkorDBClient(Driver driver) {
			return new DefaultFalkorDBClient(driver, "test_audit_integration");
		}

		@Bean
		public FalkorDBMappingContext falkorDBMappingContext() {
			return new DefaultFalkorDBMappingContext();
		}

		@Bean
		public FalkorDBTemplate falkorDBTemplate(FalkorDBClient client, FalkorDBMappingContext mappingContext) {
			EntityInstantiators instantiators = new EntityInstantiators();
			DefaultFalkorDBEntityConverter converter = new DefaultFalkorDBEntityConverter(mappingContext, instantiators,
					client);
			return new FalkorDBTemplate(client, mappingContext, converter);
		}

	}

}
