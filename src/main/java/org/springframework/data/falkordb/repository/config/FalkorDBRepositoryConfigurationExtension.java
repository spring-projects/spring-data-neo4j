/*
 * Copyright (c) 2023-2024 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.springframework.data.falkordb.repository.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
import org.springframework.data.falkordb.repository.FalkorDBRepository;
import org.springframework.data.falkordb.repository.support.FalkorDBRepositoryFactoryBean;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * {@link org.springframework.data.repository.config.RepositoryConfigurationExtension} for
 * FalkorDB.
 *
 * @author Shahar Biron
 * @since 1.0
 */
public class FalkorDBRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

	private static final String FALKORDB_TEMPLATE_REF = "falkorDB-template-ref";

	@Override
	public String getModuleName() {
		return "FalkorDB";
	}

	@Override
	public String getRepositoryFactoryBeanClassName() {
		return FalkorDBRepositoryFactoryBean.class.getName();
	}

	@Override
	protected String getModulePrefix() {
		return "falkordb";
	}

	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Collections.singleton(org.springframework.data.falkordb.core.schema.Node.class);
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.singleton(FalkorDBRepository.class);
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {

		source.getAttribute(FALKORDB_TEMPLATE_REF).ifPresent(s -> builder.addPropertyReference("falkorDBTemplate", s));
	}

	@Override
	protected boolean useRepositoryConfiguration(RepositoryMetadata metadata) {
		return metadata.isReactiveRepository() == false;
	}

	@Override
	protected String getDefaultNamedQueryLocation() {
		return "META-INF/falkordb-named-queries.properties";
	}

	@Override
	protected Class<?> getDefaultMappingContextClass() {
		return FalkorDBMappingContext.class;
	}

}
