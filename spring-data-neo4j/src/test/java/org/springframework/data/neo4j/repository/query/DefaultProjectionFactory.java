/*
 * Copyright 2011-2014 the original author or authors.
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
package org.springframework.data.neo4j.repository.query;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.List;

/**
 * Default implementation of {@link ProjectionFactory}.
 * 
 * @author Brad Nussbaum
 */
public class DefaultProjectionFactory implements ProjectionFactory {

	@Override
	public <T> T createProjection(Class<T> projectionType, Object source) {
		return null;
	}

	@Override
	public <T> T createProjection(Class<T> projectionType) {
		return null;
	}

	@Override
	public List<String> getInputProperties(Class<?> projectionType) {
		return null;
	}

	@Override
	public ProjectionInformation getProjectionInformation(Class<?> projectionType) {
		return null;
	}
}
