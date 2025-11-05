package org.springframework.boot.autoconfigure.data.falkordb;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.falkordb.core.FalkorDBClient;

/**
 * Health indicator for FalkorDB.
 * Performs a connectivity check and reports database details.
 *
 * @author Shahar Biron
 * @since 1.0
 */
public class FalkorDBHealthIndicator implements HealthIndicator {

	private final FalkorDBClient falkorDBClient;
	private final String databaseName;

	public FalkorDBHealthIndicator(FalkorDBClient falkorDBClient, FalkorDBProperties properties) {
		this.falkorDBClient = falkorDBClient;
		this.databaseName = properties.getDatabase();
	}

	@Override
	public Health health() {
		try {
			// Execute a simple query to check connectivity
			Object result = falkorDBClient.query("RETURN 1", java.util.Collections.emptyMap(), queryResult -> {
				for (FalkorDBClient.Record record : queryResult.records()) {
					return record.get("1");
				}
				return null;
			});
			
			return Health.up()
				.withDetail("database", databaseName)
				.withDetail("status", "connected")
				.withDetail("validationQuery", "RETURN 1")
				.build();
		}
		catch (Exception ex) {
			return Health.down()
				.withException(ex)
				.build();
		}
	}
}
