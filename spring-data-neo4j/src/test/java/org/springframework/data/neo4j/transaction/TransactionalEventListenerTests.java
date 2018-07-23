package org.springframework.data.neo4j.transaction;

import static org.junit.Assert.*;
import static org.springframework.transaction.event.TransactionPhase.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * These tests are copied from <a href=
 * "https://github.com/spring-projects/spring-framework/blob/master/spring-tx/src/test/java/org/springframework/transaction/event/TransactionalEventListenerTests.java">the
 * spring framework transactional event listener tests</a> and modified to work with a version of the
 * Neo4jTransactionManager.
 *
 * @author Mark Angrish
 * @see DATAGRAPH-883
 */
public class TransactionalEventListenerTests {

	private ConfigurableApplicationContext context;

	private EventCollector eventCollector;

	private TransactionTemplate transactionTemplate = new TransactionTemplate(new CallCountingTransactionManager());

	@Rule public final ExpectedException thrown = ExpectedException.none();

	@Before
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void immediately() {
		load(ImmediateTestListener.class);
		this.transactionTemplate.execute(status -> {
			TransactionalEventListenerTests.this.getContext().publishEvent("test");
			TransactionalEventListenerTests.this.getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "test");
			TransactionalEventListenerTests.this.getEventCollector().assertTotalEventsCount(1);
			return null;
		});
		getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "test");
		getEventCollector().assertTotalEventsCount(1);
	}

	@Test
	public void immediatelyImpactsCurrentTransaction() {
		load(ImmediateTestListener.class, BeforeCommitTestListener.class);
		try {
			this.transactionTemplate.execute(status -> {
				TransactionalEventListenerTests.this.getContext().publishEvent("FAIL");
				fail("Should have thrown an exception at this point");
				return null;
			});
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("Test exception"));
			assertTrue(e.getMessage().contains(EventCollector.IMMEDIATELY));
		}
		getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "FAIL");
		getEventCollector().assertTotalEventsCount(1);
	}

	@Test
	public void afterCompletionCommit() {
		load(AfterCompletionTestListener.class);
		this.transactionTemplate.execute(status -> {
			TransactionalEventListenerTests.this.getContext().publishEvent("test");
			TransactionalEventListenerTests.this.getEventCollector().assertNoEventReceived();
			return null;
		});
		getEventCollector().assertEvents(EventCollector.AFTER_COMPLETION, "test");
		getEventCollector().assertTotalEventsCount(1); // After rollback not invoked
	}

	@Test
	public void afterCompletionRollback() {
		load(AfterCompletionTestListener.class);
		this.transactionTemplate.execute(status -> {
			TransactionalEventListenerTests.this.getContext().publishEvent("test");
			TransactionalEventListenerTests.this.getEventCollector().assertNoEventReceived();
			status.setRollbackOnly();
			return null;
		});
		getEventCollector().assertEvents(EventCollector.AFTER_COMPLETION, "test");
		getEventCollector().assertTotalEventsCount(1); // After rollback not invoked
	}

	@Test
	public void afterCommit() {
		load(AfterCompletionExplicitTestListener.class);
		this.transactionTemplate.execute(status -> {
			TransactionalEventListenerTests.this.getContext().publishEvent("test");
			TransactionalEventListenerTests.this.getEventCollector().assertNoEventReceived();
			return null;
		});
		getEventCollector().assertEvents(EventCollector.AFTER_COMMIT, "test");
		getEventCollector().assertTotalEventsCount(1); // After rollback not invoked
	}

	@Test
	public void afterCommitWithTransactionalComponentListenerProxiedViaDynamicProxy() {
		load(AfterCompletionTestListener.class, TransactionalComponentTestListener.class);
		this.transactionTemplate.execute(status -> {
			TransactionalEventListenerTests.this.getContext().publishEvent("SKIP");
			TransactionalEventListenerTests.this.getEventCollector().assertNoEventReceived();
			return null;
		});
		getEventCollector().assertNoEventReceived();
	}

	@Test
	public void afterRollback() {
		load(AfterCompletionExplicitTestListener.class);
		this.transactionTemplate.execute(status -> {
			TransactionalEventListenerTests.this.getContext().publishEvent("test");
			TransactionalEventListenerTests.this.getEventCollector().assertNoEventReceived();
			status.setRollbackOnly();
			return null;
		});
		getEventCollector().assertEvents(EventCollector.AFTER_ROLLBACK, "test");
		getEventCollector().assertTotalEventsCount(1); // After commit not invoked
	}

	@Test
	public void beforeCommit() {
		load(BeforeCommitTestListener.class);
		this.transactionTemplate.execute(status -> {
			TransactionSynchronizationManager.registerSynchronization(new EventTransactionSynchronization(10) {
				@Override
				public void beforeCommit(boolean readOnly) {
					getEventCollector().assertNoEventReceived(); // Not seen yet
				}
			});
			TransactionSynchronizationManager.registerSynchronization(new EventTransactionSynchronization(20) {
				@Override
				public void beforeCommit(boolean readOnly) {
					getEventCollector().assertEvents(EventCollector.BEFORE_COMMIT, "test");
					getEventCollector().assertTotalEventsCount(1);
				}
			});
			TransactionalEventListenerTests.this.getContext().publishEvent("test");
			TransactionalEventListenerTests.this.getEventCollector().assertNoEventReceived();
			return null;
		});
		getEventCollector().assertEvents(EventCollector.BEFORE_COMMIT, "test");
		getEventCollector().assertTotalEventsCount(1);
	}

	@Test
	public void beforeCommitWithException() { // Validates the custom synchronization is invoked
		load(BeforeCommitTestListener.class);
		try {
			this.transactionTemplate.execute(status -> {
				TransactionSynchronizationManager.registerSynchronization(new EventTransactionSynchronization(10) {
					@Override
					public void beforeCommit(boolean readOnly) {
						throw new IllegalStateException("test");
					}
				});
				TransactionalEventListenerTests.this.getContext().publishEvent("test");
				TransactionalEventListenerTests.this.getEventCollector().assertNoEventReceived();
				return null;
			});
			fail("Should have thrown an exception");
		} catch (IllegalStateException e) {
			// Test exception - ignore
		}
		getEventCollector().assertNoEventReceived(); // Before commit not invoked
	}

	@Test
	public void regularTransaction() {
		load(ImmediateTestListener.class, BeforeCommitTestListener.class, AfterCompletionExplicitTestListener.class);
		this.transactionTemplate.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				TransactionSynchronizationManager.registerSynchronization(new EventTransactionSynchronization(10) {
					@Override
					public void beforeCommit(boolean readOnly) {
						getEventCollector().assertTotalEventsCount(1); // Immediate event
						getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "test");
					}
				});
				TransactionSynchronizationManager.registerSynchronization(new EventTransactionSynchronization(20) {
					@Override
					public void beforeCommit(boolean readOnly) {
						getEventCollector().assertEvents(EventCollector.BEFORE_COMMIT, "test");
						getEventCollector().assertTotalEventsCount(2);
					}
				});
				TransactionalEventListenerTests.this.getContext().publishEvent("test");
				TransactionalEventListenerTests.this.getEventCollector().assertTotalEventsCount(1);
				return null;
			}
		});
		getEventCollector().assertEvents(EventCollector.AFTER_COMMIT, "test");
		getEventCollector().assertTotalEventsCount(3); // Immediate, before commit, after commit
	}

	@Test
	public void noTransaction() {
		load(BeforeCommitTestListener.class, AfterCompletionTestListener.class, AfterCompletionExplicitTestListener.class);
		this.context.publishEvent("test");
		getEventCollector().assertTotalEventsCount(0);
	}

	@Test
	public void noTransactionWithFallbackExecution() {
		load(FallbackExecutionTestListener.class);
		this.context.publishEvent("test");
		this.eventCollector.assertEvents(EventCollector.BEFORE_COMMIT, "test");
		this.eventCollector.assertEvents(EventCollector.AFTER_COMMIT, "test");
		this.eventCollector.assertEvents(EventCollector.AFTER_ROLLBACK, "test");
		this.eventCollector.assertEvents(EventCollector.AFTER_COMPLETION, "test");
		getEventCollector().assertTotalEventsCount(4);
	}

	@Test
	public void conditionFoundOnTransactionalEventListener() {
		load(ImmediateTestListener.class);
		this.transactionTemplate.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				TransactionalEventListenerTests.this.getContext().publishEvent("SKIP");
				TransactionalEventListenerTests.this.getEventCollector().assertNoEventReceived();
				return null;
			}
		});
		getEventCollector().assertNoEventReceived();
	}

	@Test
	public void afterCommitMetaAnnotation() throws Exception {
		load(AfterCommitMetaAnnotationTestListener.class);
		this.transactionTemplate.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				TransactionalEventListenerTests.this.getContext().publishEvent("test");
				TransactionalEventListenerTests.this.getEventCollector().assertNoEventReceived();
				return null;
			}
		});
		getEventCollector().assertEvents(EventCollector.AFTER_COMMIT, "test");
		getEventCollector().assertTotalEventsCount(1);
	}

	@Test
	public void conditionFoundOnMetaAnnotation() {
		load(AfterCommitMetaAnnotationTestListener.class);
		this.transactionTemplate.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				TransactionalEventListenerTests.this.getContext().publishEvent("SKIP");
				TransactionalEventListenerTests.this.getEventCollector().assertNoEventReceived();
				return null;
			}
		});
		getEventCollector().assertNoEventReceived();
	}

	protected EventCollector getEventCollector() {
		return eventCollector;
	}

	protected ConfigurableApplicationContext getContext() {
		return context;
	}

	private void load(Class<?>... classes) {
		List<Class<?>> allClasses = new ArrayList<>();
		allClasses.add(BasicConfiguration.class);
		allClasses.addAll(Arrays.asList(classes));
		doLoad(allClasses.toArray(new Class<?>[allClasses.size()]));
	}

	private void doLoad(Class<?>... classes) {
		this.context = new AnnotationConfigApplicationContext(classes);
		this.eventCollector = this.context.getBean(EventCollector.class);
	}

	@Configuration
	static class BasicConfiguration {

		@Bean // set automatically with tx management
		public TransactionalEventListenerFactory transactionalEventListenerFactory() {
			return new TransactionalEventListenerFactory();
		}

		@Bean
		public EventCollector eventCollector() {
			return new EventCollector();
		}
	}

	@EnableTransactionManagement
	@Configuration
	static class TransactionalConfiguration {

		@Bean
		public CallCountingTransactionManager transactionManager() {
			return new CallCountingTransactionManager();
		}
	}

	static class EventCollector {

		public static final String IMMEDIATELY = "IMMEDIATELY";

		public static final String BEFORE_COMMIT = "BEFORE_COMMIT";

		public static final String AFTER_COMPLETION = "AFTER_COMPLETION";

		public static final String AFTER_COMMIT = "AFTER_COMMIT";

		public static final String AFTER_ROLLBACK = "AFTER_ROLLBACK";

		public static final String[] ALL_PHASES = { IMMEDIATELY, BEFORE_COMMIT, AFTER_COMMIT, AFTER_ROLLBACK };

		private final MultiValueMap<String, Object> events = new LinkedMultiValueMap<>();

		public void addEvent(String phase, Object event) {
			this.events.add(phase, event);
		}

		public List<Object> getEvents(String phase) {
			List<Object> v;
			return (((v = events.get(phase)) != null) || events.containsKey(phase)) ? v : Collections.emptyList();
		}

		public void assertNoEventReceived(String... phases) {
			if (phases.length == 0) { // All values if none set
				phases = ALL_PHASES;
			}
			for (String phase : phases) {
				List<Object> eventsForPhase = getEvents(phase);
				assertEquals("Expected no events for phase '" + phase + "' " + "but got " + eventsForPhase + ":", 0,
						eventsForPhase.size());
			}
		}

		public void assertEvents(String phase, Object... expected) {
			List<Object> actual = getEvents(phase);
			assertEquals("wrong number of events for phase '" + phase + "'", expected.length, actual.size());
			for (int i = 0; i < expected.length; i++) {
				assertEquals("Wrong event for phase '" + phase + "' at index " + i, expected[i], actual.get(i));
			}
		}

		public void assertTotalEventsCount(int number) {
			int size = 0;
			for (Map.Entry<String, List<Object>> entry : this.events.entrySet()) {
				size += entry.getValue().size();
			}
			assertEquals("Wrong number of total events (" + this.events.size() + ") " + "registered phase(s)", number, size);
		}
	}

	static abstract class BaseTransactionalTestListener {

		static final String FAIL_MSG = "FAIL";

		@Autowired private EventCollector eventCollector;

		public void handleEvent(String phase, String data) {
			this.eventCollector.addEvent(phase, data);
			if (FAIL_MSG.equals(data)) {
				throw new IllegalStateException("Test exception on phase '" + phase + "'");
			}
		}
	}

	@Component
	static class ImmediateTestListener extends BaseTransactionalTestListener {

		@EventListener(condition = "!'SKIP'.equals(#data)")
		public void handleImmediately(String data) {
			handleEvent(EventCollector.IMMEDIATELY, data);
		}
	}

	@Component
	static class AfterCompletionTestListener extends BaseTransactionalTestListener {

		@TransactionalEventListener(phase = AFTER_COMPLETION)
		public void handleAfterCompletion(String data) {
			handleEvent(EventCollector.AFTER_COMPLETION, data);
		}
	}

	@Component
	static class AfterCompletionExplicitTestListener extends BaseTransactionalTestListener {

		@TransactionalEventListener(phase = AFTER_COMMIT)
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}

		@TransactionalEventListener(phase = AFTER_ROLLBACK)
		public void handleAfterRollback(String data) {
			handleEvent(EventCollector.AFTER_ROLLBACK, data);
		}
	}

	@Transactional
	@Component
	static interface TransactionalComponentTestListenerInterface {

		// Cannot use #data in condition due to dynamic proxy.
		@TransactionalEventListener(condition = "!'SKIP'.equals(#p0)")
		void handleAfterCommit(String data);
	}

	static class TransactionalComponentTestListener extends BaseTransactionalTestListener
			implements TransactionalComponentTestListenerInterface {

		@Override
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}
	}

	@Component
	static class BeforeCommitTestListener extends BaseTransactionalTestListener {

		@TransactionalEventListener(phase = BEFORE_COMMIT)
		@Order(15)
		public void handleBeforeCommit(String data) {
			handleEvent(EventCollector.BEFORE_COMMIT, data);
		}
	}

	@Component
	static class FallbackExecutionTestListener extends BaseTransactionalTestListener {

		@TransactionalEventListener(phase = BEFORE_COMMIT, fallbackExecution = true)
		public void handleBeforeCommit(String data) {
			handleEvent(EventCollector.BEFORE_COMMIT, data);
		}

		@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}

		@TransactionalEventListener(phase = AFTER_ROLLBACK, fallbackExecution = true)
		public void handleAfterRollback(String data) {
			handleEvent(EventCollector.AFTER_ROLLBACK, data);
		}

		@TransactionalEventListener(phase = AFTER_COMPLETION, fallbackExecution = true)
		public void handleAfterCompletion(String data) {
			handleEvent(EventCollector.AFTER_COMPLETION, data);
		}
	}

	@TransactionalEventListener(phase = AFTER_COMMIT, condition = "!'SKIP'.equals(#p0)")
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface AfterCommitEventListener {

	}

	@Component
	static class AfterCommitMetaAnnotationTestListener extends BaseTransactionalTestListener {

		@AfterCommitEventListener
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}
	}

	static class EventTransactionSynchronization extends TransactionSynchronizationAdapter {

		private final int order;

		EventTransactionSynchronization(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return order;
		}
	}

	@SuppressWarnings("serial")
	static class CallCountingTransactionManager extends AbstractPlatformTransactionManager {

		public TransactionDefinition lastDefinition;
		public int begun;
		public int commits;
		public int rollbacks;
		public int inflight;

		@Override
		protected Object doGetTransaction() {
			return new Object();
		}

		@Override
		protected void doBegin(Object transaction, TransactionDefinition definition) {
			this.lastDefinition = definition;
			++begun;
			++inflight;
		}

		@Override
		protected void doCommit(DefaultTransactionStatus status) {
			++commits;
			--inflight;
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) {
			++rollbacks;
			--inflight;
		}

		public void clear() {
			begun = commits = rollbacks = inflight = 0;
		}

	}
}
