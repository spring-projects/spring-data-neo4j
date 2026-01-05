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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

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

		protected boolean canEqual(final Object other) {
			return other instanceof PodEntity;
		}

		public PodEntity withCode(String code) {
			return Objects.equals(this.code, code) ? this : new PodEntity(code);
		}

		@Override
		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof PodEntity other)) {
				return false;
			}
			if (!other.canEqual(this)) {
				return false;
			}
			final Object this$code = this.getCode();
			final Object other$code = other.getCode();
			return Objects.equals(this$code, other$code);
		}

		@Override
		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $code = this.getCode();
			result = result * PRIME + (($code != null) ? $code.hashCode() : 43);
			return result;
		}

		@Override
		public String toString() {
			return "SameIdProperty.PodEntity(code=" + this.getCode() + ")";
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

		public void setCode(String code) {
			this.code = code;
		}

		public List<PodEntity> getRoutes() {
			return this.routes;
		}

		public void setRoutes(List<PodEntity> routes) {
			this.routes = routes;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof PolEntity;
		}

		public PolEntity withCode(String code) {
			return (this.code != code) ? new PolEntity(code, this.routes) : this;
		}

		public PolEntity withRoutes(List<PodEntity> routes) {
			return Objects.equals(this.routes, routes) ? this : new PolEntity(this.code, routes);
		}

		@Override
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
			if (!Objects.equals(this$code, other$code)) {
				return false;
			}
			final Object this$routes = this.getRoutes();
			final Object other$routes = other.getRoutes();
			return Objects.equals(this$routes, other$routes);
		}

		@Override
		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $code = this.getCode();
			result = result * PRIME + (($code != null) ? $code.hashCode() : 43);
			final Object $routes = this.getRoutes();
			result = result * PRIME + (($routes != null) ? $routes.hashCode() : 43);
			return result;
		}

		@Override
		public String toString() {
			return "SameIdProperty.PolEntity(code=" + this.getCode() + ", routes=" + this.getRoutes() + ")";
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

		public void setCode(String code) {
			this.code = code;
		}

		public List<RouteProperties> getRoutes() {
			return this.routes;
		}

		public void setRoutes(List<RouteProperties> routes) {
			this.routes = routes;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof PolEntityWithRelationshipProperties;
		}

		public PolEntityWithRelationshipProperties withCode(String code) {
			return (this.code != code) ? new PolEntityWithRelationshipProperties(code, this.routes) : this;
		}

		public PolEntityWithRelationshipProperties withRoutes(List<RouteProperties> routes) {
			return (this.routes != routes) ? new PolEntityWithRelationshipProperties(this.code, routes) : this;
		}

		@Override
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
			if (!Objects.equals(this$code, other$code)) {
				return false;
			}
			final Object this$routes = this.getRoutes();
			final Object other$routes = other.getRoutes();
			return Objects.equals(this$routes, other$routes);
		}

		@Override
		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $code = this.getCode();
			result = result * PRIME + (($code != null) ? $code.hashCode() : 43);
			final Object $routes = this.getRoutes();
			result = result * PRIME + (($routes != null) ? $routes.hashCode() : 43);
			return result;
		}

		@Override
		public String toString() {
			return "SameIdProperty.PolEntityWithRelationshipProperties(code=" + this.getCode() + ", routes="
					+ this.getRoutes() + ")";
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

		private RouteProperties(Long id, Double truck, String truckCurrency, Double ft20, String ft20Currency,
				Double ft40, String ft40Currency, Double ft40HC, String ft40HCCurrency, PodEntity pod) {
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

		public void setId(Long id) {
			this.id = id;
		}

		public Double getTruck() {
			return this.truck;
		}

		public void setTruck(Double truck) {
			this.truck = truck;
		}

		public String getTruckCurrency() {
			return this.truckCurrency;
		}

		public void setTruckCurrency(String truckCurrency) {
			this.truckCurrency = truckCurrency;
		}

		public Double getFt20() {
			return this.ft20;
		}

		public void setFt20(Double ft20) {
			this.ft20 = ft20;
		}

		public String getFt20Currency() {
			return this.ft20Currency;
		}

		public void setFt20Currency(String ft20Currency) {
			this.ft20Currency = ft20Currency;
		}

		public Double getFt40() {
			return this.ft40;
		}

		public void setFt40(Double ft40) {
			this.ft40 = ft40;
		}

		public String getFt40Currency() {
			return this.ft40Currency;
		}

		public void setFt40Currency(String ft40Currency) {
			this.ft40Currency = ft40Currency;
		}

		public Double getFt40HC() {
			return this.ft40HC;
		}

		public void setFt40HC(Double ft40HC) {
			this.ft40HC = ft40HC;
		}

		public String getFt40HCCurrency() {
			return this.ft40HCCurrency;
		}

		public void setFt40HCCurrency(String ft40HCCurrency) {
			this.ft40HCCurrency = ft40HCCurrency;
		}

		public PodEntity getPod() {
			return this.pod;
		}

		public void setPod(PodEntity pod) {
			this.pod = pod;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof RouteProperties;
		}

		public RouteProperties withId(Long id) {
			return (this.id != id) ? new RouteProperties(id, this.truck, this.truckCurrency, this.ft20,
					this.ft20Currency, this.ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod) : this;
		}

		public RouteProperties withTruck(Double truck) {
			return (this.truck != truck) ? new RouteProperties(this.id, truck, this.truckCurrency, this.ft20,
					this.ft20Currency, this.ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod) : this;
		}

		public RouteProperties withTruckCurrency(String truckCurrency) {
			return Objects.equals(this.truckCurrency, truckCurrency) ? this
					: new RouteProperties(this.id, this.truck, truckCurrency, this.ft20, this.ft20Currency, this.ft40,
							this.ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod);
		}

		public RouteProperties withFt20(Double ft20) {
			return (this.ft20 != ft20) ? new RouteProperties(this.id, this.truck, this.truckCurrency, ft20,
					this.ft20Currency, this.ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod) : this;
		}

		public RouteProperties withFt20Currency(String ft20Currency) {
			return (this.ft20Currency != ft20Currency) ? new RouteProperties(this.id, this.truck, this.truckCurrency,
					this.ft20, ft20Currency, this.ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod)
					: this;
		}

		public RouteProperties withFt40(Double ft40) {
			return (this.ft40 != ft40) ? new RouteProperties(this.id, this.truck, this.truckCurrency, this.ft20,
					this.ft20Currency, ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod) : this;
		}

		public RouteProperties withFt40Currency(String ft40Currency) {
			return (this.ft40Currency != ft40Currency) ? new RouteProperties(this.id, this.truck, this.truckCurrency,
					this.ft20, this.ft20Currency, this.ft40, ft40Currency, this.ft40HC, this.ft40HCCurrency, this.pod)
					: this;
		}

		public RouteProperties withFt40HC(Double ft40HC) {
			return Objects.equals(this.ft40HC, ft40HC) ? this
					: new RouteProperties(this.id, this.truck, this.truckCurrency, this.ft20, this.ft20Currency,
							this.ft40, this.ft40Currency, ft40HC, this.ft40HCCurrency, this.pod);
		}

		public RouteProperties withFt40HCCurrency(String ft40HCCurrency) {
			return (this.ft40HCCurrency != ft40HCCurrency)
					? new RouteProperties(this.id, this.truck, this.truckCurrency, this.ft20, this.ft20Currency,
							this.ft40, this.ft40Currency, this.ft40HC, ft40HCCurrency, this.pod)
					: this;
		}

		public RouteProperties withPod(PodEntity pod) {
			return (this.pod != pod) ? new RouteProperties(this.id, this.truck, this.truckCurrency, this.ft20,
					this.ft20Currency, this.ft40, this.ft40Currency, this.ft40HC, this.ft40HCCurrency, pod) : this;
		}

		@Override
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
			if (!Objects.equals(this$id, other$id)) {
				return false;
			}
			final Object this$truck = this.getTruck();
			final Object other$truck = other.getTruck();
			if (!Objects.equals(this$truck, other$truck)) {
				return false;
			}
			final Object this$truckCurrency = this.getTruckCurrency();
			final Object other$truckCurrency = other.getTruckCurrency();
			if (!Objects.equals(this$truckCurrency, other$truckCurrency)) {
				return false;
			}
			final Object this$ft20 = this.getFt20();
			final Object other$ft20 = other.getFt20();
			if (!Objects.equals(this$ft20, other$ft20)) {
				return false;
			}
			final Object this$ft20Currency = this.getFt20Currency();
			final Object other$ft20Currency = other.getFt20Currency();
			if (!Objects.equals(this$ft20Currency, other$ft20Currency)) {
				return false;
			}
			final Object this$ft40 = this.getFt40();
			final Object other$ft40 = other.getFt40();
			if (!Objects.equals(this$ft40, other$ft40)) {
				return false;
			}
			final Object this$ft40Currency = this.getFt40Currency();
			final Object other$ft40Currency = other.getFt40Currency();
			if (!Objects.equals(this$ft40Currency, other$ft40Currency)) {
				return false;
			}
			final Object this$ft40HC = this.getFt40HC();
			final Object other$ft40HC = other.getFt40HC();
			if (!Objects.equals(this$ft40HC, other$ft40HC)) {
				return false;
			}
			final Object this$ft40HCCurrency = this.getFt40HCCurrency();
			final Object other$ft40HCCurrency = other.getFt40HCCurrency();
			if (!Objects.equals(this$ft40HCCurrency, other$ft40HCCurrency)) {
				return false;
			}
			final Object this$pod = this.getPod();
			final Object other$pod = other.getPod();
			return Objects.equals(this$pod, other$pod);
		}

		@Override
		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $id = this.getId();
			result = (result * PRIME) + (($id != null) ? $id.hashCode() : 43);
			final Object $truck = this.getTruck();
			result = (result * PRIME) + (($truck != null) ? $truck.hashCode() : 43);
			final Object $truckCurrency = this.getTruckCurrency();
			result = (result * PRIME) + (($truckCurrency != null) ? $truckCurrency.hashCode() : 43);
			final Object $ft20 = this.getFt20();
			result = (result * PRIME) + (($ft20 != null) ? $ft20.hashCode() : 43);
			final Object $ft20Currency = this.getFt20Currency();
			result = (result * PRIME) + (($ft20Currency != null) ? $ft20Currency.hashCode() : 43);
			final Object $ft40 = this.getFt40();
			result = (result * PRIME) + (($ft40 != null) ? $ft40.hashCode() : 43);
			final Object $ft40Currency = this.getFt40Currency();
			result = (result * PRIME) + (($ft40Currency != null) ? $ft40Currency.hashCode() : 43);
			final Object $ft40HC = this.getFt40HC();
			result = (result * PRIME) + (($ft40HC != null) ? $ft40HC.hashCode() : 43);
			final Object $ft40HCCurrency = this.getFt40HCCurrency();
			result = (result * PRIME) + (($ft40HCCurrency != null) ? $ft40HCCurrency.hashCode() : 43);
			final Object $pod = this.getPod();
			result = (result * PRIME) + (($pod != null) ? $pod.hashCode() : 43);
			return result;
		}

		@Override
		public String toString() {
			return "SameIdProperty.RouteProperties(id=" + this.getId() + ", truck=" + this.getTruck()
					+ ", truckCurrency=" + this.getTruckCurrency() + ", ft20=" + this.getFt20() + ", ft20Currency="
					+ this.getFt20Currency() + ", ft40=" + this.getFt40() + ", ft40Currency=" + this.getFt40Currency()
					+ ", ft40HC=" + this.getFt40HC() + ", ft40HCCurrency=" + this.getFt40HCCurrency() + ", pod="
					+ this.getPod() + ")";
		}

	}

}
