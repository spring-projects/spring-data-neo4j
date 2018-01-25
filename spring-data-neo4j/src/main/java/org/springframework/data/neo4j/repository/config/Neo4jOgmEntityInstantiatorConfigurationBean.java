package org.springframework.data.neo4j.repository.config;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;

public class Neo4jOgmEntityInstantiatorConfigurationBean implements InitializingBean {

	private final SessionFactory sessionFactory;
	private final Neo4jMappingContext mappingContext;
	private ConversionService conversionService;

	@Autowired
	public Neo4jOgmEntityInstantiatorConfigurationBean(SessionFactory sessionFactory, Neo4jMappingContext mappingContext) {
		this.sessionFactory = sessionFactory;
		this.mappingContext = mappingContext;
	}

	@Override
	public void afterPropertiesSet() {
		sessionFactory.setEntityInstantiator(
				new OgmEntityInstantiatorAdapter(
						mappingContext,
						conversionService != null ? conversionService : new MetaDataDrivenConversionService(sessionFactory.metaData()))
		);
	}

	@Autowired(required = false)
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}
}
