/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.repository.cdi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.ProcessBean;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.session.Session;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Mark Angrish
 */
public class Neo4jRepositoryExtensionTests {

	Bean<Session> session;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		Set<Type> types = Collections.singleton((Type) Session.class);

		session = mock(Bean.class);
		when(session.getTypes()).thenReturn(types);
	}

	@Test
	public void registersSession() {

		Neo4jCdiRepositoryExtension extension = new Neo4jCdiRepositoryExtension();
		extension.processBean(createSessionBeanMock(session));

		assertSessionRegistered(extension, session);
	}

	@SuppressWarnings("unchecked")
	private static void assertSessionRegistered(Neo4jCdiRepositoryExtension extension, Bean<Session> em) {

		Map<Set<Annotation>, Bean<Session>> sessions = (Map<Set<Annotation>, Bean<Session>>) ReflectionTestUtils
				.getField(extension, "sessions");
		assertThat(sessions.size()).isEqualTo(1);
		assertThat(sessions.values()).contains(em);
	}

	@SuppressWarnings("unchecked")
	private static ProcessBean<Session> createSessionBeanMock(Bean<Session> bean) {

		ProcessBean<Session> mock = mock(ProcessBean.class);
		when(mock.getBean()).thenReturn(bean);

		return mock;
	}
}
