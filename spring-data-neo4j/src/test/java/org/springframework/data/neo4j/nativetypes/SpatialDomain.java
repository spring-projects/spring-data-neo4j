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
package org.springframework.data.neo4j.nativetypes;

import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.types.spatial.CartesianPoint2d;
import org.neo4j.ogm.types.spatial.CartesianPoint3d;
import org.neo4j.ogm.types.spatial.GeographicPoint2d;
import org.neo4j.ogm.types.spatial.GeographicPoint3d;
import org.springframework.data.geo.Point;

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

	private Point sdnPoint;

	private List<Point> sdnPoints;

	private Point[] morePoints;

	public Long getId() {
		return id;
	}

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

	public Point getSdnPoint() {
		return sdnPoint;
	}

	public void setSdnPoint(Point sdnPoint) {
		this.sdnPoint = sdnPoint;
	}

	public List<Point> getSdnPoints() {
		return sdnPoints;
	}

	public void setSdnPoints(List<Point> sdnPoints) {
		this.sdnPoints = sdnPoints;
	}

	public Point[] getMorePoints() {
		return morePoints;
	}

	public void setMorePoints(Point[] morePoints) {
		this.morePoints = morePoints;
	}
}
