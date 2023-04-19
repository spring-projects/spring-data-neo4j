package org.springframework.data.neo4j.test;

import org.neo4j.driver.types.Entity;

/**
 * @author Michael J. Simons
 */
public final class TestIdentitySupport {

	/**
	 * @param entity The entity container as received from the server.
	 * @return The internal id
	 */
	@SuppressWarnings("deprecation")
	public static Long getInternalId(Entity entity) {
		return entity.id();
	}

	private TestIdentitySupport() {
	}
}
