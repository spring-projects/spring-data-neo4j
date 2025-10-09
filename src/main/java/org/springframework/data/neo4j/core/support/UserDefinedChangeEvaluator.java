package org.springframework.data.neo4j.core.support;

/**
 * Interface to attach to a {@link org.springframework.data.neo4j.core.schema.Node}
 * to indicate if an entity needs to be processed by built-in update procedures.
 * @param <T> type to implement the UserDefinedChangeEvaluator for
 * @author Gerrit Meier
 */
public interface UserDefinedChangeEvaluator<T> {
	/**
	 * Report if this entity needs to be considered for an update.
	 * This includes possible relationships.
	 * @param instance instance of type `T` to check
	 * @return true, if it should be processed
	 */
	default boolean needsUpdate(T instance) {
		return true;
	}

	Class<T> getEvaluatingClass();
}
