/*
 * Copyright 2011-2025 the original author or authors.
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZonedDateTime;

import org.neo4j.driver.types.IsoDuration;
import org.neo4j.driver.types.Point;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Similar to {@link ThingWithAllCypherTypes} but using field access.
 *
 * @author Michael J. Simons
 */
@SuppressWarnings("HiddenField")
@Node
public class ThingWithAllCypherTypes2 {

	@Id
	@GeneratedValue
	private final Long id;

	private boolean aBoolean;

	private long aLong;

	private int anInt;

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

	public ThingWithAllCypherTypes2(Long id) {
		this.id = id;
	}

	public Long getId() {
		return this.id;
	}

	public boolean isABoolean() {
		return this.aBoolean;
	}

	public void setABoolean(boolean aBoolean) {
		this.aBoolean = aBoolean;
	}

	public long getALong() {
		return this.aLong;
	}

	public void setALong(long aLong) {
		this.aLong = aLong;
	}

	public int getAnInt() {
		return this.anInt;
	}

	public void setAnInt(int anInt) {
		this.anInt = anInt;
	}

	public double getADouble() {
		return this.aDouble;
	}

	public void setADouble(double aDouble) {
		this.aDouble = aDouble;
	}

	public String getAString() {
		return this.aString;
	}

	public void setAString(String aString) {
		this.aString = aString;
	}

	public byte[] getAByteArray() {
		return this.aByteArray;
	}

	public void setAByteArray(byte[] aByteArray) {
		this.aByteArray = aByteArray;
	}

	public LocalDate getALocalDate() {
		return this.aLocalDate;
	}

	public void setALocalDate(LocalDate aLocalDate) {
		this.aLocalDate = aLocalDate;
	}

	public OffsetTime getAnOffsetTime() {
		return this.anOffsetTime;
	}

	public void setAnOffsetTime(OffsetTime anOffsetTime) {
		this.anOffsetTime = anOffsetTime;
	}

	public LocalTime getALocalTime() {
		return this.aLocalTime;
	}

	public void setALocalTime(LocalTime aLocalTime) {
		this.aLocalTime = aLocalTime;
	}

	public ZonedDateTime getAZoneDateTime() {
		return this.aZoneDateTime;
	}

	public void setAZoneDateTime(ZonedDateTime aZoneDateTime) {
		this.aZoneDateTime = aZoneDateTime;
	}

	public LocalDateTime getALocalDateTime() {
		return this.aLocalDateTime;
	}

	public void setALocalDateTime(LocalDateTime aLocalDateTime) {
		this.aLocalDateTime = aLocalDateTime;
	}

	public IsoDuration getAnIsoDuration() {
		return this.anIsoDuration;
	}

	public void setAnIsoDuration(IsoDuration anIsoDuration) {
		this.anIsoDuration = anIsoDuration;
	}

	public Point getAPoint() {
		return this.aPoint;
	}

	public void setAPoint(Point aPoint) {
		this.aPoint = aPoint;
	}

	public Period getAZeroPeriod() {
		return this.aZeroPeriod;
	}

	public void setAZeroPeriod(Period aZeroPeriod) {
		this.aZeroPeriod = aZeroPeriod;
	}

	public Duration getAZeroDuration() {
		return this.aZeroDuration;
	}

	public void setAZeroDuration(Duration aZeroDuration) {
		this.aZeroDuration = aZeroDuration;
	}

}
