package org.springframework.data.neo4j.core.support;

/**
 * Interface to attach to a {@link org.springframework.data.neo4j.core.schema.Node}
 * to indicate if an entity needs to be processed by built-in update procedures.
 * @author Gerrit Meier
 */
public interface UserDefinedChangeSupport {
	/**
	 * Report if this entity needs to be considered for an update.
	 * This includes possible relationships.
	 * @return true, if it should be processed
	 */
	boolean needsUpdate();
}
