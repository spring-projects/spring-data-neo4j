package org.springframework.data.neo4j.web.support;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.PassThroughFilterChain;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * @author Mark Angrish
 */
public class OpenSessionInViewTests {

	private Session session;

	private SessionFactory sessionFactory;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private ServletWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		sessionFactory = mock(SessionFactory.class);
		session = mock(Session.class);

		given(sessionFactory.openSession()).willReturn(session);

		this.request = new MockHttpServletRequest();
		this.request.setAsyncSupported(true);
		this.response = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(this.request);
	}

	@After
	public void tearDown() throws Exception {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	@Test
	public void testOpenSessionInViewInterceptor() throws Exception {
		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(this.sessionFactory);

		MockServletContext sc = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(sc);

		interceptor.preHandle(new ServletWebRequest(request));
		assertTrue(TransactionSynchronizationManager.hasResource(this.sessionFactory));

		// check that further invocations simply participate
		interceptor.preHandle(new ServletWebRequest(request));

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.postHandle(new ServletWebRequest(request), null);
		assertTrue(TransactionSynchronizationManager.hasResource(sessionFactory));

		interceptor.afterCompletion(new ServletWebRequest(request), null);
		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactory));
	}

	@Test
	public void testOpenSessionInViewInterceptorAsyncScenario() throws Exception {

		// Initial request thread

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sessionFactory);

		given(sessionFactory.openSession()).willReturn(this.session);

		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(sessionFactory));

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.webRequest);
		asyncManager.setTaskExecutor(new SyncTaskExecutor());
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "anything";
			}
		});

		interceptor.afterConcurrentHandlingStarted(this.webRequest);
		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactory));

		// Async dispatch thread

		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(sessionFactory));

		asyncManager.clearConcurrentResult();

		// check that further invocations simply participate
		interceptor.preHandle(new ServletWebRequest(request));

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.postHandle(this.webRequest, null);
		assertTrue(TransactionSynchronizationManager.hasResource(sessionFactory));

		interceptor.afterCompletion(this.webRequest, null);
		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactory));
	}

	@Test
	public void testOpenSessionInViewInterceptorAsyncTimeoutScenario() throws Exception {

		// Initial request thread

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sessionFactory);

		given(this.sessionFactory.openSession()).willReturn(this.session);

		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(this.sessionFactory));

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setTaskExecutor(new SyncTaskExecutor());
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "anything";
			}
		});

		interceptor.afterConcurrentHandlingStarted(this.webRequest);
		assertFalse(TransactionSynchronizationManager.hasResource(this.sessionFactory));

		// Async request timeout

		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();
		for (AsyncListener listener : asyncContext.getListeners()) {
			listener.onTimeout(new AsyncEvent(asyncContext));
		}
		for (AsyncListener listener : asyncContext.getListeners()) {
			listener.onComplete(new AsyncEvent(asyncContext));
		}
	}

	@Test
	public void testOpenSessionInViewFilter() throws Exception {

		final SessionFactory factory2 = mock(SessionFactory.class);
		final Session manager2 = mock(Session.class);

		given(factory2.openSession()).willReturn(manager2);

		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactory", sessionFactory);
		wac.getDefaultListableBeanFactory().registerSingleton("mySessionFactory", factory2);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig2.addInitParameter("sessionFactoryBeanName", "mySessionFactory");

		final OpenSessionInViewFilter filter = new OpenSessionInViewFilter();
		filter.init(filterConfig);
		final OpenSessionInViewFilter filter2 = new OpenSessionInViewFilter();
		filter2.init(filterConfig2);

		final FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(sessionFactory));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		final FilterChain filterChain2 = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
					throws IOException, ServletException {
				assertTrue(TransactionSynchronizationManager.hasResource(factory2));
				filter.doFilter(servletRequest, servletResponse, filterChain);
			}
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		filter2.doFilter(request, response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		assertNotNull(request.getAttribute("invoked"));

		wac.close();
	}

	@Test
	public void testOpenSessionInViewFilterAsyncScenario() throws Exception {
		final SessionFactory factory2 = mock(SessionFactory.class);
		final Session manager2 = mock(Session.class);

		given(factory2.openSession()).willReturn(manager2);

		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactory", sessionFactory);
		wac.getDefaultListableBeanFactory().registerSingleton("mySessionFactory", factory2);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig2.addInitParameter("sessionFactoryBeanName", "mySessionFactory");

		final OpenSessionInViewFilter filter = new OpenSessionInViewFilter();
		filter.init(filterConfig);
		final OpenSessionInViewFilter filter2 = new OpenSessionInViewFilter();
		filter2.init(filterConfig2);

		final AtomicInteger count = new AtomicInteger(0);

		final FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(sessionFactory));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
				count.incrementAndGet();
			}
		};

		final AtomicInteger count2 = new AtomicInteger(0);

		final FilterChain filterChain2 = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
					throws IOException, ServletException {
				assertTrue(TransactionSynchronizationManager.hasResource(factory2));
				filter.doFilter(servletRequest, servletResponse, filterChain);
				count2.incrementAndGet();
			}
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		AsyncWebRequest asyncWebRequest = mock(AsyncWebRequest.class);
		given(asyncWebRequest.isAsyncStarted()).willReturn(true);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setTaskExecutor(new SyncTaskExecutor());
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "anything";
			}
		});

		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		filter2.doFilter(this.request, this.response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		assertEquals(1, count.get());
		assertEquals(1, count2.get());
		assertNotNull(request.getAttribute("invoked"));
		verify(asyncWebRequest, times(2)).addCompletionHandler(any(Runnable.class));
		verify(asyncWebRequest).addTimeoutHandler(any(Runnable.class));
		verify(asyncWebRequest, times(2)).addCompletionHandler(any(Runnable.class));
		verify(asyncWebRequest).startAsync();

		// Async dispatch after concurrent handling produces result ...

		reset(asyncWebRequest);
		given(asyncWebRequest.isAsyncStarted()).willReturn(false);

		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		filter.doFilter(this.request, this.response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(sessionFactory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		assertEquals(2, count.get());
		assertEquals(2, count2.get());

		wac.close();
	}

	@SuppressWarnings("serial")
	private static class SyncTaskExecutor extends SimpleAsyncTaskExecutor {

		@Override
		public void execute(Runnable task, long startTimeout) {
			task.run();
		}
	}
}
