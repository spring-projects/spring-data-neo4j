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

import org.neo4j.driver.types.IsoDuration;
import org.neo4j.driver.types.Point;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZonedDateTime;

/**
 * Contains properties of all cypher types.
 *
 * @author Michael J. Simons
 */
@SuppressWarnings("HiddenField")
@Node("CypherTypes")
public class ThingWithAllCypherTypes {

	@Id
	@GeneratedValue
	public final Long id;

	private boolean aBoolean;

	private long aLong;

	private double aDouble;

	private String aString;

	private byte[] aByteArray;

	private LocalDate aLocalDate;

	private OffsetTime anOffsetTime;

	private LocalTime aLocalTime;

	private ZonedDateTime aZoneDateTime;

	private LocalDateTime aLocalDateTime;

	private IsoDuration anIsoDuration;

	private Point aPoint;

	private Period aZeroPeriod;

	private Duration aZeroDuration;

	private ThingWithAllCypherTypes(Long id, boolean aBoolean, long aLong, double aDouble, String aString, byte[] aByteArray, LocalDate aLocalDate, OffsetTime anOffsetTime, LocalTime aLocalTime, ZonedDateTime aZoneDateTime, LocalDateTime aLocalDateTime, IsoDuration anIsoDuration, Point aPoint, Period aZeroPeriod, Duration aZeroDuration) {
		this.id = id;
		this.aBoolean = aBoolean;
		this.aLong = aLong;
		this.aDouble = aDouble;
		this.aString = aString;
		this.aByteArray = aByteArray;
		this.aLocalDate = aLocalDate;
		this.anOffsetTime = anOffsetTime;
		this.aLocalTime = aLocalTime;
		this.aZoneDateTime = aZoneDateTime;
		this.aLocalDateTime = aLocalDateTime;
		this.anIsoDuration = anIsoDuration;
		this.aPoint = aPoint;
		this.aZeroPeriod = aZeroPeriod;
		this.aZeroDuration = aZeroDuration;
	}

	public static ThingWithAllCypherTypesBuilder builder() {
		return new ThingWithAllCypherTypesBuilder();
	}

	public Long getId() {
		return this.id;
	}

	public boolean isABoolean() {
		return this.aBoolean;
	}

	public long getALong() {
		return this.aLong;
	}

	public double getADouble() {
		return this.aDouble;
	}

	public String getAString() {
		return this.aString;
	}

	public byte[] getAByteArray() {
		return this.aByteArray;
	}

	public LocalDate getALocalDate() {
		return this.aLocalDate;
	}

	public OffsetTime getAnOffsetTime() {
		return this.anOffsetTime;
	}

	public LocalTime getALocalTime() {
		return this.aLocalTime;
	}

	public ZonedDateTime getAZoneDateTime() {
		return this.aZoneDateTime;
	}

	public LocalDateTime getALocalDateTime() {
		return this.aLocalDateTime;
	}

	public IsoDuration getAnIsoDuration() {
		return this.anIsoDuration;
	}

	public Point getAPoint() {
		return this.aPoint;
	}

	public Period getAZeroPeriod() {
		return this.aZeroPeriod;
	}

	public Duration getAZeroDuration() {
		return this.aZeroDuration;
	}

	public void setABoolean(boolean aBoolean) {
		this.aBoolean = aBoolean;
	}

	public void setALong(long aLong) {
		this.aLong = aLong;
	}

	public void setADouble(double aDouble) {
		this.aDouble = aDouble;
	}

	public void setAString(String aString) {
		this.aString = aString;
	}

	public void setAByteArray(byte[] aByteArray) {
		this.aByteArray = aByteArray;
	}

	public void setALocalDate(LocalDate aLocalDate) {
		this.aLocalDate = aLocalDate;
	}

	public void setAnOffsetTime(OffsetTime anOffsetTime) {
		this.anOffsetTime = anOffsetTime;
	}

