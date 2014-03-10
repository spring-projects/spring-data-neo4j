/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.cross_store.fieldaccess;

import javax.persistence.Id;

import org.springframework.data.neo4j.fieldaccess.FieldAccessListener;
import org.springframework.data.neo4j.fieldaccess.FieldAccessorListenerFactory;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class JpaIdFieldAccessListenerFactory implements FieldAccessorListenerFactory {
	private final Neo4jTemplate template;

	public JpaIdFieldAccessListenerFactory(Neo4jTemplate template) {
		this.template = template;
	}

	@Override
	public boolean accept(final Neo4jPersistentProperty property) {
		return property.findAnnotation(Id.class) != null;
	}

	@Override
	public FieldAccessListener forField(final Neo4jPersistentProperty property) {
		return new JpaIdFieldListener(property, template);
	}

	public static class JpaIdFieldListener implements FieldAccessListener {
		protected final Neo4jPersistentProperty property;
		private final Neo4jTemplate template;

		public JpaIdFieldListener(final Neo4jPersistentProperty property, Neo4jTemplate template) {
			this.property = property;
			this.template = template;
		}

		@Override
		public void valueChanged(Object entity, Object oldVal, Object newVal) {
			if (newVal != null) {
				template.save(entity);
				/* TODO                EntityState entityState = entity.getEntityState();
				                entityState.persist();
				*/
			}
		}
	}
}
