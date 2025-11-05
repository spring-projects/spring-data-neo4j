package org.springframework.boot.autoconfigure.data.falkordb;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.falkordb.core.FalkorDBClient;

/**
 * Auto-configuration for FalkorDB health indicator.
 *
 * @author Shahar Biron
 * @since 1.0
 */
@AutoConfiguration(after = FalkorDBAutoConfiguration.class)
@ConditionalOnClass({FalkorDBClient.class, HealthIndicator.class})
public class FalkorDBHealthContributorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "falkorDBHealthIndicator")
	public FalkorDBHealthIndicator falkorDBHealthIndicator(FalkorDBClient falkorDBClient,
			FalkorDBProperties properties) {
		return new FalkorDBHealthIndicator(falkorDBClient, properties);
	}
}
