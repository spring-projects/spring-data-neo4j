/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
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
