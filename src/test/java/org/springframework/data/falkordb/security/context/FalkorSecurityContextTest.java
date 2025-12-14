/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.springframework.data.falkordb.security.model.Action;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.Role;
import org.springframework.data.falkordb.security.model.User;

class FalkorSecurityContextTest {

	@Test
	void shouldIncludeInheritedRolesAndDefaultRole() {
		Role grandParent = new Role();
		grandParent.setName("ROLE_GRANDPARENT");

		Role parent = new Role();
		parent.setName("ROLE_PARENT");
		parent.setParentRoles(new HashSet<>(Collections.singleton(grandParent)));

		Role child = new Role();
		child.setName("ROLE_CHILD");
		child.setParentRoles(new HashSet<>(Collections.singleton(parent)));

		User user = new User();
		user.setUsername("alice");

		FalkorSecurityContext ctx = new FalkorSecurityContext(user,
				new HashSet<>(Collections.singleton(child)), Collections.emptySet());

		assertThat(ctx.getEffectiveRoles())
				.contains("ROLE_CHILD", "ROLE_PARENT", "ROLE_GRANDPARENT")
				.contains(FalkorSecurityContext.DEFAULT_DEFAULT_ROLE);
	}

	@Test
	void shouldSupportWildcardResourceMatching() {
		User user = new User();
		user.setUsername("alice");

		Privilege any = new Privilege();
		any.setAction(Action.READ);
		any.setResource("*");
		any.setGrant(true);

		FalkorSecurityContext ctx = new FalkorSecurityContext(user,
				Collections.singleton("ROLE_USER"), Collections.singleton(any));

		assertThat(ctx.can(Action.READ, "com.acme.Foo")).isTrue();
		assertThat(ctx.can(Action.WRITE, "com.acme.Foo")).isFalse();
	}

	@Test
	void shouldSupportPropertyWildcardResourceMatching() {
		User user = new User();
		user.setUsername("alice");

		Privilege anyProp = new Privilege();
		anyProp.setAction(Action.READ);
		anyProp.setResource("com.acme.Foo.*");
		anyProp.setGrant(true);

		FalkorSecurityContext ctx = new FalkorSecurityContext(user,
				Collections.singleton("ROLE_USER"), Collections.singleton(anyProp));

		assertThat(ctx.can(Action.READ, "com.acme.Foo.bar")).isTrue();
		assertThat(ctx.can(Action.READ, "com.acme.Foo")).isFalse();
	}
}
