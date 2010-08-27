package org.springframework.datastore.graph.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GraphInactive {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Entity {
		
		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.FIELD)
		public @interface Relationship {
			
			String type();
			
			Direction direction() default Direction.OUTGOING;

			Class<? extends NodeBacked> elementClass() default NodeBacked.class;	
		}
		
		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.FIELD)
		public @interface RelationshipEntity {
			String type();
			
			Direction direction() default Direction.OUTGOING;

			Class<? extends RelationshipBacked> elementClass() default RelationshipBacked.class;	
		}
		
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Relationship {
		
		String type() default "";
		
		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.FIELD)
		public @interface StartNode {
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.FIELD)
		public @interface EndNode {
		}
		
	}
	
}
