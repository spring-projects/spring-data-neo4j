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

import java.util.Optional;

import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
import org.springframework.data.falkordb.core.mapping.FalkorDBPersistentEntity;
import org.springframework.data.falkordb.core.mapping.FalkorDBPersistentProperty;
import org.springframework.data.falkordb.repository.query.DerivedFalkorDBQuery;
import org.springframework.data.falkordb.repository.query.FalkorDBQueryMethod;
import org.springframework.data.falkordb.repository.query.StringBasedFalkorDBQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * Factory to create
 * {@link org.springframework.data.falkordb.repository.FalkorDBRepository} instances.
 *
 * @author Shahar Biron
 * @since 1.0
 */
public class FalkorDBRepositoryFactory extends RepositoryFactorySupport {

	private final FalkorDBTemplate falkorDBTemplate;

	private final FalkorDBMappingContext mappingContext;

	/**
	 * Creates a new {@link FalkorDBRepositoryFactory} with the given
	 * {@link FalkorDBTemplate}.
	 * @param falkorDBTemplate must not be {@literal null}
	 */
	public FalkorDBRepositoryFactory(FalkorDBTemplate falkorDBTemplate) {

		Assert.notNull(falkorDBTemplate, "FalkorDBTemplate must not be null");

		this.falkorDBTemplate = falkorDBTemplate;
		this.mappingContext = falkorDBTemplate.getMappingContext();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, ID> FalkorDBEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {

		FalkorDBPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(domainClass);

		return new FalkorDBEntityInformationImpl<>((FalkorDBPersistentEntity<T>) entity);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation information) {
		FalkorDBEntityInformation<?, Object> entityInformation = getEntityInformation(information.getDomainType());
		return getTargetRepositoryViaReflection(information, falkorDBTemplate, entityInformation);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleFalkorDBRepository.class;
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
			ValueExpressionDelegate valueExpressionDelegate) {

		return Optional.of(new FalkorDBQueryLookupStrategy());
	}

	/**
	 * Query lookup strategy for FalkorDB repositories.
	 */
	private class FalkorDBQueryLookupStrategy implements QueryLookupStrategy {

		@Override
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {

			FalkorDBQueryMethod queryMethod = new FalkorDBQueryMethod(method, metadata, factory,
					mappingContext);

			// Check if the method has an annotated query
			if (queryMethod.hasAnnotatedQuery()) {
				return new StringBasedFalkorDBQuery(queryMethod, falkorDBTemplate);
			}

			// For derived queries, use DerivedFalkorDBQuery
			return new DerivedFalkorDBQuery(queryMethod, falkorDBTemplate);
		}

	}

	/**
	 * Simple implementation of {@link FalkorDBEntityInformation}.
	 *
	 * @param <T> entity type
	 * @param <ID> identifier type
	 */
	private static class FalkorDBEntityInformationImpl<T, ID> implements FalkorDBEntityInformation<T, ID> {

		private final FalkorDBPersistentEntity<T> entity;

		FalkorDBEntityInformationImpl(FalkorDBPersistentEntity<T> entity) {
			this.entity = entity;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ID getId(T t) {
			if (t == null) {
				return null;
			}
			return (ID) entity.getIdentifierAccessor(t).getIdentifier();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Class<ID> getIdType() {
			return (Class<ID>) entity.getRequiredIdProperty().getType();
		}

		@Override
		public Class<T> getJavaType() {
			return entity.getType();
		}

		@Override
		public String getPrimaryLabel() {
			return entity.getPrimaryLabel();
		}

		@Override
		public String[] getLabels() {
			return entity.getLabels();
		}

		@Override
		public boolean isNew(T t) {
			return getId(t) == null;
		}

	}

}
