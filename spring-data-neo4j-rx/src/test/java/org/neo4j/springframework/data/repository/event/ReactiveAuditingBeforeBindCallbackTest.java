/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.repository.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.springframework.core.Ordered;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.mapping.context.PersistentEntities;

/**
 * @author Michael J. Simons
 */
class ReactiveAuditingBeforeBindCallbackTest {

	IsNewAwareAuditingHandler spyOnHandler;

	ReactiveAuditingBeforeBindCallback callback;

	@BeforeEach
	public void setUp() {

		Neo4jMappingContext mappingContext = new Neo4jMappingContext();
		mappingContext.setInitialEntitySet(new HashSet<>(Arrays.asList(Sample.class, ImmutableSample.class)));
		mappingContext.initialize();

		IsNewAwareAuditingHandler originalHandler = new IsNewAwareAuditingHandler(
			new PersistentEntities(Arrays.asList(mappingContext)));
		spyOnHandler = spy(originalHandler);
		callback = new ReactiveAuditingBeforeBindCallback(() -> spyOnHandler);
	}

	@Test
	void rejectsNullAuditingHandler() {

		assertThatIllegalArgumentException().isThrownBy(() -> new ReactiveAuditingBeforeBindCallback(null));
	}

	@Test
	void triggersCreationMarkForObjectWithEmptyId() {

		Sample sample = new Sample();
		StepVerifier
			.create(callback.onBeforeBind(sample))
			.expectNextMatches(s -> {
				Sample auditedObject = (Sample) s;
				return auditedObject.created != null && auditedObject.modified != null;
			})
			.verifyComplete();

		verify(spyOnHandler, times(1)).markCreated(sample);
		verify(spyOnHandler, times(0)).markModified(any());
	}

	@Test
	void triggersModificationMarkForObjectWithSetId() {

		Sample sample = new Sample();
		sample.id = "id";
		sample.version = 1L;

		StepVerifier
			.create(callback.onBeforeBind(sample))
			.expectNextMatches(s -> {
				Sample auditedObject = (Sample) s;
				return auditedObject.created == null && auditedObject.modified != null;
			})
			.verifyComplete();

		verify(spyOnHandler, times(0)).markCreated(any());
		verify(spyOnHandler, times(1)).markModified(sample);
	}

	@Test
	void hasExplicitOrder() {

		assertThat(callback).isInstanceOf(Ordered.class);
		assertThat(callback.getOrder()).isEqualTo(100);
	}

	@Test
	void propagatesChangedInstanceToEvent() {

		ImmutableSample sample = new ImmutableSample();

		ImmutableSample newSample = new ImmutableSample();
		IsNewAwareAuditingHandler handler = mock(IsNewAwareAuditingHandler.class);
		doReturn(newSample).when(handler).markAudited(eq(sample));

		ReactiveAuditingBeforeBindCallback localCallback = new ReactiveAuditingBeforeBindCallback(() -> handler);
		StepVerifier.create(localCallback.onBeforeBind(sample)).expectNext(newSample);
	}

}
