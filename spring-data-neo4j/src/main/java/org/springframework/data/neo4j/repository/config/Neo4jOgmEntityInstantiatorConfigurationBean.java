package org.springframework.data.neo4j.repository.config;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;

public class Neo4jOgmEntityInstantiatorConfigurationBean {

	public Neo4jOgmEntityInstantiatorConfigurationBean(SessionFactory sessionFactory, Neo4jMappingContext mappingContext,
			ObjectProvider<ConversionService> conversionServiceObjectProvider) {

		ConversionService conversionService = conversionServiceObjectProvider.getIfAvailable();

		sessionFactory.setEntityInstantiator(
				new OgmEntityInstantiatorAdapter(mappingContext, conversionService != null ? conversionService
						: new MetaDataDrivenConversionService(sessionFactory.metaData())));
	}

}
