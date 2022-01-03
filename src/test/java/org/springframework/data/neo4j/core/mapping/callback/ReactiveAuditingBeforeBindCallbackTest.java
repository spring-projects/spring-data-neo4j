/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.core.mapping.callback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.data.auditing.ReactiveIsNewAwareAuditingHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;

/**
 * @author Michael J. Simons
 */
class ReactiveAuditingBeforeBindCallbackTest {

	ReactiveIsNewAwareAuditingHandler spyOnHandler;

	ReactiveAuditingBeforeBindCallback callback;

	@BeforeEach
	public void setUp() {

		Neo4jMappingContext mappingContext = new Neo4jMappingContext();
		mappingContext.setInitialEntitySet(new HashSet<>(Arrays.asList(Sample.class, ImmutableSample.class)));
		mappingContext.initialize();

		ReactiveIsNewAwareAuditingHandler originalHandler = new ReactiveIsNewAwareAuditingHandler(
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
		StepVerifier.create(callback.onBeforeBind(sample)).expectNextMatches(s -> {
			Sample auditedObject = (Sample) s;
			return auditedObject.created != null && auditedObject.modified != null;
		}).verifyComplete();

		verify(spyOnHandler, times(1)).markCreated(sample);
		verify(spyOnHandler, times(0)).markModified(any());
	}

	@Test
	void triggersModificationMarkForObjectWithSetId() {

		Sample sample = new Sample();
		sample.id = "id";
		sample.version = 1L;

		StepVerifier.create(callback.onBeforeBind(sample)).expectNextMatches(s -> {
			Sample auditedObject = (Sample) s;
			return auditedObject.created == null && auditedObject.modified != null;
		}).verifyComplete();

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
		ReactiveIsNewAwareAuditingHandler handler = mock(ReactiveIsNewAwareAuditingHandler.class);
		doReturn(Mono.just(newSample)).when(handler).markAudited(eq(sample));

		ReactiveAuditingBeforeBindCallback localCallback = new ReactiveAuditingBeforeBindCallback(() -> handler);
		StepVerifier.create(localCallback.onBeforeBind(sample)).expectNext(newSample);
	}

}
