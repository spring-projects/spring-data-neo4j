/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.integration.shared;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.Node;

/**
 * Contains properties of all additional types.
 *
 * @author Michael J. Simons
 */
@Node("AdditionalTypes")
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ThingWithAllAdditionalTypes {

	enum SomeEnum {
		ValueA, ValueB, TheUsualMisfit, ValueC
	}

	@Id @GeneratedValue
	@With
	public final Long id;

	private boolean[] booleanArray;

	private byte aByte;

	private char aChar;

	private char[] charArray;

	private Date aDate;

	private BigDecimal aBigDecimal;

	private BigInteger aBigInteger;

	private double[] doubleArray;

	private float aFloat;

	private float[] floatArray;

	private int anInt;

	private int[] intArray;

	private Locale aLocale;

	private long[] longArray;

	private short aShort;

	private short[] shortArray;

	private Period aPeriod;

	private Duration aDuration;

	private String[] stringArray;

	private List<String> listOfStrings;

	private Set<String> setOfStrings;

	private Instant anInstant;

	private UUID aUUID;

	private SomeEnum anEnum;

	private SomeEnum[] anArrayOfEnums;

	private List<Double> listOfDoubles;

	private List<SomeEnum> aCollectionOfEnums;
}
