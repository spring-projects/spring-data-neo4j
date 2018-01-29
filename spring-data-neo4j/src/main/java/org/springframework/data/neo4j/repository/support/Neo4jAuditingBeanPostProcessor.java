package org.springframework.data.neo4j.repository.support;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.neo4j.repository.config.Neo4jAuditingEventListener;
import org.springframework.data.neo4j.repository.config.Neo4jIsNewAwareAuditingHandler;

public class Neo4jAuditingBeanPostProcessor implements BeanPostProcessor {

	private final ObjectFactory<Neo4jIsNewAwareAuditingHandler> isNewAwareHandler;

	public Neo4jAuditingBeanPostProcessor(ObjectFactory<Neo4jIsNewAwareAuditingHandler> isNewAwareHandler) {
		this.isNewAwareHandler = isNewAwareHandler;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof SessionFactory) {
			SessionFactory sessionFactory = (SessionFactory) bean;
			sessionFactory.register(new Neo4jAuditingEventListener(isNewAwareHandler));
		}
		return bean;
	}
}
