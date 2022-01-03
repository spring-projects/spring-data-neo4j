/*
 * Copyright 2011-2022 the original author or authors.
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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;

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
 * Contains properties of all cypher types.
 *
 * @author Michael J. Simons
 */
@Node("CypherTypes")
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ThingWithAllCypherTypes {

	@Id @GeneratedValue @With public final Long id;

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
}
