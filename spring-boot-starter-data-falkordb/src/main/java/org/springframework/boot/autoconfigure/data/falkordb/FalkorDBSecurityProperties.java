package org.springframework.boot.autoconfigure.data.falkordb;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for FalkorDB RBAC / security integration.
 */
@ConfigurationProperties(prefix = "spring.data.falkordb.security")
public class FalkorDBSecurityProperties {

	/**
	 * Whether RBAC security is enabled.
	 */
	private boolean enabled = false;

	/**
	 * Name of the administrative role.
	 */
	private String adminRole = "admin";

	/**
	 * Default role included for all users (parity with Python RBAC 'PUBLIC' role).
	 */
	private String defaultRole = org.springframework.data.falkordb.security.context.FalkorSecurityContext.DEFAULT_DEFAULT_ROLE;

	/**
	 * Whether audit logging is enabled.
	 */
	private boolean auditEnabled = true;

	/**
	 * Whether the core template should attempt query-time row-level security enforcement by
	 * rewriting generated Cypher queries.
	 *
	 * Disabled by default (opt-in).
	 */
	private boolean queryRewriteEnabled = false;

	/**
	 * Time-to-live for cached privileges per user.
	 */
	private Duration privilegeCacheTtl = Duration.ofMinutes(1);

	/**
	 * Bootstrap settings for creating initial admin role/user.
	 */
	private final Bootstrap bootstrap = new Bootstrap();

	public Bootstrap getBootstrap() {
		return this.bootstrap;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getAdminRole() {
		return this.adminRole;
	}

	public void setAdminRole(String adminRole) {
		this.adminRole = adminRole;
	}

	public String getDefaultRole() {
		return this.defaultRole;
	}

	public void setDefaultRole(String defaultRole) {
		this.defaultRole = defaultRole;
	}

	public boolean isAuditEnabled() {
		return this.auditEnabled;
	}

	public void setAuditEnabled(boolean auditEnabled) {
		this.auditEnabled = auditEnabled;
	}

	public boolean isQueryRewriteEnabled() {
		return this.queryRewriteEnabled;
	}

	public void setQueryRewriteEnabled(boolean queryRewriteEnabled) {
		this.queryRewriteEnabled = queryRewriteEnabled;
	}

	public Duration getPrivilegeCacheTtl() {
		return this.privilegeCacheTtl;
	}

	public void setPrivilegeCacheTtl(Duration privilegeCacheTtl) {
		this.privilegeCacheTtl = privilegeCacheTtl;
	}

	public static class Bootstrap {

		/**
		 * Enable bootstrap logic.
		 */
		private boolean enabled = false;

		/**
		 * Create (or ensure) an admin user at startup.
		 */
		private String adminUsername;

		/**
		 * Optional email for the bootstrapped admin user.
		 */
		private String adminEmail;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getAdminUsername() {
			return this.adminUsername;
		}

		public void setAdminUsername(String adminUsername) {
			this.adminUsername = adminUsername;
		}

		public String getAdminEmail() {
			return this.adminEmail;
		}

		public void setAdminEmail(String adminEmail) {
			this.adminEmail = adminEmail;
		}
	}

}
