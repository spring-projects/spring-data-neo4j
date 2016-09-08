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
package org.springframework.data.neo4j.domain.sample;

/**
 * @author Mark Angrish
 */
public class Address {

	private String country;
	private String city;
	private String streetName;
	private String streetNo;

	public Address() {}

	public Address(String country, String city, String streetName, String streetNo) {
		this.country = country;
		this.city = city;
		this.streetName = streetName;
		this.streetNo = streetNo;
	}

	public String getCountry() {
		return country;
	}

	public String getCity() {
		return city;
	}

	public String getStreetName() {
		return streetName;
	}

	public String getStreetNo() {
		return streetNo;
	}
}
