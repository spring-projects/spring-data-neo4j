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

import java.util.concurrent.Callable;

import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.transaction.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptorAdapter;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;

/**
 * An interceptor with asynchronous web requests used in OpenSessionInViewFilter and OpenSessionInViewInterceptor.
 * Ensures the following: 1) The session is bound/unbound when "callable processing" is started 2) The session is closed
 * if an async request times out
 *
 * @author Mark Angrish
 */
class AsyncRequestInterceptor extends CallableProcessingInterceptorAdapter
		implements DeferredResultProcessingInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(AsyncRequestInterceptor.class);

	private final SessionFactory sessionFactory;

	private final SessionHolder sessionHolder;

	private volatile boolean timeoutInProgress;

	public AsyncRequestInterceptor(SessionFactory sessionFactory, SessionHolder sessionHolder) {
		this.sessionFactory = sessionFactory;
		this.sessionHolder = sessionHolder;
	}

	@Override
	public <T> void preProcess(NativeWebRequest request, Callable<T> task) {
		bindSession();
	}

	public void bindSession() {
		this.timeoutInProgress = false;
		TransactionSynchronizationManager.bindResource(this.sessionFactory, this.sessionHolder);
	}

	@Override
	public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) {
		TransactionSynchronizationManager.unbindResource(this.sessionFactory);
	}

	@Override
	public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) {
		this.timeoutInProgress = true;
		return RESULT_NONE; // give other interceptors a chance to handle the timeout
	}

	@Override
	public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
		closeAfterTimeout();
	}

	private void closeAfterTimeout() {
		if (this.timeoutInProgress) {
			logger.debug("Closing Neo4j OGM Session after async request timeout");
			// close session.
			// SessionFactoryUtils.closeSession(session);
		}
	}

	@Override
	public <T> void beforeConcurrentHandling(NativeWebRequest request, DeferredResult<T> deferredResult) {}

	@Override
	public <T> void preProcess(NativeWebRequest request, DeferredResult<T> deferredResult) {
		// Do nothing.
	}

	@Override
	public <T> void postProcess(NativeWebRequest request, DeferredResult<T> deferredResult, Object result) {
		// Do nothing.
	}

	@Override
	public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult) {
		this.timeoutInProgress = true;
		return true; // give other interceptors a chance to handle the timeout
	}

	@Override
	public <T> void afterCompletion(NativeWebRequest nativeWebRequest, DeferredResult<T> deferredResult)
			throws Exception {
		// Do nothing.
	}
}
