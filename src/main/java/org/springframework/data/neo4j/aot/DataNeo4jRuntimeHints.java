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
package org.springframework.data.neo4j.aot;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.data.neo4j.core.mapping.callback.AfterConvertCallback;
import org.springframework.data.neo4j.core.mapping.callback.BeforeBindCallback;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;
import org.springframework.data.neo4j.repository.query.SimpleQueryByExampleExecutor;
import org.springframework.data.neo4j.repository.support.SimpleNeo4jRepository;
import org.springframework.lang.Nullable;

import java.util.Arrays;

/**
 * @author Gerrit Meier
 */
public class DataNeo4jRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {

		hints.reflection().registerTypes(
				Arrays.asList(
						TypeReference.of(SimpleNeo4jRepository.class),
						TypeReference.of(SimpleQueryByExampleExecutor.class),
						TypeReference.of(BeforeBindCallback.class),
						TypeReference.of(AfterConvertCallback.class),
						// todo "temporary" fix, should get resolved when defined in @GeneratedValue
						TypeReference.of(UUIDStringGenerator.class),
						TypeReference.of(GeneratedValue.InternalIdGenerator.class),
						TypeReference.of(GeneratedValue.UUIDGenerator.class)
				),
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_PUBLIC_METHODS));
	}
}
