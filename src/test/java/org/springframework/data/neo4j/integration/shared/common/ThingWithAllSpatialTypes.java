/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.types.CartesianPoint2d;
import org.springframework.data.neo4j.types.CartesianPoint3d;
import org.springframework.data.neo4j.types.GeographicPoint2d;
import org.springframework.data.neo4j.types.GeographicPoint3d;

/**
 * Contains properties of all spatial types.
 *
 * @author Michael J. Simons
 */
@Node("SpatialTypes")
public class ThingWithAllSpatialTypes {

	@Id
	@GeneratedValue
	public final Long id;

	private Point sdnPoint;

	private GeographicPoint2d geo2d;

	private GeographicPoint3d geo3d;

	private CartesianPoint2d car2d;

	private CartesianPoint3d car3d;

	private ThingWithAllSpatialTypes(Long id, Point sdnPoint, GeographicPoint2d geo2d, GeographicPoint3d geo3d, CartesianPoint2d car2d, CartesianPoint3d car3d) {
		this.id = id;
		this.sdnPoint = sdnPoint;
		this.geo2d = geo2d;
		this.geo3d = geo3d;
		this.car2d = car2d;
		this.car3d = car3d;
	}

	public static ThingWithAllSpatialTypesBuilder builder() {
		return new ThingWithAllSpatialTypesBuilder();
	}

	public Long getId() {
		return this.id;
	}

	public Point getSdnPoint() {
		return this.sdnPoint;
	}

	public GeographicPoint2d getGeo2d() {
		return this.geo2d;
	}

	public GeographicPoint3d getGeo3d() {
		return this.geo3d;
	}

	public CartesianPoint2d getCar2d() {
		return this.car2d;
	}

	public CartesianPoint3d getCar3d() {
		return this.car3d;
	}

	public void setSdnPoint(Point sdnPoint) {
		this.sdnPoint = sdnPoint;
	}

	public void setGeo2d(GeographicPoint2d geo2d) {
		this.geo2d = geo2d;
	}

	public void setGeo3d(GeographicPoint3d geo3d) {
		this.geo3d = geo3d;
	}

	public void setCar2d(CartesianPoint2d car2d) {
		this.car2d = car2d;
	}

	public void setCar3d(CartesianPoint3d car3d) {
		this.car3d = car3d;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof ThingWithAllSpatialTypes)) {
			return false;
		}
		final ThingWithAllSpatialTypes other = (ThingWithAllSpatialTypes) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
			return false;
		}
		final Object this$sdnPoint = this.getSdnPoint();
		final Object other$sdnPoint = other.getSdnPoint();
		if (this$sdnPoint == null ? other$sdnPoint != null : !this$sdnPoint.equals(other$sdnPoint)) {
			return false;
		}
		final Object this$geo2d = this.getGeo2d();
		final Object other$geo2d = other.getGeo2d();
		if (this$geo2d == null ? other$geo2d != null : !this$geo2d.equals(other$geo2d)) {
			return false;
		}
		final Object this$geo3d = this.getGeo3d();
		final Object other$geo3d = other.getGeo3d();
		if (this$geo3d == null ? other$geo3d != null : !this$geo3d.equals(other$geo3d)) {
			return false;
		}
		final Object this$car2d = this.getCar2d();
		final Object other$car2d = other.getCar2d();
		if (this$car2d == null ? other$car2d != null : !this$car2d.equals(other$car2d)) {
			return false;
		}
		final Object this$car3d = this.getCar3d();
		final Object other$car3d = other.getCar3d();
		if (this$car3d == null ? other$car3d != null : !this$car3d.equals(other$car3d)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof ThingWithAllSpatialTypes;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		final Object $sdnPoint = this.getSdnPoint();
		result = result * PRIME + ($sdnPoint == null ? 43 : $sdnPoint.hashCode());
		final Object $geo2d = this.getGeo2d();
		result = result * PRIME + ($geo2d == null ? 43 : $geo2d.hashCode());
		final Object $geo3d = this.getGeo3d();
		result = result * PRIME + ($geo3d == null ? 43 : $geo3d.hashCode());
		final Object $car2d = this.getCar2d();
		result = result * PRIME + ($car2d == null ? 43 : $car2d.hashCode());
		final Object $car3d = this.getCar3d();
		result = result * PRIME + ($car3d == null ? 43 : $car3d.hashCode());
		return result;
	}

	public String toString() {
		return "ThingWithAllSpatialTypes(id=" + this.getId() + ", sdnPoint=" + this.getSdnPoint() + ", geo2d=" + this.getGeo2d() + ", geo3d=" + this.getGeo3d() + ", car2d=" + this.getCar2d() + ", car3d=" + this.getCar3d() + ")";
	}

	public ThingWithAllSpatialTypes withId(Long newId) {
		return this.id == newId ? this : new ThingWithAllSpatialTypes(newId, this.sdnPoint, this.geo2d, this.geo3d, this.car2d, this.car3d);
	}

	/**
	 * the builder
	 */
	@SuppressWarnings("HiddenField")
	public static class ThingWithAllSpatialTypesBuilder {
		private Long id;
		private Point sdnPoint;
		private GeographicPoint2d geo2d;
		private GeographicPoint3d geo3d;
		private CartesianPoint2d car2d;
		private CartesianPoint3d car3d;

		ThingWithAllSpatialTypesBuilder() {
		}

		public ThingWithAllSpatialTypesBuilder id(Long id) {
			this.id = id;
			return this;
		}

		public ThingWithAllSpatialTypesBuilder sdnPoint(Point sdnPoint) {
			this.sdnPoint = sdnPoint;
			return this;
		}

		public ThingWithAllSpatialTypesBuilder geo2d(GeographicPoint2d geo2d) {
			this.geo2d = geo2d;
			return this;
		}

		public ThingWithAllSpatialTypesBuilder geo3d(GeographicPoint3d geo3d) {
			this.geo3d = geo3d;
			return this;
		}

		public ThingWithAllSpatialTypesBuilder car2d(CartesianPoint2d car2d) {
			this.car2d = car2d;
			return this;
		}

		public ThingWithAllSpatialTypesBuilder car3d(CartesianPoint3d car3d) {
			this.car3d = car3d;
			return this;
		}

		public ThingWithAllSpatialTypes build() {
			return new ThingWithAllSpatialTypes(this.id, this.sdnPoint, this.geo2d, this.geo3d, this.car2d, this.car3d);
		}

		public String toString() {
			return "ThingWithAllSpatialTypes.ThingWithAllSpatialTypesBuilder(id=" + this.id + ", sdnPoint=" + this.sdnPoint + ", geo2d=" + this.geo2d + ", geo3d=" + this.geo3d + ", car2d=" + this.car2d + ", car3d=" + this.car3d + ")";
		}
	}
}
