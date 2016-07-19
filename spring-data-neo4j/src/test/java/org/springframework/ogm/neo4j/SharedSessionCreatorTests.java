package org.springframework.ogm.neo4j;


import org.junit.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Mark Angrish
 */
public class SharedSessionCreatorTests {

	@Test
	public void proxyingWorksIfInfoReturnsNullSessionInterface() {
		SessionFactory emf = mock(SessionFactory.class);
		// SessionFactoryInfo.getSessionInterface returns null
		assertThat(SharedSessionCreator.createSharedSession(emf), is(notNullValue()));
	}


	@Test(expected = IllegalStateException.class)
	public void transactionRequiredExceptionOnFlush() {
		SessionFactory emf = mock(SessionFactory.class);
		Session em = SharedSessionCreator.createSharedSession(emf);
		em.beginTransaction();
	}

	@Test(expected = IllegalStateException.class)
	public void transactionRequiredExceptionOnSave() {
		SessionFactory emf = mock(SessionFactory.class);
		Session em = SharedSessionCreator.createSharedSession(emf);
		em.save(new Object());
	}

	@Test(expected = IllegalStateException.class)
	public void transactionRequiredExceptionOnDelete() {
		SessionFactory emf = mock(SessionFactory.class);
		Session em = SharedSessionCreator.createSharedSession(emf);
		em.delete(new Object());
	}

}
