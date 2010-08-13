package org.springframework.persistence.graph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.persistence.graph.neo4j.NodeBacked;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Relationship {
	
	String type();
	
	Direction direction();

	Class<? extends NodeBacked> elementClass() default NodeBacked.class;

}
