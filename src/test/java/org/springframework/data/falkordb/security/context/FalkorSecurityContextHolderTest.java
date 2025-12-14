/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.Role;
import org.springframework.data.falkordb.security.model.User;

class FalkorSecurityContextHolderTest {

	@AfterEach
	void tearDown() {
		FalkorSecurityContextHolder.clearContext();
	}

	@Test
	void withContextShouldRestorePreviousContext() {
		User u1 = new User();
		u1.setUsername("u1");
		Role r1 = new Role();
		r1.setName("R1");
		Privilege p1 = new Privilege();
		p1.setGrant(true);
		FalkorSecurityContext ctx1 = new FalkorSecurityContext(u1, Collections.singleton(r1), Collections.singleton(p1));

		User u2 = new User();
		u2.setUsername("u2");
		Role r2 = new Role();
		r2.setName("R2");
		Privilege p2 = new Privilege();
		p2.setGrant(true);
		FalkorSecurityContext ctx2 = new FalkorSecurityContext(u2, Collections.singleton(r2), Collections.singleton(p2));

		FalkorSecurityContextHolder.setContext(ctx1);
		assertThat(FalkorSecurityContextHolder.getContext().getUser().getUsername()).isEqualTo("u1");

		try (FalkorSecurityContextHolder.Scope scope = FalkorSecurityContextHolder.withContext(ctx2)) {
			assertThat(FalkorSecurityContextHolder.getContext().getUser().getUsername()).isEqualTo("u2");
		}

		assertThat(FalkorSecurityContextHolder.getContext().getUser().getUsername()).isEqualTo("u1");
	}

	@Test
	void withContextShouldClearIfNoPreviousContext() {
		User u = new User();
		u.setUsername("u");
		FalkorSecurityContext ctx = new FalkorSecurityContext(u, Collections.<Role>emptySet(), Collections.<Privilege>emptySet());

		assertThat(FalkorSecurityContextHolder.getContext()).isNull();
		try (FalkorSecurityContextHolder.Scope scope = FalkorSecurityContextHolder.withContext(ctx)) {
			assertThat(FalkorSecurityContextHolder.getContext()).isNotNull();
		}
		assertThat(FalkorSecurityContextHolder.getContext()).isNull();
	}

}