	public void setALocalTime(LocalTime aLocalTime) {
		this.aLocalTime = aLocalTime;
	}

	public void setAZoneDateTime(ZonedDateTime aZoneDateTime) {
		this.aZoneDateTime = aZoneDateTime;
	}

	public void setALocalDateTime(LocalDateTime aLocalDateTime) {
		this.aLocalDateTime = aLocalDateTime;
	}

	public void setAnIsoDuration(IsoDuration anIsoDuration) {
		this.anIsoDuration = anIsoDuration;
	}

	public void setAPoint(Point aPoint) {
		this.aPoint = aPoint;
	}

	public void setAZeroPeriod(Period aZeroPeriod) {
		this.aZeroPeriod = aZeroPeriod;
	}

	public void setAZeroDuration(Duration aZeroDuration) {
		this.aZeroDuration = aZeroDuration;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof ThingWithAllCypherTypes)) {
			return false;
		}
		final ThingWithAllCypherTypes other = (ThingWithAllCypherTypes) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
			return false;
		}
		if (this.isABoolean() != other.isABoolean()) {
			return false;
		}
		if (this.getALong() != other.getALong()) {
			return false;
		}
		if (Double.compare(this.getADouble(), other.getADouble()) != 0) {
			return false;
		}
		final Object this$aString = this.getAString();
		final Object other$aString = other.getAString();
		if (this$aString == null ? other$aString != null : !this$aString.equals(other$aString)) {
			return false;
		}
		if (!java.util.Arrays.equals(this.getAByteArray(), other.getAByteArray())) {
			return false;
		}
		final Object this$aLocalDate = this.getALocalDate();
		final Object other$aLocalDate = other.getALocalDate();
		if (this$aLocalDate == null ? other$aLocalDate != null : !this$aLocalDate.equals(other$aLocalDate)) {
			return false;
		}
		final Object this$anOffsetTime = this.getAnOffsetTime();
		final Object other$anOffsetTime = other.getAnOffsetTime();
		if (this$anOffsetTime == null ? other$anOffsetTime != null : !this$anOffsetTime.equals(other$anOffsetTime)) {
			return false;
		}
		final Object this$aLocalTime = this.getALocalTime();
		final Object other$aLocalTime = other.getALocalTime();
		if (this$aLocalTime == null ? other$aLocalTime != null : !this$aLocalTime.equals(other$aLocalTime)) {
			return false;
		}
		final Object this$aZoneDateTime = this.getAZoneDateTime();
		final Object other$aZoneDateTime = other.getAZoneDateTime();
		if (this$aZoneDateTime == null ? other$aZoneDateTime != null : !this$aZoneDateTime.equals(other$aZoneDateTime)) {
			return false;
		}
		final Object this$aLocalDateTime = this.getALocalDateTime();
		final Object other$aLocalDateTime = other.getALocalDateTime();
		if (this$aLocalDateTime == null ? other$aLocalDateTime != null : !this$aLocalDateTime.equals(other$aLocalDateTime)) {
			return false;
		}
		final Object this$anIsoDuration = this.getAnIsoDuration();
		final Object other$anIsoDuration = other.getAnIsoDuration();
		if (this$anIsoDuration == null ? other$anIsoDuration != null : !this$anIsoDuration.equals(other$anIsoDuration)) {
			return false;
		}
		final Object this$aPoint = this.getAPoint();
		final Object other$aPoint = other.getAPoint();
		if (this$aPoint == null ? other$aPoint != null : !this$aPoint.equals(other$aPoint)) {
			return false;
		}
		final Object this$aZeroPeriod = this.getAZeroPeriod();
		final Object other$aZeroPeriod = other.getAZeroPeriod();
		if (this$aZeroPeriod == null ? other$aZeroPeriod != null : !this$aZeroPeriod.equals(other$aZeroPeriod)) {
			return false;
		}
		final Object this$aZeroDuration = this.getAZeroDuration();
		final Object other$aZeroDuration = other.getAZeroDuration();
		if (this$aZeroDuration == null ? other$aZeroDuration != null : !this$aZeroDuration.equals(other$aZeroDuration)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof ThingWithAllCypherTypes;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		result = result * PRIME + (this.isABoolean() ? 79 : 97);
		final long $aLong = this.getALong();
		result = result * PRIME + (int) ($aLong >>> 32 ^ $aLong);
		final long $aDouble = Double.doubleToLongBits(this.getADouble());
		result = result * PRIME + (int) ($aDouble >>> 32 ^ $aDouble);
		final Object $aString = this.getAString();
		result = result * PRIME + ($aString == null ? 43 : $aString.hashCode());
		result = result * PRIME + java.util.Arrays.hashCode(this.getAByteArray());
		final Object $aLocalDate = this.getALocalDate();
		result = result * PRIME + ($aLocalDate == null ? 43 : $aLocalDate.hashCode());
		final Object $anOffsetTime = this.getAnOffsetTime();
		result = result * PRIME + ($anOffsetTime == null ? 43 : $anOffsetTime.hashCode());
		final Object $aLocalTime = this.getALocalTime();
		result = result * PRIME + ($aLocalTime == null ? 43 : $aLocalTime.hashCode());
		final Object $aZoneDateTime = this.getAZoneDateTime();
		result = result * PRIME + ($aZoneDateTime == null ? 43 : $aZoneDateTime.hashCode());
		final Object $aLocalDateTime = this.getALocalDateTime();
		result = result * PRIME + ($aLocalDateTime == null ? 43 : $aLocalDateTime.hashCode());
		final Object $anIsoDuration = this.getAnIsoDuration();
		result = result * PRIME + ($anIsoDuration == null ? 43 : $anIsoDuration.hashCode());
		final Object $aPoint = this.getAPoint();
		result = result * PRIME + ($aPoint == null ? 43 : $aPoint.hashCode());
		final Object $aZeroPeriod = this.getAZeroPeriod();
		result = result * PRIME + ($aZeroPeriod == null ? 43 : $aZeroPeriod.hashCode());
		final Object $aZeroDuration = this.getAZeroDuration();
		result = result * PRIME + ($aZeroDuration == null ? 43 : $aZeroDuration.hashCode());
		return result;
	}

	public String toString() {
		return "ThingWithAllCypherTypes(id=" + this.getId() + ", aBoolean=" + this.isABoolean() + ", aLong=" + this.getALong() + ", aDouble=" + this.getADouble() + ", aString=" + this.getAString() + ", aByteArray=" + java.util.Arrays.toString(this.getAByteArray()) + ", aLocalDate=" + this.getALocalDate() + ", anOffsetTime=" + this.getAnOffsetTime() + ", aLocalTime=" + this.getALocalTime() + ", aZoneDateTime=" + this.getAZoneDateTime() + ", aLocalDateTime=" + this.getALocalDateTime() + ", anIsoDuration=" + this.getAnIsoDuration() + ", aPoint=" + this.getAPoint() + ", aZeroPeriod=" + this.getAZeroPeriod() + ", aZeroDuration=" + this.getAZeroDuration() + ")";
	}

	public ThingWithAllCypherTypes withId(Long id) {
		return this.id == id ? this : new ThingWithAllCypherTypes(id, this.aBoolean, this.aLong, this.aDouble, this.aString, this.aByteArray, this.aLocalDate, this.anOffsetTime, this.aLocalTime, this.aZoneDateTime, this.aLocalDateTime, this.anIsoDuration, this.aPoint, this.aZeroPeriod, this.aZeroDuration);
	}

	/**
	 * the builder
	 */
	public static class ThingWithAllCypherTypesBuilder {
		private Long id;
		private boolean aBoolean;
		private long aLong;
		private double aDouble;
		private String aString;
		private byte[] aByteArray;
		private LocalDate aLocalDate;
		private OffsetTime anOffsetTime;
		private LocalTime aLocalTime;
		private ZonedDateTime aZoneDateTime;
		private LocalDateTime aLocalDateTime;
		private IsoDuration anIsoDuration;
		private Point aPoint;
		private Period aZeroPeriod;
		private Duration aZeroDuration;

		ThingWithAllCypherTypesBuilder() {
		}

		public ThingWithAllCypherTypesBuilder id(Long id) {
			this.id = id;
			return this;
		}

		public ThingWithAllCypherTypesBuilder aBoolean(boolean aBoolean) {
			this.aBoolean = aBoolean;
			return this;
		}

		public ThingWithAllCypherTypesBuilder aLong(long aLong) {
			this.aLong = aLong;
			return this;
		}

		public ThingWithAllCypherTypesBuilder aDouble(double aDouble) {
			this.aDouble = aDouble;
			return this;
		}

		public ThingWithAllCypherTypesBuilder aString(String aString) {
			this.aString = aString;
			return this;
		}

		public ThingWithAllCypherTypesBuilder aByteArray(byte[] aByteArray) {
			this.aByteArray = aByteArray;
			return this;
		}

		public ThingWithAllCypherTypesBuilder aLocalDate(LocalDate aLocalDate) {
			this.aLocalDate = aLocalDate;
			return this;
		}

		public ThingWithAllCypherTypesBuilder anOffsetTime(OffsetTime anOffsetTime) {
			this.anOffsetTime = anOffsetTime;
			return this;
		}

		public ThingWithAllCypherTypesBuilder aLocalTime(LocalTime aLocalTime) {
			this.aLocalTime = aLocalTime;
			return this;
		}

		public ThingWithAllCypherTypesBuilder aZoneDateTime(ZonedDateTime aZoneDateTime) {
			this.aZoneDateTime = aZoneDateTime;
			return this;
		}

		public ThingWithAllCypherTypesBuilder aLocalDateTime(LocalDateTime aLocalDateTime) {
			this.aLocalDateTime = aLocalDateTime;
			return this;
		}

		public ThingWithAllCypherTypesBuilder anIsoDuration(IsoDuration anIsoDuration) {
			this.anIsoDuration = anIsoDuration;
			return this;
		}

		public ThingWithAllCypherTypesBuilder aPoint(Point aPoint) {
			this.aPoint = aPoint;
			return this;
		}

		public ThingWithAllCypherTypesBuilder aZeroPeriod(Period aZeroPeriod) {
			this.aZeroPeriod = aZeroPeriod;
			return this;
		}

		public ThingWithAllCypherTypesBuilder aZeroDuration(Duration aZeroDuration) {
			this.aZeroDuration = aZeroDuration;
			return this;
		}

		public ThingWithAllCypherTypes build() {
			return new ThingWithAllCypherTypes(this.id, this.aBoolean, this.aLong, this.aDouble, this.aString, this.aByteArray, this.aLocalDate, this.anOffsetTime, this.aLocalTime, this.aZoneDateTime, this.aLocalDateTime, this.anIsoDuration, this.aPoint, this.aZeroPeriod, this.aZeroDuration);
		}

		public String toString() {
			return "ThingWithAllCypherTypes.ThingWithAllCypherTypesBuilder(id=" + this.id + ", aBoolean=" + this.aBoolean + ", aLong=" + this.aLong + ", aDouble=" + this.aDouble + ", aString=" + this.aString + ", aByteArray=" + java.util.Arrays.toString(this.aByteArray) + ", aLocalDate=" + this.aLocalDate + ", anOffsetTime=" + this.anOffsetTime + ", aLocalTime=" + this.aLocalTime + ", aZoneDateTime=" + this.aZoneDateTime + ", aLocalDateTime=" + this.aLocalDateTime + ", anIsoDuration=" + this.anIsoDuration + ", aPoint=" + this.aPoint + ", aZeroPeriod=" + this.aZeroPeriod + ", aZeroDuration=" + this.aZeroDuration + ")";
		}
	}
}
