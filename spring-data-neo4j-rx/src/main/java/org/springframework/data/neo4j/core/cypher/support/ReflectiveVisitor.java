/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.springframework.data.neo4j.core.cypher.support;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a convenience class implementing a {@link Visitor} and it takes care of choosing the right methods
 * to dispatch the {@link Visitor#enter(Visitable)} and {@link Visitor#leave(Visitable)} calls to.
 * <p/>
 * Classes extending this visitor need to provide corresponding {@code enter} and {@code leave} methods taking exactly
 * one argument of the type of {@link Visitable} they are interested it.
 * <p/>
 * The type must be an exact match, this support class doesn't try to find a close match up in the class hierarchy if it
 * doesn't find an exact match.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
public abstract class ReflectiveVisitor implements Visitor {

	/**
	 * Private enum to specify a visiting phase.
	 */
	private enum Phase {
		ENTER("enter"),
		LEAVE("leave");

		final String methodName;

		Phase(String methodName) {
			this.methodName = methodName;
		}
	}

	private final Map<TargetAndPhase, Optional<MethodHandle>> cachedHandles = new ConcurrentHashMap<>();

	/**
	 * This is a hook that is called with the uncasted, raw visitable just before entering a visitable.
	 * <p/>
	 * The hook is called regardless wither a matching {@code enter} is found or not.
	 *
	 * @param visitable The visitable that is passed on to a matching enter after this call.
	 */
	protected abstract void preEnter(Visitable visitable);

	/**
	 * This is a hook that is called with the uncasted, raw visitable just after leaving the visitable.
	 * <p/>
	 * The hook is called regardless wither a matching {@code leave} is found or not.
	 *
	 * @param visitable
	 */
	protected abstract void postLeave(Visitable visitable);

	@Override
	public final void enter(Visitable visitable) {
		preEnter(visitable);
		executeConcreteMethodIn(new TargetAndPhase(visitable.getClass(), Phase.ENTER), visitable);
	}

	@Override
	public final void leave(Visitable visitable) {
		executeConcreteMethodIn(new TargetAndPhase(visitable.getClass(), Phase.LEAVE), visitable);
		postLeave(visitable);
	}

	private void executeConcreteMethodIn(TargetAndPhase targetAndPhase, Visitable onVisitable) {
		Optional<MethodHandle> optionalHandle = this.cachedHandles.computeIfAbsent(targetAndPhase, this::findHandleFor);
		optionalHandle.ifPresent(handle -> {
			try {
				handle.invoke(onVisitable);
			} catch (Throwable throwable) {
			}
		});
	}

	private Optional<MethodHandle> findHandleFor(TargetAndPhase targetAndPhase) {

		try {
			// Using MethodHandles.lookup().findVirtual() doesn't allow to make a protected method accessible.
			Method method = this.getClass()
				.getDeclaredMethod(targetAndPhase.phase.methodName, targetAndPhase.classOfVisitable);
			method.setAccessible(true);
			return Optional.of(MethodHandles.lookup().in(this.getClass()).unreflect(method).bindTo(this));
		} catch (IllegalAccessException | NoSuchMethodException e) {
			// We don't do anything if the method doesn't exists
			return Optional.empty();
		}
	}

	@RequiredArgsConstructor
	@EqualsAndHashCode
	private static class TargetAndPhase {
		private final Class<? extends Visitable> classOfVisitable;

		private final Phase phase;
	}
}
