package org.springframework.boot.autoconfigure.data.falkordb;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.security.audit.AuditLogger;
import org.springframework.data.falkordb.security.bootstrap.FalkorDBSecurityBootstrapRunner;
import org.springframework.data.falkordb.security.integration.AuthenticationFalkorSecurityContextAdapter;
import org.springframework.data.falkordb.security.integration.FalkorDBSecurityContextFilter;
import org.springframework.data.falkordb.security.integration.PrivilegeService;
import org.springframework.data.falkordb.security.manager.RBACManager;
import org.springframework.data.falkordb.security.rls.RowLevelSecurityQueryRewriter;
import org.springframework.data.falkordb.core.query.FalkorDBQueryRewriter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Auto-configuration for FalkorDB RBAC/Security integration with Spring Security.
 *
 * Provides beans to bridge Spring Security's {@link Authentication} to
 * {@link org.springframework.data.falkordb.security.context.FalkorSecurityContext}
 * and a {@link OncePerRequestFilter} that populates the Falkor context per request.
 *
 * Note: the filter is provided as a bean but is not automatically wired into the
 * Spring Security filter chain. Applications should register it explicitly in
 * their {@code SecurityFilterChain} configuration.
 */
@AutoConfiguration(after = FalkorDBAutoConfiguration.class)
@ConditionalOnClass({ SecurityContextHolder.class, Authentication.class, OncePerRequestFilter.class })
@ConditionalOnBean(FalkorDBTemplate.class)
@ConditionalOnProperty(prefix = "spring.data.falkordb.security", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FalkorDBSecurityProperties.class)
public class FalkorDBSecurityConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PrivilegeService falkorDBPrivilegeService(FalkorDBTemplate template, FalkorDBSecurityProperties properties) {
		return new PrivilegeService(template, properties.getPrivilegeCacheTtl());
	}

	@Bean
	@ConditionalOnMissingBean
	public AuthenticationFalkorSecurityContextAdapter falkorDBAuthenticationAdapter(FalkorDBTemplate template,
			PrivilegeService privilegeService, FalkorDBSecurityProperties properties) {
		return new AuthenticationFalkorSecurityContextAdapter(template, privilegeService, properties.getDefaultRole());
	}

	@Bean
	@ConditionalOnMissingBean
	public AuditLogger falkorDBAuditLogger(FalkorDBTemplate template, FalkorDBSecurityProperties properties) {
		return new AuditLogger(template, properties.isAuditEnabled());
	}

	@Bean
	@ConditionalOnMissingBean
	public RBACManager falkorDBRbacManager(FalkorDBTemplate template, FalkorDBSecurityProperties properties) {
		return new RBACManager(template, properties.getAdminRole());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.data.falkordb.security", name = "query-rewrite-enabled", havingValue = "true")
	public FalkorDBQueryRewriter falkorDBQueryRewriter() {
		return new RowLevelSecurityQueryRewriter();
	}

	@Bean
	@ConditionalOnMissingBean
	public FalkorDBSecurityContextFilter falkorDBSecurityContextFilter(
			AuthenticationFalkorSecurityContextAdapter adapter) {
		return new FalkorDBSecurityContextFilter(adapter);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.data.falkordb.security.bootstrap", name = "enabled", havingValue = "true")
	public FalkorDBSecurityBootstrapRunner falkorDBSecurityBootstrapRunner(FalkorDBTemplate template,
			FalkorDBSecurityProperties properties) {
		return new FalkorDBSecurityBootstrapRunner(template, properties);
	}

}
