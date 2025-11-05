package org.springframework.boot.autoconfigure.data.falkordb;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for FalkorDB.
 *
 * @author Shahar Biron
 * @since 1.0
 */
@ConfigurationProperties(prefix = "spring.data.falkordb")
@Validated
public class FalkorDBProperties {

	/**
	 * FalkorDB server URI. Default is falkordb://localhost:6379
	 */
	private String uri = "falkordb://localhost:6379";

	/**
	 * FalkorDB database name (required).
	 */
	@NotBlank(message = "spring.data.falkordb.database must be configured")
	private String database;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}
}
	public void setDatabase(String database) {
		this.database = database;
	}
}
