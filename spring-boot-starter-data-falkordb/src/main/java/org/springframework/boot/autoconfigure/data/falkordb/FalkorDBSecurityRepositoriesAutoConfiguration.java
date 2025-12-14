package org.springframework.boot.autoconfigure.data.falkordb;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.repository.FalkorDBRepository;
import org.springframework.data.falkordb.repository.config.FalkorDBRepositoryConfigurationExtension;
import org.springframework.data.falkordb.security.repository.FalkorDBSecurityRepositoryFactoryBean;

/**
 * Auto-configuration for security-aware Spring Data FalkorDB repositories.
 * Uses {@link FalkorDBSecurityRepositoryFactoryBean} to create
 * {@link org.springframework.data.falkordb.security.repository.SecureFalkorDBRepository}
 * instances when RBAC security is enabled.
 */
@AutoConfiguration(after = FalkorDBAutoConfiguration.class)
@ConditionalOnClass({ FalkorDBTemplate.class, FalkorDBRepository.class })
@ConditionalOnBean(FalkorDBTemplate.class)
@ConditionalOnMissingBean({ FalkorDBSecurityRepositoryFactoryBean.class,
		FalkorDBRepositoryConfigurationExtension.class })
@ConditionalOnProperty(prefix = "spring.data.falkordb.repositories", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "spring.data.falkordb.security", name = "enabled",
		havingValue = "true")
@Import(FalkorDBSecurityRepositoriesRegistrar.class)
public class FalkorDBSecurityRepositoriesAutoConfiguration {
	// Configuration handled by the security registrar
}