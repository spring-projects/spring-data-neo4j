package org.springframework.persistence.support;

import java.lang.annotation.Annotation;

/**
 * Abstract superaspect for aspects that advice
 * field access with a mixin for all types annotated with
 * a given annotation.
 * 
 * @param <ET> annotation on entity
 * @param <N> type of introduced interface
 * 
 * @author Rod Johnson
 */
privileged abstract public aspect AbstractTypeAnnotatingMixinFields<ET extends Annotation, N> 
					extends AbstractMixinFields<N> {
	 
	// ITD to introduce N state to Annotated objects
	declare parents : (@ET *) implements N;

}
