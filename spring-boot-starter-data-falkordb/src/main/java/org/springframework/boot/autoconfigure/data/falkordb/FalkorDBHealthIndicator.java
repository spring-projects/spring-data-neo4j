package org.springframework.boot.autoconfigure.data.falkordb;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.falkordb.core.FalkorDBClient;

/**
 * Health indicator for FalkorDB.
 *
 * @author Shahar Biron
 * @since 1.0
 */
public class FalkorDBHealthIndicator implements HealthIndicator {

	private final FalkorDBClient falkorDBClient;

	public FalkorDBHealthIndicator(FalkorDBClient falkorDBClient) {
		this.falkorDBClient = falkorDBClient;
	}

	@Override
	public Health health() {
		try {
			// Execute a simple query to check connectivity
			falkorDBClient.query("RETURN 1", java.util.Collections.emptyMap(), result -> {
				for (FalkorDBClient.Record record : result.records()) {
					return record.get("1");
				}
				return null;
			});
			
			return Health.up()
				.withDetail("database", "connected")
				.build();
		}
		catch (Exception ex) {
			return Health.down()
				.withException(ex)
				.build();
		}
	}
}
