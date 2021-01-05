/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.config;

import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.typeconversion.ConversionCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.conversion.Neo4jOgmEntityInstantiatorAdapter;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;

/**
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class Neo4jOgmEntityInstantiatorConfigurationBean {

	public Neo4jOgmEntityInstantiatorConfigurationBean(SessionFactory sessionFactory, Neo4jMappingContext mappingContext,
			ObjectProvider<ConversionService> conversionServiceObjectProvider) {

		MetaData metaData = sessionFactory.metaData();
		ConversionService conversionService = conversionServiceObjectProvider
				.getIfUnique(() -> new MetaDataDrivenConversionService(metaData));
		metaData.registerConversionCallback(new ConversionServiceBasedConversionCallback(conversionService));

		sessionFactory.setEntityInstantiator(new Neo4jOgmEntityInstantiatorAdapter(mappingContext, conversionService));
	}

	private static class ConversionServiceBasedConversionCallback implements ConversionCallback {

		private final ConversionService delegate;

		public ConversionServiceBasedConversionCallback(ConversionService conversionService) {
			this.delegate = conversionService;
		}

		@Override
		public <T> T convert(Class<T> targetType, Object value) {
			if (value == null) {
				return null;
			}
			return delegate.convert(value, targetType);
		}
	}
}
