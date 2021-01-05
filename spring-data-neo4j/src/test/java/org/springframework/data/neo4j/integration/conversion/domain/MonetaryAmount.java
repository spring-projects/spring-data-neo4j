/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.integration.conversion.domain;

public class MonetaryAmount {

	private final int amount;

	public MonetaryAmount(int pounds, int pence) {
		this.amount = pounds * 100 + pence;
	}

	public int getAmountAsSubUnits() {
		return this.amount;
	}

	@Override
	public int hashCode() {
		final int prime = 17;
		int result = 1;
		result = prime * result + (amount ^ (amount >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MonetaryAmount))
			return false;

		MonetaryAmount other = (MonetaryAmount) obj;
		return this.amount == other.amount;
	}

	@Override
	public String toString() {
		int pence = this.amount % 100;
		return "£" + (this.amount / 100) + "." + (pence < 10 ? "0" + pence : pence);
	}

}
