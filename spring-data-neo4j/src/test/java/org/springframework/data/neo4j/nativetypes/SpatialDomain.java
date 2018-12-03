/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.nativetypes;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.types.spatial.CartesianPoint2d;
import org.neo4j.ogm.types.spatial.CartesianPoint3d;
import org.neo4j.ogm.types.spatial.GeographicPoint2d;
import org.neo4j.ogm.types.spatial.GeographicPoint3d;

@NodeEntity
public class SpatialDomain {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private GeographicPoint2d geographicPoint2d;

	private GeographicPoint3d geographicPoint3d;

	private CartesianPoint2d cartesianPoint2d;

	private CartesianPoint3d cartesianPoint3d;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public GeographicPoint2d getGeographicPoint2d() {
		return geographicPoint2d;
	}

	public void setGeographicPoint2d(GeographicPoint2d geographicPoint2d) {
		this.geographicPoint2d = geographicPoint2d;
	}

	public GeographicPoint3d getGeographicPoint3d() {
		return geographicPoint3d;
	}

	public void setGeographicPoint3d(GeographicPoint3d geographicPoint3d) {
		this.geographicPoint3d = geographicPoint3d;
	}

	public CartesianPoint2d getCartesianPoint2d() {
		return cartesianPoint2d;
	}

	public void setCartesianPoint2d(CartesianPoint2d cartesianPoint2d) {
		this.cartesianPoint2d = cartesianPoint2d;
	}

	public CartesianPoint3d getCartesianPoint3d() {
		return cartesianPoint3d;
	}

	public void setCartesianPoint3d(CartesianPoint3d cartesianPoint3d) {
		this.cartesianPoint3d = cartesianPoint3d;
	}
}
