package org.springframework.datastore.graph.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.datastore.graph.api.Direction;
import org.springframework.datastore.graph.api.NodeBacked;

/**
 * @author Michael Hunger
 * @since 27.08.2010
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RelatedTo {
    String type();

    Direction direction() default Direction.OUTGOING;

    Class<? extends NodeBacked> elementClass() default NodeBacked.class;
}
