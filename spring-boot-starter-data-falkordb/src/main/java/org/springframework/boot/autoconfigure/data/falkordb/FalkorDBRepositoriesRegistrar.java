package org.springframework.boot.autoconfigure.data.falkordb;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.data.falkordb.repository.config.EnableFalkorDBRepositories;
import org.springframework.data.falkordb.repository.config.FalkorDBRepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.lang.annotation.Annotation;

/**
 * Registrar for FalkorDB repositories.
 *
 * @author Shahar Biron
 * @since 1.0
 */
class FalkorDBRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableFalkorDBRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableFalkorDBRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new FalkorDBRepositoryConfigurationExtension();
	}

	@EnableFalkorDBRepositories
	private static final class EnableFalkorDBRepositoriesConfiguration {
	}
}
