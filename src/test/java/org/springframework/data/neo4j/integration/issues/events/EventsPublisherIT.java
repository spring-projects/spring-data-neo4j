/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration.issues.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class EventsPublisherIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	static AtomicBoolean receivedBeforeCommitEvent = new AtomicBoolean(false);

	static AtomicBoolean receivedAfterCommitEvent = new AtomicBoolean(false);

	@Test
		// GH-2580
	void beforeAndAfterCommitEventsShouldWork(@Autowired Neo4jObjectService service) {

		service.save("foobar");
		assertThat(receivedBeforeCommitEvent).isTrue();
		assertThat(receivedAfterCommitEvent).isTrue();
	}

	@Component
	static class Neo4jObjectListener {

		private final Neo4jObjectService service;

		Neo4jObjectListener(Neo4jObjectService service) {
			this.service = service;
		}

		@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
		public void onBeforeCommit(Neo4jMessage message) {
			Optional<Neo4jObject> optionalNeo4jObject = service.findById(message.getMessageId());
			receivedBeforeCommitEvent.compareAndSet(false, optionalNeo4jObject.isPresent());
		}

		@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
		@Transactional(propagation = Propagation.REQUIRES_NEW)
		public void onAfterCommit(Neo4jMessage message) {
			Optional<Neo4jObject> optionalNeo4jObject = service.findById(message.getMessageId());
			receivedAfterCommitEvent.compareAndSet(false, optionalNeo4jObject.isPresent());
		}
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@ComponentScan
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(
				Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singleton(Neo4jObject.class.getPackage().getName());
		}

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}

	static class Neo4jMessage {
		private final String messageId;

		Neo4jMessage(String messageId) {
			this.messageId = messageId;
		}

		public String getMessageId() {
			return this.messageId;
		}

		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof Neo4jMessage)) {
				return false;
			}
			final Neo4jMessage other = (Neo4jMessage) o;
			if (!other.canEqual((Object) this)) {
				return false;
			}
			final Object this$messageId = this.getMessageId();
			final Object other$messageId = other.getMessageId();
			if (this$messageId == null ? other$messageId != null : !this$messageId.equals(other$messageId)) {
				return false;
			}
			return true;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof Neo4jMessage;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $messageId = this.getMessageId();
			result = result * PRIME + ($messageId == null ? 43 : $messageId.hashCode());
			return result;
		}

		public String toString() {
			return "EventsPublisherIT.Neo4jMessage(messageId=" + this.getMessageId() + ")";
		}
	}

	@Repository
	interface Neo4jObjectRepository extends Neo4jRepository<Neo4jObject, String> {
	}
}
