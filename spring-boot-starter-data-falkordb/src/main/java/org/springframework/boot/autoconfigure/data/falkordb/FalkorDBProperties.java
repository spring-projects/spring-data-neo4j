package org.springframework.boot.autoconfigure.data.falkordb;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for FalkorDB.
 *
 * @author Shahar Biron
 * @since 1.0
 */
@ConfigurationProperties(prefix = "spring.data.falkordb")
public class FalkorDBProperties {

	/**
	 * FalkorDB server URI. Default is falkordb://localhost:6379
	 */
	private String uri = "falkordb://localhost:6379";

	/**
	 * FalkorDB database name.
	 */
	private String database;

	/**
	 * Connection timeout in milliseconds.
	 */
	private int connectionTimeout = 2000;

	/**
	 * Socket timeout in milliseconds.
	 */
	private int socketTimeout = 2000;

	/**
	 * Username for authentication (if required).
	 */
	private String username;

	/**
	 * Password for authentication (if required).
	 */
	private String password;

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

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
