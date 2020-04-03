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
package org.neo4j.springframework.data.core.cypher;

import static org.apiguardian.api.API.Status.*;

import java.util.ArrayList;
import java.util.List;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.neo4j.springframework.data.core.cypher.support.Visitor;
import org.springframework.lang.Nullable;

/**
 * See <a href="https://s3.amazonaws.com/artifacts.opencypher.org/railroad/CaseExpression.html">CaseExpression</a>.
 *
 * @author Gerrit Meier
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public abstract class Case implements Visitable {

	private CaseElse caseElse;
	private final List<CaseWhenThen> caseWhenThens;

	static Case create(Expression caseExpression) {
		return new SimpleCase(caseExpression, new ArrayList<>());
	}

	static Case create() {
		return new GenericCase(new ArrayList<>());
	}

	protected Case(List<CaseWhenThen> caseWhenThens) {
		this.caseWhenThens = caseWhenThens;
	}

	@Nullable
	abstract Expression getCaseExpression();

	void setCaseElse(CaseElse caseElse) {
		this.caseElse = caseElse;
	}

	List<CaseWhenThen> getCaseWhenThens() {
		return caseWhenThens;
	}

	public OngoingWhenThen when(Expression whenExpression) {
		return new OngoingWhenThen(whenExpression);
	}

	/**
	 * Specification for a renderable, complete CASE statement
	 */
	@API(status = EXPERIMENTAL, since = "1.0")
	public interface CaseEnding extends Condition {
		OngoingWhenThen when(Expression whenExpression);

		CaseEnding elseDefault(Expression elseExpression);

	}

	/**
	 * Special implementation of the {@link Case} class to support simple case with an initial expression / condition.
	 */
	@API(status = EXPERIMENTAL, since = "1.0")
	public static class SimpleCase extends Case {

		private final Expression caseExpression;

		private SimpleCase(Expression caseExpression, List<CaseWhenThen> caseWhenThens) {
			super(caseWhenThens);
			this.caseExpression = caseExpression;
		}

		@Override
		Expression getCaseExpression() {
			return caseExpression;
		}

		/**
		 * The renderable implementation of {@link SimpleCase}.
		 */
		public final static class EndingSimpleCase extends SimpleCase implements CaseEnding {

			private EndingSimpleCase(Expression caseExpression, List<CaseWhenThen> caseWhenThens) {
				super(caseExpression, caseWhenThens);
			}

			public CaseEnding elseDefault(Expression elseExpression) {
				this.setCaseElse(new CaseElse(elseExpression));
				return this;
			}
		}
	}

	/**
	 * Implementation of the {@link Case} class to support generic case.
	 */
	@API(status = EXPERIMENTAL, since = "1.0")
	public static class GenericCase extends Case {

		private GenericCase(List<CaseWhenThen> caseWhenThens) {
			super(caseWhenThens);
		}

		@Override
		Expression getCaseExpression() {
			return null;
		}

		/**
		 * The renderable implementation of {@link GenericCase}.
		 */
		public final static class EndingGenericCase extends GenericCase implements CaseEnding {

			private EndingGenericCase(List<CaseWhenThen> caseWhenThens) {
				super(caseWhenThens);
			}
			public CaseEnding elseDefault(Expression elseExpression) {
				this.setCaseElse(new CaseElse(elseExpression));
				return this;
			}
		}
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.enter(this);
		if (getCaseExpression() != null) {
			getCaseExpression().accept(visitor);
		}

		getCaseWhenThens().forEach(caseWhenThen -> caseWhenThen.accept(visitor));

		if (caseElse != null) {
			caseElse.accept(visitor);
		}

		visitor.leave(this);
	}

	/**
	 * Helper class to collect `when` expressions and create {@link CaseWhenThen} instances when the `then` is provided.
	 */
	@API(status = INTERNAL, since = "1.0")
	public final class OngoingWhenThen {

		final Expression whenExpression;

		private OngoingWhenThen(Expression whenExpression) {
			this.whenExpression = whenExpression;
		}

		public CaseEnding then(Expression thenExpression) {
			CaseWhenThen caseWhenThen = new CaseWhenThen(whenExpression, thenExpression);
			getCaseWhenThens().add(caseWhenThen);
			if (getCaseExpression() != null) {
				return new SimpleCase.EndingSimpleCase(Case.this.getCaseExpression(), getCaseWhenThens());
			} else {
				return new GenericCase.EndingGenericCase(getCaseWhenThens());
			}
		}

	}

	/**
	 * Represents a pair of `when-then` expressions.
	 */
	@API(status = INTERNAL, since = "1.0")
	public final class CaseWhenThen implements Visitable {

		private final Expression whenExpression;
		private final Expression thenExpression;

		private CaseWhenThen(Expression whenExpression, Expression thenExpression) {

			this.whenExpression = whenExpression;
			this.thenExpression = thenExpression;
		}

		public OngoingWhenThen when(Expression nextWhenExpression) {
			return new OngoingWhenThen(nextWhenExpression);
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.enter(this);
			whenExpression.accept(visitor);
			visitor.leave(this);
			thenExpression.accept(visitor);
		}

	}

	/**
	 * Represents a finalizing `else` expression.
	 */
	@API(status = INTERNAL, since = "1.0")
	public final class CaseElse implements Visitable {
		private final Expression elseExpression;

		private CaseElse(Expression elseExpression) {
			this.elseExpression = elseExpression;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.enter(this);
			elseExpression.accept(visitor);
			visitor.leave(this);
		}
	}

}
