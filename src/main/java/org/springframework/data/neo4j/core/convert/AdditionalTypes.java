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
package org.springframework.data.neo4j.core.convert;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.value.LossyCoercion;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.convert.ConverterBuilder;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mapping.MappingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Additional types that are supported out of the box. Mostly all of
 * {@link org.springframework.data.mapping.model.SimpleTypeHolder SimpleTypeHolder's} defaults.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @author Dennis Crissman
 * @since 6.0
 */
final class AdditionalTypes {

	static final List<?> CONVERTERS;

	static {

		List<Object> hlp = new ArrayList<>();
		hlp.add(ConverterBuilder.reading(Value.class, boolean[].class, AdditionalTypes::asBooleanArray).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, Byte.class, AdditionalTypes::asByte).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, byte.class, AdditionalTypes::asByte).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, Character.class, AdditionalTypes::asCharacter).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, char.class, AdditionalTypes::asCharacter).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, char[].class, AdditionalTypes::asCharArray).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, Date.class, AdditionalTypes::asDate).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, double[].class, AdditionalTypes::asDoubleArray).andWriting(Values::value));
		hlp.add(new EnumConverter());
		hlp.add(ConverterBuilder.reading(Value.class, Float.class, AdditionalTypes::asFloat).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, float.class, AdditionalTypes::asFloat).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, float[].class, AdditionalTypes::asFloatArray).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, Integer.class, Value::asInt).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, int.class, Value::asInt).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, int[].class, AdditionalTypes::asIntArray).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, Locale.class, AdditionalTypes::asLocale).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, long[].class, AdditionalTypes::asLongArray).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, Short.class, AdditionalTypes::asShort).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, short.class, AdditionalTypes::asShort).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, short[].class, AdditionalTypes::asShortArray).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, String[].class, AdditionalTypes::asStringArray).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, BigDecimal.class, AdditionalTypes::asBigDecimal).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, BigInteger.class, AdditionalTypes::asBigInteger).andWriting(AdditionalTypes::value));
		hlp.add(new TemporalAmountConverter());
		hlp.add(ConverterBuilder.reading(Value.class, Instant.class, AdditionalTypes::asInstant).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, UUID.class, AdditionalTypes::asUUID).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, URL.class, AdditionalTypes::asURL).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, URI.class, AdditionalTypes::asURI).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, TimeZone.class, AdditionalTypes::asTimeZone).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, ZoneId.class, AdditionalTypes::asZoneId).andWriting(AdditionalTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, Entity.class, Value::asEntity));
		hlp.add(ConverterBuilder.reading(Value.class, Node.class, Value::asNode));
		hlp.add(ConverterBuilder.reading(Value.class, Relationship.class, Value::asRelationship));
		hlp.add(ConverterBuilder.reading(Value.class, Map.class, Value::asMap).andWriting(AdditionalTypes::value));

		CONVERTERS = Collections.unmodifiableList(hlp);
	}

	static Value value(Map<?, ?> map) {
		return Values.value(map);
	}

	static TimeZone asTimeZone(Value value) {
		return TimeZone.getTimeZone(value.asString());
	}

	static Value value(TimeZone timeZone) {
		if (timeZone == null) {
			return Values.NULL;
		}

		return Values.value(timeZone.getID());
	}

	static ZoneId asZoneId(Value value) {
		return ZoneId.of(value.asString());
	}

	static Value value(ZoneId zoneId) {
		if (zoneId == null) {
			return Values.NULL;
		}

		return Values.value(zoneId.getId());
	}

	static UUID asUUID(Value value) {
		return UUID.fromString(value.asString());
	}

	static Value value(UUID uuid) {
		if (uuid == null) {
			return Values.NULL;
		}

		return Values.value(uuid.toString());
	}

	static URL asURL(Value value) {
		try {
			return new URL(value.asString());
		} catch (MalformedURLException e) {
			throw new MappingException("Could not create URL from value: " + value.asString(), e);
		}
	}

	static Value value(URL url) {
		if (url == null) {
			return Values.NULL;
		}

		return Values.value(url.toString());
	}

	static URI asURI(Value value) {
		return URI.create(value.asString());
	}

	static Value value(URI uri) {
		if (uri == null) {
			return Values.NULL;
		}
		return Values.value(uri.toString());
	}

	static Instant asInstant(Value value) {
		return value.asZonedDateTime().toInstant();
	}

	static Value value(Instant instant) {
		return Values.value(instant.atOffset(ZoneOffset.UTC));
	}

	static BigDecimal asBigDecimal(Value value) {
		return new BigDecimal(value.asString());
	}

	static Value value(BigDecimal bigDecimal) {
		if (bigDecimal == null) {
			return Values.NULL;
		}

		return Values.value(bigDecimal.toString());
	}

	static BigInteger asBigInteger(Value value) {
		return new BigInteger(value.asString());
	}

	static Value value(BigInteger bigInteger) {
		if (bigInteger == null) {
			return Values.NULL;
		}

		return Values.value(bigInteger.toString());
	}

	static Byte asByte(Value value) {
		byte[] bytes = value.asByteArray();
		Assert.isTrue(bytes.length == 1, "Expected a byte array with exactly 1 element");
		return bytes[0];
	}

	static Value value(Byte aByte) {
		if (aByte == null) {
			return Values.NULL;
		}

		return Values.value(new Byte[] { aByte });
	}

	static Character asCharacter(Value value) {
		char[] chars = value.asString().toCharArray();
		Assert.isTrue(chars.length == 1, "Expected a char array with exactly 1 element");
		return chars[0];
	}

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	static Date asDate(Value value) {

		return Date.from(DATE_TIME_FORMATTER.parse(value.asString(), Instant::from));
	}

	static Value value(Date date) {
		if (date == null) {
			return Values.NULL;
		}

		return Values.value(DATE_TIME_FORMATTER.format(date.toInstant().atZone(ZoneOffset.UTC.normalized())));
	}

	@ReadingConverter
	@WritingConverter
	static final class EnumConverter implements GenericConverter {

		private final Set<ConvertiblePair> convertibleTypes;

		EnumConverter() {
			Set<ConvertiblePair> tmp = new HashSet<>();
			tmp.add(new ConvertiblePair(Value.class, Enum.class));
			tmp.add(new ConvertiblePair(Enum.class, Value.class));
			this.convertibleTypes = Collections.unmodifiableSet(tmp);
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return convertibleTypes;
		}

		@SuppressWarnings({"raw", "unchecked"}) // Due to dynamic enum retrieval
		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

			if (source == null) {
				return Value.class.isAssignableFrom(targetType.getType()) ? Values.NULL : null;
			}

			if (Value.class.isAssignableFrom(sourceType.getType())) {
				return Enum.valueOf((Class<Enum>) targetType.getType(), ((Value) source).asString());
			} else {
				return Values.value(((Enum<?>) source).name());
			}
		}
	}

	/**
	 * This is a workaround for the fact that Spring Data Commons requires {@link GenericConverter generic converters} to
	 * have a non-null convertible pair since 2.3. Without it, they get filtered out and thus not registered in a
	 * conversion service. We do this as an afterthought in
	 * {@link Neo4jConversions#registerConvertersIn(ConverterRegistry)}.
	 * <p>
	 * This class uses is a {@link GenericConverter} without a concrete pair of convertible types. By making it implement
	 * {@link ConditionalConverter} it works with Springs conversion service out of the box.
	 */
	static final class EnumArrayConverter implements GenericConverter, ConditionalConverter {

		private final EnumConverter delegate;

		EnumArrayConverter() {
			this.delegate = new EnumConverter();
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return null;
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (Value.class.isAssignableFrom(sourceType.getType())) {
				return describesSupportedEnumVariant(targetType);
			} else if (Value.class.isAssignableFrom(targetType.getType())) {
				return describesSupportedEnumVariant(sourceType);
			} else {
				return false;
			}
		}

		private static boolean describesSupportedEnumVariant(TypeDescriptor typeDescriptor) {
			return typeDescriptor.isArray()
					&& Enum.class.isAssignableFrom(typeDescriptor.getElementTypeDescriptor().getType());
		}

		@Override
		public Object convert(Object object, TypeDescriptor sourceType, TypeDescriptor targetType) {

			if (object == null) {
				return Value.class.isAssignableFrom(targetType.getType()) ? Values.NULL : null;
			}

			if (Value.class.isAssignableFrom(sourceType.getType())) {
				Value source = (Value) object;

				TypeDescriptor elementTypeDescriptor = targetType.getElementTypeDescriptor();
				Object[] targetArray = (Object[]) Array.newInstance(elementTypeDescriptor.getType(), source.size());

				Arrays.setAll(targetArray,
						i -> delegate.convert(source.get(i), TypeDescriptor.valueOf(Value.class), elementTypeDescriptor));
				return targetArray;
			} else {
				Enum[] source = (Enum[]) object;

				return Values.value(Arrays.stream(source)
						.map(e -> delegate.convert(e, sourceType.getElementTypeDescriptor(), TypeDescriptor.valueOf(Value.class)))
						.toArray());
			}
		}
	}

	static Float asFloat(Value value) {
		return Float.parseFloat(value.asString());
	}

	static Value value(Float aFloat) {
		if (aFloat == null) {
			return Values.NULL;
		}

		return Values.value(aFloat.toString());
	}

	static Locale asLocale(Value value) {

		return StringUtils.parseLocale(value.asString());
	}

	static Value value(Locale locale) {
		if (locale == null) {
			return Values.NULL;
		}

		return Values.value(locale.toString());
	}

	static Short asShort(Value value) {
		long val = value.asLong();
		if (val > Short.MAX_VALUE || val < Short.MIN_VALUE) {
			throw new LossyCoercion(value.type().name(), "Java short");
		}
		return (short) val;
	}

	static Value value(Short aShort) {
		if (aShort == null) {
			return Values.NULL;
		}

		return Values.value(aShort.longValue());
	}

	static boolean[] asBooleanArray(Value value) {
		boolean[] array = new boolean[value.size()];
		int i = 0;
		for (Boolean v : value.values(Value::asBoolean)) {
			array[i++] = v;
		}
		return array;
	}

	static char[] asCharArray(Value value) {
		char[] array = new char[value.size()];
		int i = 0;
		for (Character v : value.values(AdditionalTypes::asCharacter)) {
			array[i++] = v;
		}
		return array;
	}

	static String[] asStringArray(Value value) {
		String[] array = new String[value.size()];
		return value.asList(Value::asString).toArray(array);
	}

	static double[] asDoubleArray(Value value) {
		double[] array = new double[value.size()];
		int i = 0;
		for (double v : value.values(Value::asDouble)) {
			array[i++] = v;
		}
		return array;
	}

	static float[] asFloatArray(Value value) {
		float[] array = new float[value.size()];
		int i = 0;
		for (float v : value.values(AdditionalTypes::asFloat)) {
			array[i++] = v;
		}
		return array;
	}

	static Value value(float[] aFloatArray) {
		if (aFloatArray == null) {
			return Values.NULL;
		}

		String[] values = new String[aFloatArray.length];
		int i = 0;
		for (float v : aFloatArray) {
			values[i++] = Float.toString(v);
		}
		return Values.value(values);
	}

	static int[] asIntArray(Value value) {
		int[] array = new int[value.size()];
		int i = 0;
		for (int v : value.values(Value::asInt)) {
			array[i++] = v;
		}
		return array;
	}

	static long[] asLongArray(Value value) {
		long[] array = new long[value.size()];
		int i = 0;
		for (long v : value.values(Value::asLong)) {
			array[i++] = v;
		}
		return array;
	}

	static short[] asShortArray(Value value) {
		short[] array = new short[value.size()];
		int i = 0;
		for (short v : value.values(AdditionalTypes::asShort)) {
			array[i++] = v;
		}
		return array;
	}

	static Value value(short[] aShortArray) {
		if (aShortArray == null) {
			return Values.NULL;
		}

		long[] values = new long[aShortArray.length];
		int i = 0;
		for (short v : aShortArray) {
			values[i++] = v;
		}
		return Values.value(values);
	}

	private AdditionalTypes() {}
}
