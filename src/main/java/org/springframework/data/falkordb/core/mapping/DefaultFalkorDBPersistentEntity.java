/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.falkordb.core.mapping;

import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link FalkorDBPersistentEntity}.
 *
 * @param <T> the entity type
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public class DefaultFalkorDBPersistentEntity<T> extends BasicPersistentEntity<T, FalkorDBPersistentProperty>
		implements FalkorDBPersistentEntity<T> {

	private final String primaryLabel;

	private final String[] labels;

	/**
	 * Creates a new {@link DefaultFalkorDBPersistentEntity} from the given
	 * {@link TypeInformation}.
	 * @param information must not be {@literal null}.
	 */
	public DefaultFalkorDBPersistentEntity(TypeInformation<T> information) {
		super(information);

		Node nodeAnnotation = findAnnotation(Node.class);

		if (nodeAnnotation != null) {
			// Determine primary label
			if (StringUtils.hasText(nodeAnnotation.primaryLabel())) {
				this.primaryLabel = nodeAnnotation.primaryLabel();
			}
			else if (nodeAnnotation.labels().length > 0) {
				this.primaryLabel = nodeAnnotation.labels()[0];
			}
			else if (nodeAnnotation.value().length > 0) {
				this.primaryLabel = nodeAnnotation.value()[0];
			}
			else {
				this.primaryLabel = getType().getSimpleName();
			}

			// Determine all labels
			if (nodeAnnotation.labels().length > 0) {
				this.labels = nodeAnnotation.labels();
			}
			else if (nodeAnnotation.value().length > 0) {
				this.labels = nodeAnnotation.value();
			}
			else {
				this.labels = new String[] { this.primaryLabel };
			}
		}
		else {
			// Fallback to class name if no annotation
			this.primaryLabel = getType().getSimpleName();
			this.labels = new String[] { this.primaryLabel };
		}
	}

	@Override
	public String getPrimaryLabel() {
		return this.primaryLabel;
	}

	@Override
	public String[] getLabels() {
		return this.labels.clone();
	}

	@Override
	public boolean isNodeEntity() {
		return findAnnotation(Node.class) != null;
	}

	@Override
	public boolean hasCompositeId() {
		// Simple implementation - can be enhanced later
		return false;
	}

}
