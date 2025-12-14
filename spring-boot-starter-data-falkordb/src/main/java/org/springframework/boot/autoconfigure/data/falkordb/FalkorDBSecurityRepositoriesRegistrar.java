package org.springframework.boot.autoconfigure.data.falkordb;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.falkordb.repository.config.EnableFalkorDBRepositories;
import org.springframework.data.falkordb.repository.config.FalkorDBRepositoryConfigurationExtension;
import org.springframework.data.falkordb.security.repository.FalkorDBSecurityRepositoryFactoryBean;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * Registrar variant that enables FalkorDB repositories using the
 * security-aware {@link FalkorDBSecurityRepositoryFactoryBean}.
 */
class FalkorDBSecurityRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableFalkorDBRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableFalkorDBSecurityRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new FalkorDBRepositoryConfigurationExtension();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableFalkorDBRepositories(repositoryFactoryBeanClass = FalkorDBSecurityRepositoryFactoryBean.class)
	static class EnableFalkorDBSecurityRepositoriesConfiguration {
	}

}