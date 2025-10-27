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
package org.springframework.data.falkordb.repository.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.FactoryBean} to create
 * {@link org.springframework.data.falkordb.repository.FalkorDBRepository} instances.
 *
 * @param <T> repository type
 * @param <S> entity type
 * @param <ID> entity identifier type
 * @author Shahar Biron
 * @since 1.0
 */
public class FalkorDBRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
		extends RepositoryFactoryBeanSupport<T, S, ID> {

	private FalkorDBTemplate falkorDBTemplate;

	/**
	 * Creates a new {@link FalkorDBRepositoryFactoryBean} for the given repository
	 * interface.
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public FalkorDBRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	/**
	 * Configures the {@link FalkorDBTemplate} to be used.
	 * @param falkorDBTemplate the FalkorDB template
	 */
	@Autowired
	public void setFalkorDBTemplate(FalkorDBTemplate falkorDBTemplate) {
		this.falkorDBTemplate = falkorDBTemplate;
		setMappingContext(falkorDBTemplate.getMappingContext());
	}

	@Override
	public void setMappingContext(MappingContext<?, ?> mappingContext) {
		super.setMappingContext(mappingContext);
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		Assert.state(falkorDBTemplate != null, "FalkorDBTemplate must not be null");
		return new FalkorDBRepositoryFactory(falkorDBTemplate);
	}

	@Override
	public void afterPropertiesSet() {
		Assert.state(falkorDBTemplate != null, "FalkorDBTemplate must not be null");
		super.afterPropertiesSet();
	}

}
