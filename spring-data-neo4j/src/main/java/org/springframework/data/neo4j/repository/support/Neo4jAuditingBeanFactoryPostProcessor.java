package org.springframework.data.neo4j.repository.support;

import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.event.EventListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.neo4j.repository.config.Neo4jAuditingEventListener;

public class Neo4jAuditingBeanFactoryPostProcessor implements BeanPostProcessor {

	private final SessionFactory sessionFactory;

	@Autowired
	public Neo4jAuditingBeanFactoryPostProcessor(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Neo4jAuditingEventListener) {
			EventListener auditingEventListener = (EventListener) bean;

			// we need to de-register first to be sure we just register once.
			// Background: SpringBoot auto configuration does register all event listener in the context.
			sessionFactory.deregister(auditingEventListener);
			sessionFactory.register(auditingEventListener);
		}
		return bean;
	}
}
