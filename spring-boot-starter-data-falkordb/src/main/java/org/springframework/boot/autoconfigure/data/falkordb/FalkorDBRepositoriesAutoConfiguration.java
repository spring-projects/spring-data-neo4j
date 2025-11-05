package org.springframework.boot.autoconfigure.data.falkordb;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.repository.FalkorDBRepository;
import org.springframework.data.falkordb.repository.config.EnableFalkorDBRepositories;
import org.springframework.data.falkordb.repository.config.FalkorDBRepositoryConfigurationExtension;
import org.springframework.data.falkordb.repository.support.FalkorDBRepositoryFactoryBean;

/**
 * Auto-configuration for Spring Data FalkorDB Repositories.
 * Only activates if FalkorDBTemplate bean is available.
 *
 * @author Shahar Biron
 * @since 1.0
 */
@AutoConfiguration(after = FalkorDBAutoConfiguration.class)
@ConditionalOnClass({FalkorDBTemplate.class, FalkorDBRepository.class})
@ConditionalOnBean(FalkorDBTemplate.class)
@ConditionalOnMissingBean({FalkorDBRepositoryFactoryBean.class,
		FalkorDBRepositoryConfigurationExtension.class})
@ConditionalOnProperty(prefix = "spring.data.falkordb.repositories", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@Import(FalkorDBRepositoriesRegistrar.class)
public class FalkorDBRepositoriesAutoConfiguration {
	// Configuration handled by the registrar
}
