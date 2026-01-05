/*
 * Copyright 2011-present the original author or authors.
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

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gerrit Meier
 */
@SuppressWarnings("HiddenField")
public class SameIdProperty {
	/**
	 * @author Gerrit Meier
	 */
	@Node("Pod")
	public static class PodEntity {
		@Id
		private String code;

		private PodEntity(String code) {
			this.code = code;
		}

		public PodEntity() {
		}

		public String getCode() {
			return this.code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof PodEntity)) {
				return false;
			}
			final PodEntity other = (PodEntity) o;
			if (!other.canEqual((Object) this)) {
				return false;
			}
			final Object this$code = this.getCode();
			final Object other$code = other.getCode();
			if (this$code == null ? other$code != null : !this$code.equals(other$code)) {
				return false;
			}
			return true;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof PodEntity;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $code = this.getCode();
			result = result * PRIME + ($code == null ? 43 : $code.hashCode());
			return result;
		}

		public String toString() {
			return "SameIdProperty.PodEntity(code=" + this.getCode() + ")";
		}

		public PodEntity withCode(String code) {
			return this.code == code ? this : new PodEntity(code);
		}
	}

	/**
	 * @author Gerrit Meier
	 */
	@Node("Pol")
	public static class PolEntity {
		@Id
		private String code;

		@Relationship(type = "ROUTES")
		private List<PodEntity> routes = new ArrayList<>();

		private PolEntity(String code, List<PodEntity> routes) {
			this.code = code;
			this.routes = routes;
		}

		public PolEntity() {
		}

		public String getCode() {
			return this.code;
		}

		public List<PodEntity> getRoutes() {
			return this.routes;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public void setRoutes(List<PodEntity> routes) {
			this.routes = routes;
		}

		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof PolEntity)) {
				return false;
			}
			final PolEntity other = (PolEntity) o;
			if (!other.canEqual((Object) this)) {
				return false;
			}
			final Object this$code = this.getCode();
			final Object other$code = other.getCode();
			if (this$code == null ? other$code != null : !this$code.equals(other$code)) {
				return false;
			}
			final Object this$routes = this.getRoutes();
			final Object other$routes = other.getRoutes();
			if (this$routes == null ? other$routes != null : !this$routes.equals(other$routes)) {
				return false;
			}
			return true;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof PolEntity;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $code = this.getCode();
			result = result * PRIME + ($code == null ? 43 : $code.hashCode());
			final Object $routes = this.getRoutes();
			result = result * PRIME + ($routes == null ? 43 : $routes.hashCode());
			return result;
		}

		public String toString() {
			return "SameIdProperty.PolEntity(code=" + this.getCode() + ", routes=" + this.getRoutes() + ")";
		}

		public PolEntity withCode(String code) {
			return this.code == code ? this : new PolEntity(code, this.routes);
		}

		public PolEntity withRoutes(List<PodEntity> routes) {
			return this.routes == routes ? this : new PolEntity(this.code, routes);
		}
	}

	/**
	 * @author Gerrit Meier
	 */
	@Node("PolWithRP")
	public static class PolEntityWithRelationshipProperties {
		@Id
		private String code;

		@Relationship(type = "ROUTES")
		private List<RouteProperties> routes = new ArrayList<>();

		private PolEntityWithRelationshipProperties(String code, List<RouteProperties> routes) {
			this.code = code;
			this.routes = routes;
		}

		public PolEntityWithRelationshipProperties() {
		}

		public String getCode() {
			return this.code;
		}

		public List<RouteProperties> getRoutes() {
			return this.routes;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public void setRoutes(List<RouteProperties> routes) {
			this.routes = routes;
		}

		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof PolEntityWithRelationshipProperties)) {
				return false;
			}
			final PolEntityWithRelationshipProperties other = (PolEntityWithRelationshipProperties) o;
			if (!other.canEqual((Object) this)) {
				return false;
			}
			final Object this$code = this.getCode();
			final Object other$code = other.getCode();
			if (this$code == null ? other$code != null : !this$code.equals(other$code)) {
				return false;
			}
			final Object this$routes = this.getRoutes();
			final Object other$routes = other.getRoutes();
			if (this$routes == null ? other$routes != null : !this$routes.equals(other$routes)) {
				return false;
			}
			return true;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof PolEntityWithRelationshipProperties;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $code = this.getCode();
			result = result * PRIME + ($code == null ? 43 : $code.hashCode());
			final Object $routes = this.getRoutes();
			result = result * PRIME + ($routes == null ? 43 : $routes.hashCode());
			return result;
		}

		public String toString() {
			return "SameIdProperty.PolEntityWithRelationshipProperties(code=" + this.getCode() + ", routes=" + this.getRoutes() + ")";
		}

		public PolEntityWithRelationshipProperties withCode(String code) {
			return this.code == code ? this : new PolEntityWithRelationshipProperties(code, this.routes);
		}

		public PolEntityWithRelationshipProperties withRoutes(List<RouteProperties> routes) {
			return this.routes == routes ? this : new PolEntityWithRelationshipProperties(this.code, routes);
		}
	}

	/**
	 * @author Gerrit Meier
	 */
	@RelationshipProperties
	public static class RouteProperties {

		@RelationshipId
		private Long id;

		private Double truck;
		private String truckCurrency;

		private Double ft20;
		private String ft20Currency;

		private Double ft40;
		private String ft40Currency;

		private Double ft40HC;
		private String ft40HCCurrency;


		@TargetNode
		private PodEntity pod;

		private RouteProperties(Long id, Double truck, String truckCurrency, Double ft20, String ft20Currency, Double ft40, String ft40Currency, Double ft40HC, String ft40HCCurrency, PodEntity pod) {
			this.id = id;
			this.truck = truck;
			this.truckCurrency = truckCurrency;
			this.ft20 = ft20;
			this.ft20Currency = ft20Currency;
			this.ft40 = ft40;
			this.ft40Currency = ft40Currency;
			this.ft40HC = ft40HC;
			this.ft40HCCurrency = ft40HCCurrency;
			this.pod = pod;
		}

		public RouteProperties() {
		}

		public Long getId() {
			return this.id;
		}

		public Double getTruck() {
			return this.truck;
		}

		public String getTruckCurrency() {
			return this.truckCurrency;
		}

		public Double getFt20() {
			return this.ft20;
		}

		public String getFt20Currency() {
			return this.ft20Currency;
		}

		public Double getFt40() {
			return this.ft40;
		}

		public String getFt40Currency() {
			return this.ft40Currency;
		}

		public Double getFt40HC() {
			return this.ft40HC;
		}

		public String getFt40HCCurrency() {
			return this.ft40HCCurrency;
		}

		public PodEntity getPod() {
			return this.pod;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setTruck(Double truck) {
			this.truck = truck;
		}

		public void setTruckCurrency(String truckCurrency) {
			this.truckCurrency = truckCurrency;
		}

		public void setFt20(Double ft20) {
			this.ft20 = ft20;
		}

		public void setFt20Currency(String ft20Currency) {
			this.ft20Currency = ft20Currency;
		}

		public void setFt40(Double ft40) {
			this.ft40 = ft40;
		}

		public void setFt40Currency(String ft40Currency) {
			this.ft40Currency = ft40Currency;
		}

		public void setFt40HC(Double ft40HC) {
			this.ft40HC = ft40HC;
		}

		public void setFt40HCCurrency(String ft40HCCurrency) {
			this.ft40HCCurrency = ft40HCCurrency;
		}

		public void setPod(PodEntity pod) {
			this.pod = pod;
		}

		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof RouteProperties)) {
				return false;
			}
			final RouteProperties other = (RouteProperties) o;
			if (!other.canEqual((Object) this)) {
				return false;
			}
			final Object this$id = this.getId();
			final Object other$id = other.getId();
			if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
				return false;
			}
			final Object this$truck = this.getTruck();
			final Object other$truck = other.getTruck();
			if (this$truck == null ? other$truck != null : !this$truck.equals(other$truck)) {
				return false;
			}
			final Object this$truckCurrency = this.getTruckCurrency();
			final Object other$truckCurrency = other.getTruckCurrency();
			if (this$truckCurrency == null ? other$truckCurrency != null : !this$truckCurrency.equals(other$truckCurrency)) {
				return false;
			}
			final Object this$ft20 = this.getFt20();
			final Object other$ft20 = other.getFt20();
			if (this$ft20 == null ? other$ft20 != null : !this$ft20.equals(other$ft20)) {
				return false;
			}
			final Object this$ft20Currency = this.getFt20Currency();
			final Object other$ft20Currency = other.getFt20Currency();
			if (this$ft20Currency == null ? other$ft20Currency != null : !this$ft20Currency.equals(other$ft20Currency)) {
				return false;
			}
			final Object this$ft40 = this.getFt40();
			final Object other$ft40 = other.getFt40();
			if (this$ft40 == null ? other$ft40 != null : !this$ft40.equals(other$ft40)) {
				return false;
			}
			final Object this$ft40Currency = this.getFt40Currency();
			final Object other$ft40Currency = other.getFt40Currency();
			if (this$ft40Currency == null ? other$ft40Currency != null : !this$ft40Currency.equals(other$ft40Currency)) {
				return false;
			}
			final Object this$ft40HC = this.getFt40HC();
			final Object other$ft40HC = other.getFt40HC();
			if (this$ft40HC == null ? other$ft40HC != null : !this$ft40HC.equals(other$ft40HC)) {
				return false;
			}
			final Object this$ft40HCCurrency = this.getFt40HCCurrency();
			final Object other$ft40HCCurrency = other.getFt40HCCurrency();
			if (this$ft40HCCurrency == null ? other$ft40HCCurrency != null : !this$ft40HCCurrency.equals(other$ft40HCCurrency)) {
				return false;
			}
			final Object this$pod = this.getPod();
			final Object other$pod = other.getPod();
			if (this$pod == null ? other$pod != null : !this$pod.equals(other$pod)) {
				return false;
			}
			return true;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof RouteProperties;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $id = this.getId();
			result = result * PRIME + ($id == null ? 43 : $id.hashCode());
			final Object $truck = this.getTruck();
			result = result * PRIME + ($truck == null ? 43 : $truck.hashCode());
			final Object $truckCurrency = this.getTruckCurrency();
			result = result * PRIME + ($truckCurrency == null ? 43 : $truckCurrency.hashCode());
			final Object $ft20 = this.getFt20();
			result = result * PRIME + ($ft20 == null ? 43 : $ft20.hashCode());
			final Object $ft20Currency = this.getFt20Currency();
			result = result * PRIME + ($ft20Currency == null ? 43 : $ft20Currency.hashCode());
			final Object $ft40 = this.getFt40();
			result = result * PRIME + ($ft40 == null ? 43 : $ft40.hashCode());
			final Object $ft40Currency = this.getFt40Currency();
			result = result * PRIME + ($ft40Currency == null ? 43 : $ft40Currency.hashCode());
			final Object $ft40HC = this.getFt40HC();
			result = result * PRIME + ($ft40HC == null ? 43 : $ft40HC.hashCode());
			final Object $ft40HCCurrency = this.getFt40HCCurrency();
			result = result * PRIME + ($ft40HCCurrency == null ? 43 : $ft40HCCurrency.hashCode());
			final Object $pod = this.getPod();
			result = result * PRIME + ($pod == null ? 43 : $pod.hashCode());
			return result;
		}

		public String toString() {
			return "SameIdProperty.RouteProperties(id=" + this.getId() + ", truck=" + this.getTruck() + ", truckCurrency=" + this.getTruckCurrency() + ", ft20=" + this.getFt20() + ", ft20Currency=" + this.getFt20Currency() + ", ft40=" + this.getFt40() + ", ft40Currency=" + this.getFt40Currency() + ", ft40HC=" + this.getFt40HC() + ", ft40HCCurrency=" + this.getFt40HCCurrency() + ", pod=" + this.getPod() + ")";
		}

		public RouteProperties withId(Long id) {
			return this.id == id ? this : new RouteProperties(id, this.truck, this.truckCurrency, this.ft20, this.ft20Currency, this.ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod);
		}

		public RouteProperties withTruck(Double truck) {
			return this.truck == truck ? this : new RouteProperties(this.id, truck, this.truckCurrency, this.ft20, this.ft20Currency, this.ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod);
		}

		public RouteProperties withTruckCurrency(String truckCurrency) {
			return this.truckCurrency == truckCurrency ? this : new RouteProperties(this.id, this.truck, truckCurrency, this.ft20, this.ft20Currency, this.ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod);
		}

		public RouteProperties withFt20(Double ft20) {
			return this.ft20 == ft20 ? this : new RouteProperties(this.id, this.truck, this.truckCurrency, ft20, this.ft20Currency, this.ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod);
		}

		public RouteProperties withFt20Currency(String ft20Currency) {
			return this.ft20Currency == ft20Currency ? this : new RouteProperties(this.id, this.truck, this.truckCurrency, this.ft20, ft20Currency, this.ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod);
		}

		public RouteProperties withFt40(Double ft40) {
			return this.ft40 == ft40 ? this : new RouteProperties(this.id, this.truck, this.truckCurrency, this.ft20, this.ft20Currency, ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod);
		}

		public RouteProperties withFt40Currency(String ft40Currency) {
			return this.ft40Currency == ft40Currency ? this : new RouteProperties(this.id, this.truck, this.truckCurrency, this.ft20, this.ft20Currency, this.ft40, ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod);
		}

		public RouteProperties withFt40HC(Double ft40HC) {
			return this.ft40HC == ft40HC ? this : new RouteProperties(this.id, this.truck, this.truckCurrency, this.ft20, this.ft20Currency, this.ft40, this.ft40Currency, ft40HC, this.ft40HCCurrency, this.pod);
		}

		public RouteProperties withFt40HCCurrency(String ft40HCCurrency) {
			return this.ft40HCCurrency == ft40HCCurrency ? this : new RouteProperties(this.id, this.truck, this.truckCurrency, this.ft20, this.ft20Currency, this.ft40, this.ft40Currency, this.ft40HC, ft40HCCurrency, this.pod);
		}

		public RouteProperties withPod(PodEntity pod) {
			return this.pod == pod ? this : new RouteProperties(this.id, this.truck, this.truckCurrency, this.ft20, this.ft20Currency, this.ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, pod);
		}
	}
}
