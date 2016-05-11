package org.springframework.data.neo4j.transaction.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.neo4j.transaction.SessionFactoryUtils;
import org.springframework.util.Assert;

/**
 * Created by markangrish on 14/05/2016.
 */
public class SpringSessionProxyBean implements FactoryBean<Session>, InitializingBean, BeanFactoryAware {

	private SessionFactory sessionFactory;

	private Class<? extends Session> sessionInterface = Session.class;

	private boolean allowCreate = true;

	private Session proxy;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	protected SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	protected Class<? extends Session> getSessionInterface() {
		return this.sessionInterface;
	}

	public void setAllowCreate(boolean allowCreate) {
		this.allowCreate = allowCreate;
	}

	protected boolean isAllowCreate() {
		return this.allowCreate;
	}

	@Override
	public void afterPropertiesSet() {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("Property 'sessionFactory' is required");
		}

		this.proxy = (Session) Proxy.newProxyInstance(
				getSessionFactory().getClass().getClassLoader(),
				new Class<?>[]{getSessionInterface()}, new SessionInvocationHandler());
	}


	@Override
	public Session getObject() {
		return this.proxy;
	}

	@Override
	public Class<? extends Session> getObjectType() {
		return getSessionInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (getSessionFactory() == null) {
			sessionFactory = beanFactory.getBean(SessionFactory.class);
		}
	}

	private class SessionInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on PersistenceManager interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			} else if (method.getName().equals("hashCode")) {
				// Use hashCode of PersistenceManager proxy.
				return System.identityHashCode(proxy);
			} else if (method.getName().equals("toString")) {
				// Deliver toString without touching a target EntityManager.
				return "Spring Session proxy for target factory [" + getSessionFactory() + "]";
			} else if (method.getName().equals("getSessionFactory")) {
				// Return PersistenceManagerFactory without creating a PersistenceManager.
				return getSessionFactory();
			}

			// Invoke method on target PersistenceManager.
			Session session = SessionFactoryUtils.getSession(
					getSessionFactory(), isAllowCreate());
			try {
				return method.invoke(session, args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			} finally {
				SessionFactoryUtils.releaseSession(session, getSessionFactory());
			}
		}
	}
}
