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


package org.springframework.data.neo4j.examples.restaurants.domain;

import org.neo4j.ogm.annotation.GraphId;
import org.springframework.data.geo.Point;

public class Restaurant {

	@GraphId
	private Long id;
	private String name;
	private Double latitude;
	private Double longitude;
	private int zip;

	private transient Point location;

	public Restaurant() {
	}

	public Restaurant(String name, Double latitude, Double longitude, int zip) {
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.zip = zip;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public int getZip() {
		return zip;
	}

	public void setZip(int zip) {
		this.zip = zip;
	}
}
