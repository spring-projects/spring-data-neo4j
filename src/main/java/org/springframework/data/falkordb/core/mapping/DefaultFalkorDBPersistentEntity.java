/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
