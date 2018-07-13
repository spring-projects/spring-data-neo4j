/*
 * Copyright (c) 2018 "Neo4j, Inc." / "Pivotal Software, Inc."
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.config;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;

/**
 * @author Gerrit Meier
 */
public class Neo4jOgmEntityInstantiatorConfigurationBean {

	public Neo4jOgmEntityInstantiatorConfigurationBean(SessionFactory sessionFactory, Neo4jMappingContext mappingContext,
			ObjectProvider<ConversionService> conversionServiceObjectProvider) {

		ConversionService conversionService = conversionServiceObjectProvider.getIfAvailable();

		sessionFactory.setEntityInstantiator(
				new OgmEntityInstantiatorAdapter(mappingContext, conversionService != null ? conversionService
						: new MetaDataDrivenConversionService(sessionFactory.metaData())));
	}

}
