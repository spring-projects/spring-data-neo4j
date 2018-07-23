/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.web.support;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.transaction.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet Filter that binds a Neo4j OGM Session to the thread for the entire processing of the request. Intended for
 * the "Open Session in View" pattern, i.e. to allow for lazy loading in web views despite the original transactions
 * already being completed.
 * <p>
 * This filter makes OGM Sessions available via the current thread, which will be autodetected by transaction managers.
 * At the moment this service is only suitable for transactions via {@link Neo4jTransactionManager}.
 * <p>
 * Looks up the SessionFactory in Spring's root web application context. Supports an "sessionFactoryBeanName" filter
 * init-param in {@code web.xml}; the default bean name is "sessionFactory".
 *
 * @author Mark Angrish
 * @see OpenSessionInViewInterceptor
 * @see Neo4jTransactionManager
 * @see TransactionSynchronizationManager
 */
public class OpenSessionInViewFilter extends OncePerRequestFilter {

	/**
	 * Default SessionFactory bean name: "sessionFactory".
	 *
	 * @see #setSessionFactoryBeanName
	 */
	public static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactory";

	private String sessionFactoryBeanName = DEFAULT_SESSION_FACTORY_BEAN_NAME;

	private volatile SessionFactory sessionFactory;

	/**
	 * Set the bean name of the SessionFactory to fetch from Spring's root application context.
	 * <p>
	 * Default is "sessionFactory".
	 *
	 * @see #DEFAULT_SESSION_FACTORY_BEAN_NAME
	 */
	public void setSessionFactoryBeanName(String sessionFactoryBeanName) {
		this.sessionFactoryBeanName = sessionFactoryBeanName;
	}

	/**
	 * Return the bean name of the SessionFactory to fetch from Spring's root application context.
	 */
	protected String getSessionFactoryBeanName() {
		return this.sessionFactoryBeanName;
	}

	/**
	 * Returns "false" so that the filter may re-bind the opened {@code Session} to each asynchronously dispatched thread
	 * and postpone closing it until the very last asynchronous dispatch.
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * Returns "false" so that the filter may provide an {@code Session} to each error dispatches.
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		SessionFactory sessionFactory = lookupSessionFactory(request);
		boolean participate = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		String key = getAlreadyFilteredAttributeName();

		if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
			// Do not modify the Session: just set the participate flag.
			participate = true;
		} else {
			boolean isFirstRequest = !isAsyncDispatch(request);
			if (isFirstRequest || !applySessionBindingInterceptor(asyncManager, key)) {
				logger.debug("Opening Neo4j OGM Session in OpenSessionInViewFilter");
				Session session = createSession(sessionFactory);
				SessionHolder sessionHolder = new SessionHolder(session);
				TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);

				AsyncRequestInterceptor interceptor = new AsyncRequestInterceptor(sessionFactory, sessionHolder);
				asyncManager.registerCallableInterceptor(key, interceptor);
				asyncManager.registerDeferredResultInterceptor(key, interceptor);
			}
		}

		try {
			filterChain.doFilter(request, response);
		} finally {
			if (!participate) {
				TransactionSynchronizationManager.unbindResource(sessionFactory);
				if (!isAsyncStarted(request)) {
					logger.debug("Closed Neo4J OGM Session in OpenSessionInViewFilter");
					// close session.
					// SessionFactoryUtils.closeSession(session);
				}
			}
		}
	}

	/**
	 * Look up the SessionFactory that this filter should use, taking the current HTTP request as argument.
	 * <p>
	 * The default implementation delegates to the {@code lookupSessionFactory} without arguments, caching the
	 * SessionFactory reference once obtained.
	 *
	 * @return the SessionFactory to use
	 * @see #lookupSessionFactory()
	 */
	protected SessionFactory lookupSessionFactory(HttpServletRequest request) {
		if (this.sessionFactory == null) {
			this.sessionFactory = lookupSessionFactory();
		}
		return this.sessionFactory;
	}

	/**
	 * Look up the SessionFactory that this filter should use.
	 * <p>
	 * The default implementation looks for a bean with the specified name in Spring's root application context.
	 *
	 * @return the SessionFactory to use
	 * @see #getSessionFactoryBeanName
	 */
	protected SessionFactory lookupSessionFactory() {
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		String sessionFactoryBeanName = getSessionFactoryBeanName();
		if (StringUtils.hasLength(sessionFactoryBeanName)) {
			return wac.getBean(sessionFactoryBeanName, SessionFactory.class);
		} else if (wac.containsBean(DEFAULT_SESSION_FACTORY_BEAN_NAME)) {
			return wac.getBean(DEFAULT_SESSION_FACTORY_BEAN_NAME, SessionFactory.class);
		} else {
			// Includes fallback search for single SessionFactory bean by type.
			return wac.getBean(SessionFactory.class);
		}
	}

	/**
	 * Create a Neo4j OGM Session to be bound to a request.
	 * <p>
	 * Can be overridden in subclasses.
	 *
	 * @param sessionFactory the SessionFactory to use
	 * @see SessionFactory#openSession
	 */
	protected Session createSession(SessionFactory sessionFactory) {
		return sessionFactory.openSession();
	}

	private boolean applySessionBindingInterceptor(WebAsyncManager asyncManager, String key) {
		if (asyncManager.getCallableInterceptor(key) == null) {
			return false;
		}
		((AsyncRequestInterceptor) asyncManager.getCallableInterceptor(key)).bindSession();
		return true;
	}
}
