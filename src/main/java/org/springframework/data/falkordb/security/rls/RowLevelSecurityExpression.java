/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.rls;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.falkordb.security.context.FalkorSecurityContext;
import org.springframework.data.falkordb.security.model.User;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Minimal row-level security expression.
 *
 * Currently supported:
 * {@code <entityField> == principal.<userField>}
 *
 * Example: {@code owner == principal.username}
 */
public final class RowLevelSecurityExpression {

	private static final Pattern EQ_PRINCIPAL_PATTERN = Pattern.compile(
			"^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*==\\s*principal\\.([A-Za-z_][A-Za-z0-9_]*)\\s*$");

	private final String entityField;
	private final String principalField;

	private RowLevelSecurityExpression(String entityField, String principalField) {
		this.entityField = entityField;
		this.principalField = principalField;
	}

	public static RowLevelSecurityExpression parse(String expression) {
		Assert.hasText(expression, "expression must not be null or empty");

		Matcher matcher = EQ_PRINCIPAL_PATTERN.matcher(expression);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Unsupported row-level security expression: " + expression);
		}
		return new RowLevelSecurityExpression(matcher.group(1), matcher.group(2));
	}

	public String getEntityField() {
		return this.entityField;
	}

	public String getPrincipalField() {
		return this.principalField;
	}

	public boolean matches(Object entity, FalkorSecurityContext context) {
		Objects.requireNonNull(entity, "entity must not be null");
		Objects.requireNonNull(context, "context must not be null");
		User user = context.getUser();
		if (user == null) {
			return false;
		}

		Object entityValue = readField(entity, this.entityField);
		Object principalValue = readPrincipalField(user, this.principalField);

		if (entityValue == null || principalValue == null) {
			return false;
		}
		return String.valueOf(entityValue).equals(String.valueOf(principalValue));
	}

	public String toCypherPredicate(String nodeAlias, String parameterName) {
		Assert.isTrue(StringUtils.hasText(nodeAlias), "nodeAlias must not be null or empty");
		Assert.isTrue(StringUtils.hasText(parameterName), "parameterName must not be null or empty");
		return nodeAlias + "." + this.entityField + " = $" + parameterName;
	}

	@Nullable
	public Object resolvePrincipalValue(FalkorSecurityContext context) {
		Objects.requireNonNull(context, "context must not be null");
		User user = context.getUser();
		if (user == null) {
			return null;
		}
		return readPrincipalField(user, this.principalField);
	}

	@Nullable
	private Object readPrincipalField(User user, String fieldName) {
		if (user == null || !StringUtils.hasText(fieldName)) {
			return null;
		}
		// Fast-path for common fields
		switch (fieldName) {
			case "username":
				return user.getUsername();
			case "email":
				return user.getEmail();
			default:
				return readField(user, fieldName);
		}
	}

	@Nullable
	private Object readField(Object target, String fieldName) {
		Field field = findField(target.getClass(), fieldName);
		if (field == null) {
			return null;
		}
		boolean accessible = field.canAccess(target);
		try {
			if (!accessible) {
				field.setAccessible(true);
			}
			return field.get(target);
		}
		catch (IllegalAccessException ignored) {
			return null;
		}
		finally {
			if (!accessible) {
				field.setAccessible(false);
			}
		}
	}

	@Nullable
	private Field findField(Class<?> type, String name) {
		Class<?> current = type;
		while (current != null && current != Object.class) {
			for (Field field : current.getDeclaredFields()) {
				if (field.getName().equals(name)) {
					return field;
				}
			}
			current = current.getSuperclass();
		}
		return null;
	}

}
