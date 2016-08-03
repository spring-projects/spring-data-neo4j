package org.springframework.data.neo4j.repository.cdi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.ProcessBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.session.Session;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Created by markangrish on 03/08/2016.
 */
public class Neo4jRepositoryExtensionTest {

	Bean<Session> session;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		Set<Type> types = Collections.singleton((Type) Session.class);

		session = mock(Bean.class);
		when(session.getTypes()).thenReturn(types);
	}

	@Test
	public void registersEntityManager() {

		Neo4jCdiRepositoryExtension extension = new Neo4jCdiRepositoryExtension();
		extension.processBean(createSessionBeanMock(session));

		assertSessionRegistered(extension, session);
	}

	@SuppressWarnings("unchecked")
	private static void assertSessionRegistered(Neo4jCdiRepositoryExtension extension, Bean<Session> em) {

		Map<Set<Annotation>, Bean<Session>> entityManagers = (Map<Set<Annotation>, Bean<Session>>) ReflectionTestUtils
				.getField(extension, "sessions");
		assertThat(entityManagers.size(), is(1));
		assertThat(entityManagers.values(), hasItem(em));
	}

	@SuppressWarnings("unchecked")
	private static ProcessBean<Session> createSessionBeanMock(Bean<Session> bean) {

		ProcessBean<Session> mock = mock(ProcessBean.class);
		when(mock.getBean()).thenReturn(bean);

		return mock;
	}
}
