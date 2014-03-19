/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.config;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.lifecycle.BeforeSaveEvent;

/**
 * Integration tests for Neo4j auditing.
 * 
 * @author Michael Hunger
 * @author Oliver Gierke
 */
public class AuditingIntegrationTests {

	/**
	 * @see DATAGRAPH-328
	 */
	@Test
	public void enablesAuditingAndSetsPropertiesAccordingly() throws InterruptedException {
		runTest(new ClassPathXmlApplicationContext("auditing.xml", getClass())).close();
	}

	/**
	 * @see DATAGRAPH-328
	 */
	@Test
	public void enablesAuditingWithBeanConfigAndSetsPropertiesAccordingly() throws InterruptedException {
		runTest(new ClassPathXmlApplicationContext("auditing-bean.xml", getClass())).close();
	}

	private <T extends ApplicationEventPublisher> T runTest(T context) throws InterruptedException {

		Entity entity = new Entity();
		BeforeSaveEvent<Entity> event = new BeforeSaveEvent<Entity>(this, entity);
		context.publishEvent(event);

		assertThat(entity.created, is(notNullValue()));
		assertThat(entity.modified, is(entity.created));
		Thread.sleep(10);
		entity.id = 1L;
		event = new BeforeSaveEvent<Entity>(this, entity);
		context.publishEvent(event);

		assertThat(entity.created, is(notNullValue()));
		assertThat(entity.modified, is(not(entity.created)));

		return context;
	}

	@NodeEntity
	public static class Entity {

		@GraphId Long id;
		@CreatedDate DateTime created;
		@LastModifiedDate DateTime modified;
	}
}
