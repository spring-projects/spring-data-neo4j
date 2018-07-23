/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.data.neo4j.repository.cdi;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.data.neo4j.examples.friends.domain.Person;

/**
 * Simple component exposing a {@link org.neo4j.ogm.session.Session} as CDI bean.
 *
 * @author Mark Paluch
 * @see DATAGRAPH-879
 */
class Neo4jCdiProducer {

	@Produces
	@Singleton
	SessionFactory createSessionFactorySession() {
		return new SessionFactory(MultiDriverTestClass.getBaseConfiguration().build(), getClass().getPackage().getName(),
				Person.class.getPackage().getName());
	}

	void close(@Disposes SessionFactory sessionFactory) {
		sessionFactory.close();
	}

	@Produces
	@Singleton
	Session createSession(SessionFactory sessionFactory) {
		return sessionFactory.openSession();
	}

	@Produces
	@Singleton
	@PersonDB
	@OtherQualifier
	Session createQualifiedSession(SessionFactory sessionFactory) {
		return createSession(sessionFactory);
	}
}
