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
package org.springframework.data.neo4j.integration.shared.conversion;

import lombok.Builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithAllAdditionalTypes.SomeEnum;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.types.CartesianPoint2d;
import org.springframework.data.neo4j.types.CartesianPoint3d;
import org.springframework.data.neo4j.types.GeographicPoint2d;
import org.springframework.data.neo4j.types.GeographicPoint3d;

/**
 * Provides some nodes spotting properties of all types we support.
 *
 * @author Michael J. Simons
 */
public abstract class Neo4jConversionsITBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	protected static final BookmarkCapture bookmarkCapture = new BookmarkCapture();

	protected static final Map<String, Object> CYPHER_TYPES;
	static {
		Map<String, Object> hlp = new HashMap<>();
		hlp.put("aBoolean", true);
		hlp.put("aLong", Long.MAX_VALUE);
		hlp.put("aDouble", 1.7976931348);
		hlp.put("aString", "Hallo, Cypher");
		hlp.put("aByteArray", "A thing".getBytes());
		hlp.put("aPoint", Values.point(7203, 47, 11).asPoint());
		hlp.put("aLocalDate", LocalDate.of(2015, 7, 21));
		hlp.put("anOffsetTime", OffsetTime.of(12, 31, 0, 0, ZoneOffset.ofHours(1)));
		hlp.put("aLocalTime", LocalTime.of(12, 31, 14));
		hlp.put("aZoneDateTime",
				ZonedDateTime.of(2015, 7, 21, 21, 40, 32, 0, TimeZone.getTimeZone("America/New_York").toZoneId()));
		hlp.put("aLocalDateTime", LocalDateTime.of(2015, 7, 21, 21, 0));
		hlp.put("anIsoDuration", Values.isoDuration(0, 14, 58320, 0).asObject());
		CYPHER_TYPES = Collections.unmodifiableMap(hlp);
	}

	protected static final Map<String, Object> ADDITIONAL_TYPES;
	static {
		Map<String, Object> hlp = new HashMap<>();
		hlp.put("booleanArray", new boolean[] { true, true, false });
		hlp.put("aByte", (byte) 6);
		hlp.put("aChar", 'x');
		hlp.put("charArray", new char[] { 'x', 'y', 'z' });
		hlp.put("aDate",
				Date.from(LocalDateTime.of(2019, 9, 21, 15, 23, 11).atZone(ZoneId.of("Europe/Berlin")).toInstant()));
		hlp.put("aBigDecimal", BigDecimal.valueOf(Double.MAX_VALUE).multiply(BigDecimal.TEN));
		hlp.put("aBigInteger", BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN));
		hlp.put("doubleArray", new double[] { 1.1, 2.2, 3.3 });
		hlp.put("aFloat", 23.42F);
		hlp.put("floatArray", new float[] { 4.4F, 5.5F });
		hlp.put("anInt", 42);
		hlp.put("intArray", new int[] { 21, 9 });
		hlp.put("aLocale", Locale.GERMANY);
		hlp.put("longArray", new long[] { Long.MIN_VALUE, Long.MAX_VALUE });
		hlp.put("aShort", (short) 127);
		hlp.put("shortArray", new short[] { -10, 10 });
		hlp.put("aPeriod", Period.of(23, 4, 7));
		hlp.put("aDuration", Duration.ofHours(25).plusMinutes(63).plusSeconds(65));
		hlp.put("stringArray", new String[] { "Hallo", "Welt" });
		// Done on purpose, otherwise the target collection cannot be determined.
		hlp.put("listOfStrings", new ArrayList<>(Arrays.asList("Hello", "World")));
		hlp.put("setOfStrings", new HashSet<>(Arrays.asList("Hallo", "Welt")));
		hlp.put("anInstant", Instant.from(LocalDateTime.of(2019, 9, 26, 20, 34, 23).atOffset(ZoneOffset.UTC)));
		hlp.put("aUUID", UUID.fromString("d4ec9208-4b17-4ec7-a709-19a5e53865a8"));
		try {
			hlp.put("aURL", new URL("https://www.test.com"));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		hlp.put("aURI", URI.create("urn:isbn:9783864905254"));
		hlp.put("anEnum", SomeEnum.TheUsualMisfit);
		hlp.put("anArrayOfEnums", new SomeEnum[] { SomeEnum.ValueA, SomeEnum.ValueB });
		hlp.put("aCollectionOfEnums", Arrays.asList(SomeEnum.ValueC, SomeEnum.TheUsualMisfit));
		hlp.put("listOfDoubles", Arrays.asList(1.0));
		hlp.put("aTimeZone", TimeZone.getTimeZone("America/Los_Angeles"));
		hlp.put("aZoneId", ZoneId.of("America/New_York"));
		hlp.put("aZeroPeriod", Period.of(0, 0, 0));
		hlp.put("aZeroDuration", Duration.ZERO);
		ADDITIONAL_TYPES = Collections.unmodifiableMap(hlp);
	}

	@Builder
	protected static class ParamHolder {
		String name;
		double latitude;
		double longitude;

		Map<String, Object> toParameterMap() {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("name", this.name);
			parameters.put("latitude", this.latitude);
			parameters.put("longitude", this.longitude);
			return parameters;
		}

		Point asSpringPoint() {
			return new Point(latitude, longitude);
		}

		GeographicPoint2d asGeo2d() {
			return new GeographicPoint2d(latitude, longitude);
		}

		GeographicPoint3d asGeo3d(double height) {
			return new GeographicPoint3d(latitude, longitude, height);
		}
	}

	private static final ParamHolder NEO_HQ = ParamHolder.builder().latitude(55.612191).longitude(12.994823)
			.name("Neo4j HQ").build();
	private static final ParamHolder CLARION = ParamHolder.builder().latitude(55.607726).longitude(12.994243)
			.name("Clarion").build();
	private static final ParamHolder MINC = ParamHolder.builder().latitude(55.611496).longitude(12.994039).name("Minc")
			.build();

	protected static final Map<String, Object> SPATIAL_TYPES;
	static {
		Map<String, Object> hlp = new HashMap<>();
		hlp.put("sdnPoint", NEO_HQ.asSpringPoint());
		hlp.put("geo2d", MINC.asGeo2d());
		hlp.put("geo3d", CLARION.asGeo3d(27.0));
		hlp.put("car2d", new CartesianPoint2d(10, 20));
		hlp.put("car3d", new CartesianPoint3d(30, 40, 50));
		SPATIAL_TYPES = Collections.unmodifiableMap(hlp);
	}

	protected static final Map<String, Object> CUSTOM_TYPES;
	static {
		Map<String, Object> hlp = new HashMap<>();
		hlp.put("customType", ThingWithCustomTypes.CustomType.of("ABCD"));
		hlp.put("dateAsLong", Date.from(ZonedDateTime.of(2020, 9, 21,
				12, 0, 0, 0, ZoneId.of("Europe/Berlin")).toInstant()));
		hlp.put("dateAsString", Date.from(ZonedDateTime.of(2013, 5, 6,
				12, 0, 0, 0, ZoneId.of("Europe/Berlin")).toInstant().truncatedTo(ChronoUnit.DAYS)));
		CUSTOM_TYPES = Collections.unmodifiableMap(hlp);
	}

	protected static long ID_OF_CYPHER_TYPES_NODE;
	protected static long ID_OF_ADDITIONAL_TYPES_NODE;
	protected static long ID_OF_SPATIAL_TYPES_NODE;
	protected static long ID_OF_CUSTOM_TYPE_NODE;

	@BeforeAll
	static void prepareData() {

		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			session.executeWriteWithoutResult(w -> {
				Map<String, Object> parameters;

				w.run("MATCH (n) detach delete n");

				parameters = new HashMap<>();
				parameters.put("aByteArray", "A thing".getBytes());
				ID_OF_CYPHER_TYPES_NODE = w.run("""
					CREATE (n:CypherTypes)
					SET
						n.aBoolean = true,
						n.aLong = 9223372036854775807, n.aDouble = 1.7976931348, n.aString = 'Hallo, Cypher',
						n.aByteArray = $aByteArray, n.aLocalDate = date('2015-07-21'),
						n.anOffsetTime  = time({ hour:12, minute:31, timezone: '+01:00' }),
						n.aLocalTime = localtime({ hour:12, minute:31, second:14 }),
						n.aZoneDateTime = datetime('2015-07-21T21:40:32-04[America/New_York]'),
						n.aLocalDateTime = localdatetime('2015202T21'), n.anIsoDuration = duration('P14DT16H12M'),
						n.aPoint = point({x:47, y:11})
					RETURN id(n) AS id
					""", parameters).single().get("id").asLong();

				parameters = new HashMap<>();
				parameters.put("aByte", Values.value(new byte[] { 6 }));
				ID_OF_ADDITIONAL_TYPES_NODE = w.run("""
					CREATE (n:AdditionalTypes)
					SET
						n.booleanArray = [true, true, false], n.aByte = $aByte,
						n.aChar = 'x', n.charArray = ['x', 'y', 'z'], n.aDate = '2019-09-21T13:23:11Z',
						n.doubleArray = [1.1, 2.2, 3.3], n.aFloat = '23.42', n.floatArray = ['4.4', '5.5'],
						n.anInt = 42, n.intArray = [21, 9], n.aLocale = 'de_DE',
						n.longArray = [-9223372036854775808, 9223372036854775807], n.aShort = 127,
						n.shortArray = [-10, 10], n.aBigDecimal = '1.79769313486231570E+309',
						n.aBigInteger = '92233720368547758070', n.aPeriod = duration('P23Y4M7D'),
						n.aDuration = duration('PT26H4M5S'), n.stringArray = ['Hallo', 'Welt'],
						n.listOfStrings = ['Hello', 'World'], n.setOfStrings = ['Hallo', 'Welt'],
						n.anInstant = datetime('2019-09-26T20:34:23Z'),
						n.aUUID = 'd4ec9208-4b17-4ec7-a709-19a5e53865a8', n.listOfDoubles = [1.0],
						n.aURL = 'https://www.test.com',
						n.aURI = 'urn:isbn:9783864905254',
						n.anEnum = 'TheUsualMisfit', n.anArrayOfEnums = ['ValueA', 'ValueB'],
						n.aCollectionOfEnums = ['ValueC', 'TheUsualMisfit'],
						n.aTimeZone = 'America/Los_Angeles',\s
						n.aZoneId = 'America/New_York', n.aZeroPeriod = duration('PT0S'), n.aZeroDuration = duration('PT0S')
					RETURN id(n) AS id
					""", parameters).single().get("id").asLong();

				parameters = new HashMap<>();
				parameters.put("neo4j", NEO_HQ.toParameterMap());
				parameters.put("minc", MINC.toParameterMap());
				parameters.put("clarion", CLARION.toParameterMap());
				parameters.put("aByte", Values.value(new byte[] { 6 }));
				ID_OF_SPATIAL_TYPES_NODE = w.run("""
					CREATE (n:SpatialTypes)
					SET
						n.sdnPoint = point({latitude: $neo4j.latitude, longitude: $neo4j.longitude}),
						n.geo2d = point({latitude: $minc.latitude, longitude: $minc.longitude}),
						n.geo3d = point({latitude: $clarion.latitude, longitude: $clarion.longitude, height: 27}),
						n.car2d = point({x: 10, y: 20}), n.car3d = point({x: 30, y: 40, z: 50})
					RETURN id(n) AS id
					""", parameters).single().get("id").asLong();

				parameters = new HashMap<>();
				parameters.put("customType", "ABCD");
				parameters.put("dateAsLong", 1600682400000L);
				parameters.put("dateAsString", "2013-05-06");
				ID_OF_CUSTOM_TYPE_NODE = w
						.run("CREATE (n:CustomTypes) SET n.customType = $customType, n.dateAsLong = $dateAsLong, n.dateAsString = $dateAsString RETURN id(n) AS id", parameters)
						.single().get("id").asLong();
			});
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}
}
