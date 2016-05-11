package org.springframework.data.neo4j.transaction.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.data.neo4j.transaction.SessionFactoryUtils;
import org.springframework.data.neo4j.transaction.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

/**
 * Created by markangrish on 13/05/2016.
 */
public class OpenSessionInViewInterceptor implements WebRequestInterceptor, BeanFactoryAware {

	public static final String PARTICIPATE_SUFFIX = ".PARTICIPATE";

	protected final Log logger = LogFactory.getLog(getClass());

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (getSessionFactory() == null) {
			sessionFactory = beanFactory.getBean(SessionFactory.class);
		}
	}

	@Override
	public void preHandle(WebRequest webRequest) throws Exception {
		if (TransactionSynchronizationManager.hasResource(getSessionFactory())) {
			// Do not modify the Session: just mark the request accordingly.
			String participateAttributeName = getParticipateAttributeName();
			Integer count = (Integer) webRequest.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
			int newCount = (count != null ? count + 1 : 1);
			webRequest.setAttribute(getParticipateAttributeName(), newCount, WebRequest.SCOPE_REQUEST);
		} else {
			logger.debug("Opening Neo4j Session in OpenSessionInViewInterceptor");
			Session session = SessionFactoryUtils.getSession(getSessionFactory(), true);
			TransactionSynchronizationManager.bindResource(
					getSessionFactory(), new SessionHolder(session));
		}
	}

	@Override
	public void postHandle(WebRequest webRequest, ModelMap modelMap) throws Exception {

	}

	@Override
	public void afterCompletion(WebRequest webRequest, Exception e) throws Exception {
		String participateAttributeName = getParticipateAttributeName();
		Integer count = (Integer) webRequest.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		if (count != null) {
			// Do not modify the Session: just clear the marker.
			if (count > 1) {
				webRequest.setAttribute(participateAttributeName, count - 1, WebRequest.SCOPE_REQUEST);
			} else {
				webRequest.removeAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
			}
		} else {
			SessionHolder pmHolder = (SessionHolder)
					TransactionSynchronizationManager.unbindResource(getSessionFactory());
			logger.debug("Closing Noe4j Session in OpenSessionInViewInterceptor");
			SessionFactoryUtils.releaseSession(
					pmHolder.getSession(), getSessionFactory());
		}
	}

	protected String getParticipateAttributeName() {
		return getSessionFactory().toString() + PARTICIPATE_SUFFIX;
	}
}
