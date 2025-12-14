/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.rls;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.falkordb.core.query.FalkorDBQueryRewriter;
import org.springframework.data.falkordb.core.query.RewrittenQuery;
import org.springframework.data.falkordb.security.annotation.SecurityMetadata;
import org.springframework.data.falkordb.security.annotation.SecurityMetadataUtils;
import org.springframework.data.falkordb.security.context.FalkorSecurityContext;
import org.springframework.data.falkordb.security.context.FalkorSecurityContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Rewrites generated Cypher queries to include row-level security predicates.
 *
 * Notes:
 * - Intentionally conservative: only rewrites simple {@code MATCH (n:Label) ... RETURN ...} queries
 *   that use the {@code n} alias.
 * - Fails closed: if an RLS expression exists but cannot be parsed or principal data is missing,
 *   it injects a predicate that returns no rows.
 */
public class RowLevelSecurityQueryRewriter implements FalkorDBQueryRewriter {

	private static final String NODE_ALIAS = "n";
	private static final String WHERE_TOKEN = " WHERE ";
	private static final String RETURN_TOKEN = " RETURN ";
	private static final String DELETE_TOKEN = " DELETE ";

	@Override
	public RewrittenQuery rewrite(String cypher, Map<String, Object> parameters, @Nullable Class<?> domainType) {
		if (domainType == null) {
			return RewrittenQuery.of(cypher, parameters);
		}

		SecurityMetadata metadata = SecurityMetadataUtils.resolveMetadata(domainType);
		String filter = metadata.getRowFilterExpression();
		if (!StringUtils.hasText(filter)) {
			return RewrittenQuery.of(cypher, parameters);
		}

		// Only attempt if query seems to be a basic MATCH query using our default alias.
		String upper = cypher.toUpperCase(Locale.ROOT);
		if (!upper.contains("MATCH") || !(upper.contains("(" + NODE_ALIAS.toUpperCase(Locale.ROOT))
				|| cypher.contains("(" + NODE_ALIAS))) {
			return RewrittenQuery.of(cypher, parameters);
		}

		RowLevelSecurityExpression expression;
		try {
			expression = RowLevelSecurityExpression.parse(filter);
		}
		catch (IllegalArgumentException ex) {
			return injectPredicate(cypher, parameters, "1 = 0");
		}

		FalkorSecurityContext ctx = FalkorSecurityContextHolder.getContext();
		if (ctx == null || ctx.getUser() == null) {
			return injectPredicate(cypher, parameters, "1 = 0");
		}

		String paramName = uniqueParamName(parameters, "__rls0");
		Map<String, Object> updated = new HashMap<>(parameters);
		updated.put(paramName, expression.resolvePrincipalValue(ctx));

		String predicate = expression.toCypherPredicate(NODE_ALIAS, paramName);
		return injectPredicate(cypher, updated, predicate);
	}

	private RewrittenQuery injectPredicate(String cypher, Map<String, Object> parameters, String predicate) {
		String upper = cypher.toUpperCase(Locale.ROOT);

		int returnIndex = upper.indexOf(RETURN_TOKEN);
		int deleteIndex = upper.indexOf(DELETE_TOKEN);
		int clauseIndex = returnIndex >= 0 ? returnIndex : deleteIndex;
		if (clauseIndex < 0) {
			// Unknown structure; do not rewrite.
			return RewrittenQuery.of(cypher, parameters);
		}

		int whereIndex = upper.indexOf(WHERE_TOKEN);
		boolean hasWhereBeforeClause = whereIndex >= 0 && whereIndex < clauseIndex;

		StringBuilder sb = new StringBuilder(cypher.length() + predicate.length() + 16);
		sb.append(cypher, 0, clauseIndex);
		if (hasWhereBeforeClause) {
			sb.append(" AND (").append(predicate).append(")");
		}
		else {
			sb.append(" WHERE (").append(predicate).append(")");
		}
		sb.append(cypher.substring(clauseIndex));

		return RewrittenQuery.of(sb.toString(), parameters);
	}

	private String uniqueParamName(Map<String, Object> parameters, String base) {
		String name = base;
		int i = 0;
		while (parameters.containsKey(name)) {
			i++;
			name = base + "_" + i;
		}
		return name;
	}

}
