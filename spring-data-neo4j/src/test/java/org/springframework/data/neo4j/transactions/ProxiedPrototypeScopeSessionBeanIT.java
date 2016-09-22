package org.springframework.data.neo4j.transactions;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.transactions.domain.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author vince
 */
@ContextConfiguration(classes = {ProxiedPrototypeSessionBeanContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class ProxiedPrototypeScopeSessionBeanIT extends MultiDriverTestClass {

	@Autowired
	Session session; // proxied prototype

	@Test
	public void shouldUseDifferentSessionBeanOnEachRequest() {

		User user = new User();
		user.setName("bilbo baggins");
		session.save(user);
		user.setSpecies("hobbit");

		user = session.load(User.class, user.id());

		Assert.assertEquals(null, user.getSpecies()); // session object is different
	}
}
