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
package org.springframework.data.neo4j.repository.cdi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
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
		assertThat(sessions.size(), is(1));
		assertThat(sessions.values(), hasItem(em));
	}

	@SuppressWarnings("unchecked")
	private static ProcessBean<Session> createSessionBeanMock(Bean<Session> bean) {

		ProcessBean<Session> mock = mock(ProcessBean.class);
		when(mock.getBean()).thenReturn(bean);

		return mock;
	}
}
