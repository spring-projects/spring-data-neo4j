/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.policy;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.security.manager.RBACManager;
import org.springframework.data.falkordb.security.model.Action;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.ResourceType;
import org.springframework.util.Assert;

/**
 * Declarative RBAC policy DSL similar to the Python implementation.
 *
 * This class delegates to {@link RBACManager} so that admin checks are enforced
 * consistently.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class SecurityPolicy {

	private final RBACManager rbac;

	public SecurityPolicy(RBACManager rbac) {
		Assert.notNull(rbac, "rbac must not be null");
		this.rbac = rbac;
	}

	public Privilege grant(Action action, String resourcePattern, String toRole) {
		ResourcePattern parsed = ResourcePattern.parse(resourcePattern);
		return this.rbac.grantPrivilege(toRole, action, parsed.type(), parsed.labelOrType(), parsed.property());
	}

	public Privilege deny(Action action, String resourcePattern, String toRole) {
		ResourcePattern parsed = ResourcePattern.parse(resourcePattern);
		return this.rbac.denyPrivilege(toRole, action, parsed.type(), parsed.labelOrType(), parsed.property());
	}

	public void revoke(Action action, String resourcePattern, String fromRole) {
		ResourcePattern parsed = ResourcePattern.parse(resourcePattern);
		this.rbac.revokePrivilege(fromRole, action, parsed.type(), parsed.labelOrType(), parsed.property());
	}

	public Privilege grant(Action action, Class<?> entityType, String toRole) {
		Assert.notNull(entityType, "entityType must not be null");
		return this.rbac.grantPrivilege(toRole, action, ResourceType.NODE, entityType.getName(), null);
	}

	public Privilege grantProperty(Action action, Class<?> entityType, String property, String toRole) {
		Assert.notNull(entityType, "entityType must not be null");
		Assert.hasText(property, "property must not be empty");
		return this.rbac.grantPrivilege(toRole, action, ResourceType.PROPERTY, entityType.getName(), property);
	}

	public Privilege deny(Action action, Class<?> entityType, String toRole) {
		Assert.notNull(entityType, "entityType must not be null");
		return this.rbac.denyPrivilege(toRole, action, ResourceType.NODE, entityType.getName(), null);
	}

	public Privilege denyProperty(Action action, Class<?> entityType, String property, String toRole) {
		Assert.notNull(entityType, "entityType must not be null");
		Assert.hasText(property, "property must not be empty");
		return this.rbac.denyPrivilege(toRole, action, ResourceType.PROPERTY, entityType.getName(), property);
	}
}
