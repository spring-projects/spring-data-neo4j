package org.springframework.data.neo4j.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.neo4j.template.Neo4jOgmExceptionTranslator;

/**
 * Created by markangrish on 14/05/2016.
 */
public class LocalSessionFactoryBean implements FactoryBean<SessionFactory>,
		InitializingBean, DisposableBean, PersistenceExceptionTranslator {

	protected final Log logger = LogFactory.getLog(LocalSessionFactoryBean.class);

	private SessionFactory sessionFactory;
	private String[] packagesToScan;

	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	@Override
	public void destroy() throws Exception {
	}

	@Override
	public SessionFactory getObject() throws Exception {
		return sessionFactory;
	}

	@Override
	public Class<? extends SessionFactory> getObjectType() {
		return sessionFactory.getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		logger.info("Building new Neo4j SessionFactory");
		this.sessionFactory = new SessionFactory(packagesToScan);
	}

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return Neo4jOgmExceptionTranslator.translateExceptionIfPossible(ex);
	}
}
