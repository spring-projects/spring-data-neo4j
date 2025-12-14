/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.rls;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.falkordb.integration.RlsDocument;
import org.springframework.data.falkordb.security.context.FalkorSecurityContext;
import org.springframework.data.falkordb.security.context.FalkorSecurityContextHolder;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.Role;
import org.springframework.data.falkordb.security.model.User;

class RowLevelSecurityExpressionTest {

	@AfterEach
	void tearDown() {
		FalkorSecurityContextHolder.clearContext();
	}

	@Test
	void shouldParseAndEvaluateOwnerUsernameExpression() {
		RowLevelSecurityExpression expression = RowLevelSecurityExpression.parse("owner == principal.username");
		assertThat(expression.getEntityField()).isEqualTo("owner");
		assertThat(expression.getPrincipalField()).isEqualTo("username");

		Role role = new Role();
		role.setName("ROLE_USER");
		User user = new User();
		user.setUsername("alice");

		Privilege p = new Privilege();
		p.setGrant(true);

		FalkorSecurityContext ctx = new FalkorSecurityContext(user, Collections.singleton(role), Collections.singleton(p));

		RlsDocument allowed = new RlsDocument(null, "A", "alice");
		RlsDocument denied = new RlsDocument(null, "B", "bob");

		assertThat(expression.matches(allowed, ctx)).isTrue();
		assertThat(expression.matches(denied, ctx)).isFalse();
	}

	@Test
	void shouldRewriteSimpleMatchReturnQuery() {
		RowLevelSecurityQueryRewriter rewriter = new RowLevelSecurityQueryRewriter();

		Role role = new Role();
		role.setName("ROLE_USER");
		User user = new User();
		user.setUsername("alice");

		Privilege p = new Privilege();
		p.setGrant(true);

		FalkorSecurityContext ctx = new FalkorSecurityContext(user, Collections.singleton(role), Collections.singleton(p));
		FalkorSecurityContextHolder.setContext(ctx);

		String cypher = "MATCH (n:SecureDocument) RETURN n";
		var rewritten = rewriter.rewrite(cypher, Collections.emptyMap(), RlsDocument.class);

		assertThat(rewritten.getCypher()).contains("WHERE (");
		assertThat(rewritten.getCypher()).contains("n.owner");
		assertThat(rewritten.getCypher()).contains("RETURN n");
		assertThat(rewritten.getParameters().values()).contains("alice");
	}

	@Test
	void shouldAppendPredicateToExistingWhereClause() {
		RowLevelSecurityQueryRewriter rewriter = new RowLevelSecurityQueryRewriter();

		User user = new User();
		user.setUsername("alice");
		FalkorSecurityContext ctx = new FalkorSecurityContext(user, Collections.<Role>emptySet(), Collections.<Privilege>emptySet());
		FalkorSecurityContextHolder.setContext(ctx);

		String cypher = "MATCH (n:SecureDocument) WHERE id(n) = $id RETURN n";
		var rewritten = rewriter.rewrite(cypher, Collections.singletonMap("id", 1L), RlsDocument.class);

		assertThat(rewritten.getCypher()).contains("WHERE id(n) = $id AND (");
		assertThat(rewritten.getCypher()).contains("n.owner");
		assertThat(rewritten.getParameters()).containsKey("id");
		assertThat(rewritten.getParameters().values()).contains("alice");
	}

}
