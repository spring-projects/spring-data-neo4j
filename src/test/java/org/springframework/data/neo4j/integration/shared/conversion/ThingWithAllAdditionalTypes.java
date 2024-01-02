/*
 * Copyright 2011-2024 the original author or authors.
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Contains properties of all additional types.
 *
 * @author Michael J. Simons
 */
@SuppressWarnings("HiddenField")
@Node("AdditionalTypes")
public class ThingWithAllAdditionalTypes {

	private ThingWithAllAdditionalTypes(Long id, boolean[] booleanArray, byte aByte, char aChar, char[] charArray, Date aDate, BigDecimal aBigDecimal, BigInteger aBigInteger, double[] doubleArray, float aFloat, float[] floatArray, int anInt, int[] intArray, Locale aLocale, long[] longArray, short aShort, short[] shortArray, Period aPeriod, Duration aDuration, String[] stringArray, List<String> listOfStrings, Set<String> setOfStrings, Instant anInstant, UUID aUUID, URL aURL, URI aURI, SomeEnum anEnum, SomeEnum[] anArrayOfEnums, List<Double> listOfDoubles, List<SomeEnum> aCollectionOfEnums, TimeZone aTimeZone, ZoneId aZoneId, Period aZeroPeriod, Duration aZeroDuration) {
		this.id = id;
		this.booleanArray = booleanArray;
		this.aByte = aByte;
		this.aChar = aChar;
		this.charArray = charArray;
		this.aDate = aDate;
		this.aBigDecimal = aBigDecimal;
		this.aBigInteger = aBigInteger;
		this.doubleArray = doubleArray;
		this.aFloat = aFloat;
		this.floatArray = floatArray;
		this.anInt = anInt;
		this.intArray = intArray;
		this.aLocale = aLocale;
		this.longArray = longArray;
		this.aShort = aShort;
		this.shortArray = shortArray;
		this.aPeriod = aPeriod;
		this.aDuration = aDuration;
		this.stringArray = stringArray;
		this.listOfStrings = listOfStrings;
		this.setOfStrings = setOfStrings;
		this.anInstant = anInstant;
		this.aUUID = aUUID;
		this.aURL = aURL;
		this.aURI = aURI;
		this.anEnum = anEnum;
		this.anArrayOfEnums = anArrayOfEnums;
		this.listOfDoubles = listOfDoubles;
		this.aCollectionOfEnums = aCollectionOfEnums;
		this.aTimeZone = aTimeZone;
		this.aZoneId = aZoneId;
		this.aZeroPeriod = aZeroPeriod;
		this.aZeroDuration = aZeroDuration;
	}

	public static ThingWithAllAdditionalTypesBuilder builder() {
		return new ThingWithAllAdditionalTypesBuilder();
	}

	public Long getId() {
		return this.id;
	}

	public boolean[] getBooleanArray() {
		return this.booleanArray;
	}

	public byte getAByte() {
		return this.aByte;
	}

	public char getAChar() {
		return this.aChar;
	}

	public char[] getCharArray() {
		return this.charArray;
	}

	public Date getADate() {
		return this.aDate;
	}

	public BigDecimal getABigDecimal() {
		return this.aBigDecimal;
	}

	public BigInteger getABigInteger() {
		return this.aBigInteger;
	}

	public double[] getDoubleArray() {
		return this.doubleArray;
	}

	public float getAFloat() {
		return this.aFloat;
	}

	public float[] getFloatArray() {
		return this.floatArray;
	}

	public int getAnInt() {
		return this.anInt;
	}

	public int[] getIntArray() {
		return this.intArray;
	}

	public Locale getALocale() {
		return this.aLocale;
	}

	public long[] getLongArray() {
		return this.longArray;
	}

	public short getAShort() {
		return this.aShort;
	}

	public short[] getShortArray() {
		return this.shortArray;
	}

	public Period getAPeriod() {
		return this.aPeriod;
	}

	public Duration getADuration() {
		return this.aDuration;
	}

	public String[] getStringArray() {
		return this.stringArray;
	}

	public List<String> getListOfStrings() {
		return this.listOfStrings;
	}

	public Set<String> getSetOfStrings() {
		return this.setOfStrings;
	}

	public Instant getAnInstant() {
		return this.anInstant;
	}

	public UUID getAUUID() {
		return this.aUUID;
	}

	public URL getAURL() {
		return this.aURL;
	}

	public URI getAURI() {
		return this.aURI;
	}

	public SomeEnum getAnEnum() {
		return this.anEnum;
	}

	public SomeEnum[] getAnArrayOfEnums() {
		return this.anArrayOfEnums;
	}

	public List<Double> getListOfDoubles() {
		return this.listOfDoubles;
	}

	public List<SomeEnum> getACollectionOfEnums() {
		return this.aCollectionOfEnums;
	}

	public TimeZone getATimeZone() {
		return this.aTimeZone;
	}

	public ZoneId getAZoneId() {
		return this.aZoneId;
	}

	public Period getAZeroPeriod() {
		return this.aZeroPeriod;
	}

	public Duration getAZeroDuration() {
		return this.aZeroDuration;
	}

	public void setBooleanArray(boolean[] booleanArray) {
		this.booleanArray = booleanArray;
	}

	public void setAByte(byte aByte) {
		this.aByte = aByte;
	}

	public void setAChar(char aChar) {
		this.aChar = aChar;
	}

	public void setCharArray(char[] charArray) {
		this.charArray = charArray;
	}

	public void setADate(Date aDate) {
		this.aDate = aDate;
	}

	public void setABigDecimal(BigDecimal aBigDecimal) {
		this.aBigDecimal = aBigDecimal;
	}

	public void setABigInteger(BigInteger aBigInteger) {
		this.aBigInteger = aBigInteger;
	}

	public void setDoubleArray(double[] doubleArray) {
		this.doubleArray = doubleArray;
	}

	public void setAFloat(float aFloat) {
		this.aFloat = aFloat;
	}

	public void setFloatArray(float[] floatArray) {
		this.floatArray = floatArray;
	}

	public void setAnInt(int anInt) {
		this.anInt = anInt;
	}

	public void setIntArray(int[] intArray) {
		this.intArray = intArray;
	}

	public void setALocale(Locale aLocale) {
		this.aLocale = aLocale;
	}

	public void setLongArray(long[] longArray) {
		this.longArray = longArray;
	}

	public void setAShort(short aShort) {
		this.aShort = aShort;
	}

	public void setShortArray(short[] shortArray) {
		this.shortArray = shortArray;
	}

	public void setAPeriod(Period aPeriod) {
		this.aPeriod = aPeriod;
	}

	public void setADuration(Duration aDuration) {
		this.aDuration = aDuration;
	}

	public void setStringArray(String[] stringArray) {
		this.stringArray = stringArray;
	}

	public void setListOfStrings(List<String> listOfStrings) {
		this.listOfStrings = listOfStrings;
	}

	public void setSetOfStrings(Set<String> setOfStrings) {
		this.setOfStrings = setOfStrings;
	}

	public void setAnInstant(Instant anInstant) {
		this.anInstant = anInstant;
	}

	public void setAUUID(UUID aUUID) {
		this.aUUID = aUUID;
	}

	public void setAURL(URL aURL) {
		this.aURL = aURL;
	}

	public void setAURI(URI aURI) {
		this.aURI = aURI;
	}

	public void setAnEnum(SomeEnum anEnum) {
		this.anEnum = anEnum;
	}

	public void setAnArrayOfEnums(SomeEnum[] anArrayOfEnums) {
		this.anArrayOfEnums = anArrayOfEnums;
	}

	public void setListOfDoubles(List<Double> listOfDoubles) {
		this.listOfDoubles = listOfDoubles;
	}

	public void setACollectionOfEnums(List<SomeEnum> aCollectionOfEnums) {
		this.aCollectionOfEnums = aCollectionOfEnums;
	}

	public void setATimeZone(TimeZone aTimeZone) {
		this.aTimeZone = aTimeZone;
	}

	public void setAZoneId(ZoneId aZoneId) {
		this.aZoneId = aZoneId;
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
		if (!(o instanceof ThingWithAllAdditionalTypes)) {
			return false;
		}
		final ThingWithAllAdditionalTypes other = (ThingWithAllAdditionalTypes) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
			return false;
		}
		if (!java.util.Arrays.equals(this.getBooleanArray(), other.getBooleanArray())) {
			return false;
		}
		if (this.getAByte() != other.getAByte()) {
			return false;
		}
		if (this.getAChar() != other.getAChar()) {
			return false;
		}
		if (!java.util.Arrays.equals(this.getCharArray(), other.getCharArray())) {
			return false;
		}
		final Object this$aDate = this.getADate();
		final Object other$aDate = other.getADate();
		if (this$aDate == null ? other$aDate != null : !this$aDate.equals(other$aDate)) {
			return false;
		}
		final Object this$aBigDecimal = this.getABigDecimal();
		final Object other$aBigDecimal = other.getABigDecimal();
		if (this$aBigDecimal == null ? other$aBigDecimal != null : !this$aBigDecimal.equals(other$aBigDecimal)) {
			return false;
		}
		final Object this$aBigInteger = this.getABigInteger();
		final Object other$aBigInteger = other.getABigInteger();
		if (this$aBigInteger == null ? other$aBigInteger != null : !this$aBigInteger.equals(other$aBigInteger)) {
			return false;
		}
		if (!java.util.Arrays.equals(this.getDoubleArray(), other.getDoubleArray())) {
			return false;
		}
		if (Float.compare(this.getAFloat(), other.getAFloat()) != 0) {
			return false;
		}
		if (!java.util.Arrays.equals(this.getFloatArray(), other.getFloatArray())) {
			return false;
		}
		if (this.getAnInt() != other.getAnInt()) {
			return false;
		}
		if (!java.util.Arrays.equals(this.getIntArray(), other.getIntArray())) {
			return false;
		}
		final Object this$aLocale = this.getALocale();
		final Object other$aLocale = other.getALocale();
		if (this$aLocale == null ? other$aLocale != null : !this$aLocale.equals(other$aLocale)) {
			return false;
		}
		if (!java.util.Arrays.equals(this.getLongArray(), other.getLongArray())) {
			return false;
		}
		if (this.getAShort() != other.getAShort()) {
			return false;
		}
		if (!java.util.Arrays.equals(this.getShortArray(), other.getShortArray())) {
			return false;
		}
		final Object this$aPeriod = this.getAPeriod();
		final Object other$aPeriod = other.getAPeriod();
		if (this$aPeriod == null ? other$aPeriod != null : !this$aPeriod.equals(other$aPeriod)) {
			return false;
		}
		final Object this$aDuration = this.getADuration();
		final Object other$aDuration = other.getADuration();
		if (this$aDuration == null ? other$aDuration != null : !this$aDuration.equals(other$aDuration)) {
			return false;
		}
		if (!java.util.Arrays.deepEquals(this.getStringArray(), other.getStringArray())) {
			return false;
		}
		final Object this$listOfStrings = this.getListOfStrings();
		final Object other$listOfStrings = other.getListOfStrings();
		if (this$listOfStrings == null ? other$listOfStrings != null : !this$listOfStrings.equals(other$listOfStrings)) {
			return false;
		}
		final Object this$setOfStrings = this.getSetOfStrings();
		final Object other$setOfStrings = other.getSetOfStrings();
		if (this$setOfStrings == null ? other$setOfStrings != null : !this$setOfStrings.equals(other$setOfStrings)) {
			return false;
		}
		final Object this$anInstant = this.getAnInstant();
		final Object other$anInstant = other.getAnInstant();
		if (this$anInstant == null ? other$anInstant != null : !this$anInstant.equals(other$anInstant)) {
			return false;
		}
		final Object this$aUUID = this.getAUUID();
		final Object other$aUUID = other.getAUUID();
		if (this$aUUID == null ? other$aUUID != null : !this$aUUID.equals(other$aUUID)) {
			return false;
		}
		final Object this$aURL = this.getAURL();
		final Object other$aURL = other.getAURL();
		if (this$aURL == null ? other$aURL != null : !this$aURL.equals(other$aURL)) {
			return false;
		}
		final Object this$aURI = this.getAURI();
		final Object other$aURI = other.getAURI();
		if (this$aURI == null ? other$aURI != null : !this$aURI.equals(other$aURI)) {
			return false;
		}
		final Object this$anEnum = this.getAnEnum();
		final Object other$anEnum = other.getAnEnum();
		if (this$anEnum == null ? other$anEnum != null : !this$anEnum.equals(other$anEnum)) {
			return false;
		}
		if (!java.util.Arrays.deepEquals(this.getAnArrayOfEnums(), other.getAnArrayOfEnums())) {
			return false;
		}
		final Object this$listOfDoubles = this.getListOfDoubles();
		final Object other$listOfDoubles = other.getListOfDoubles();
		if (this$listOfDoubles == null ? other$listOfDoubles != null : !this$listOfDoubles.equals(other$listOfDoubles)) {
			return false;
		}
		final Object this$aCollectionOfEnums = this.getACollectionOfEnums();
		final Object other$aCollectionOfEnums = other.getACollectionOfEnums();
		if (this$aCollectionOfEnums == null ? other$aCollectionOfEnums != null : !this$aCollectionOfEnums.equals(other$aCollectionOfEnums)) {
			return false;
		}
		final Object this$aTimeZone = this.getATimeZone();
		final Object other$aTimeZone = other.getATimeZone();
		if (this$aTimeZone == null ? other$aTimeZone != null : !this$aTimeZone.equals(other$aTimeZone)) {
			return false;
		}
		final Object this$aZoneId = this.getAZoneId();
		final Object other$aZoneId = other.getAZoneId();
		if (this$aZoneId == null ? other$aZoneId != null : !this$aZoneId.equals(other$aZoneId)) {
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
		return other instanceof ThingWithAllAdditionalTypes;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		result = result * PRIME + java.util.Arrays.hashCode(this.getBooleanArray());
		result = result * PRIME + this.getAByte();
		result = result * PRIME + this.getAChar();
		result = result * PRIME + java.util.Arrays.hashCode(this.getCharArray());
		final Object $aDate = this.getADate();
		result = result * PRIME + ($aDate == null ? 43 : $aDate.hashCode());
		final Object $aBigDecimal = this.getABigDecimal();
		result = result * PRIME + ($aBigDecimal == null ? 43 : $aBigDecimal.hashCode());
		final Object $aBigInteger = this.getABigInteger();
		result = result * PRIME + ($aBigInteger == null ? 43 : $aBigInteger.hashCode());
		result = result * PRIME + java.util.Arrays.hashCode(this.getDoubleArray());
		result = result * PRIME + Float.floatToIntBits(this.getAFloat());
		result = result * PRIME + java.util.Arrays.hashCode(this.getFloatArray());
		result = result * PRIME + this.getAnInt();
		result = result * PRIME + java.util.Arrays.hashCode(this.getIntArray());
		final Object $aLocale = this.getALocale();
		result = result * PRIME + ($aLocale == null ? 43 : $aLocale.hashCode());
		result = result * PRIME + java.util.Arrays.hashCode(this.getLongArray());
		result = result * PRIME + this.getAShort();
		result = result * PRIME + java.util.Arrays.hashCode(this.getShortArray());
		final Object $aPeriod = this.getAPeriod();
		result = result * PRIME + ($aPeriod == null ? 43 : $aPeriod.hashCode());
		final Object $aDuration = this.getADuration();
		result = result * PRIME + ($aDuration == null ? 43 : $aDuration.hashCode());
		result = result * PRIME + java.util.Arrays.deepHashCode(this.getStringArray());
		final Object $listOfStrings = this.getListOfStrings();
		result = result * PRIME + ($listOfStrings == null ? 43 : $listOfStrings.hashCode());
		final Object $setOfStrings = this.getSetOfStrings();
		result = result * PRIME + ($setOfStrings == null ? 43 : $setOfStrings.hashCode());
		final Object $anInstant = this.getAnInstant();
		result = result * PRIME + ($anInstant == null ? 43 : $anInstant.hashCode());
		final Object $aUUID = this.getAUUID();
		result = result * PRIME + ($aUUID == null ? 43 : $aUUID.hashCode());
		final Object $aURL = this.getAURL();
		result = result * PRIME + ($aURL == null ? 43 : $aURL.hashCode());
		final Object $aURI = this.getAURI();
		result = result * PRIME + ($aURI == null ? 43 : $aURI.hashCode());
		final Object $anEnum = this.getAnEnum();
		result = result * PRIME + ($anEnum == null ? 43 : $anEnum.hashCode());
		result = result * PRIME + java.util.Arrays.deepHashCode(this.getAnArrayOfEnums());
		final Object $listOfDoubles = this.getListOfDoubles();
		result = result * PRIME + ($listOfDoubles == null ? 43 : $listOfDoubles.hashCode());
		final Object $aCollectionOfEnums = this.getACollectionOfEnums();
		result = result * PRIME + ($aCollectionOfEnums == null ? 43 : $aCollectionOfEnums.hashCode());
		final Object $aTimeZone = this.getATimeZone();
		result = result * PRIME + ($aTimeZone == null ? 43 : $aTimeZone.hashCode());
		final Object $aZoneId = this.getAZoneId();
		result = result * PRIME + ($aZoneId == null ? 43 : $aZoneId.hashCode());
		final Object $aZeroPeriod = this.getAZeroPeriod();
		result = result * PRIME + ($aZeroPeriod == null ? 43 : $aZeroPeriod.hashCode());
		final Object $aZeroDuration = this.getAZeroDuration();
		result = result * PRIME + ($aZeroDuration == null ? 43 : $aZeroDuration.hashCode());
		return result;
	}

	public String toString() {
		return "ThingWithAllAdditionalTypes(id=" + this.getId() + ", booleanArray=" + java.util.Arrays.toString(this.getBooleanArray()) + ", aByte=" + this.getAByte() + ", aChar=" + this.getAChar() + ", charArray=" + java.util.Arrays.toString(this.getCharArray()) + ", aDate=" + this.getADate() + ", aBigDecimal=" + this.getABigDecimal() + ", aBigInteger=" + this.getABigInteger() + ", doubleArray=" + java.util.Arrays.toString(this.getDoubleArray()) + ", aFloat=" + this.getAFloat() + ", floatArray=" + java.util.Arrays.toString(this.getFloatArray()) + ", anInt=" + this.getAnInt() + ", intArray=" + java.util.Arrays.toString(this.getIntArray()) + ", aLocale=" + this.getALocale() + ", longArray=" + java.util.Arrays.toString(this.getLongArray()) + ", aShort=" + this.getAShort() + ", shortArray=" + java.util.Arrays.toString(this.getShortArray()) + ", aPeriod=" + this.getAPeriod() + ", aDuration=" + this.getADuration() + ", stringArray=" + java.util.Arrays.deepToString(this.getStringArray()) + ", listOfStrings=" + this.getListOfStrings() + ", setOfStrings=" + this.getSetOfStrings() + ", anInstant=" + this.getAnInstant() + ", aUUID=" + this.getAUUID() + ", aURL=" + this.getAURL() + ", aURI=" + this.getAURI() + ", anEnum=" + this.getAnEnum() + ", anArrayOfEnums=" + java.util.Arrays.deepToString(this.getAnArrayOfEnums()) + ", listOfDoubles=" + this.getListOfDoubles() + ", aCollectionOfEnums=" + this.getACollectionOfEnums() + ", aTimeZone=" + this.getATimeZone() + ", aZoneId=" + this.getAZoneId() + ", aZeroPeriod=" + this.getAZeroPeriod() + ", aZeroDuration=" + this.getAZeroDuration() + ")";
	}

	public ThingWithAllAdditionalTypes withId(Long id) {
		return this.id == id ? this : new ThingWithAllAdditionalTypes(id, this.booleanArray, this.aByte, this.aChar, this.charArray, this.aDate, this.aBigDecimal, this.aBigInteger, this.doubleArray, this.aFloat, this.floatArray, this.anInt, this.intArray, this.aLocale, this.longArray, this.aShort, this.shortArray, this.aPeriod, this.aDuration, this.stringArray, this.listOfStrings, this.setOfStrings, this.anInstant, this.aUUID, this.aURL, this.aURI, this.anEnum, this.anArrayOfEnums, this.listOfDoubles, this.aCollectionOfEnums, this.aTimeZone, this.aZoneId, this.aZeroPeriod, this.aZeroDuration);
	}

	enum SomeEnum {
		ValueA, ValueB, TheUsualMisfit, ValueC
	}

	@Id
	@GeneratedValue
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

	private URL aURL;

	private URI aURI;

	private SomeEnum anEnum;

	private SomeEnum[] anArrayOfEnums;

	private List<Double> listOfDoubles;

	private List<SomeEnum> aCollectionOfEnums;

	private TimeZone aTimeZone;

	private ZoneId aZoneId;

	private Period aZeroPeriod;

	private Duration aZeroDuration;

	/**
	 * the builder
	 */
	public static class ThingWithAllAdditionalTypesBuilder {
		private Long id;
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
		private URL aURL;
		private URI aURI;
		private SomeEnum anEnum;
		private SomeEnum[] anArrayOfEnums;
		private List<Double> listOfDoubles;
		private List<SomeEnum> aCollectionOfEnums;
		private TimeZone aTimeZone;
		private ZoneId aZoneId;
		private Period aZeroPeriod;
		private Duration aZeroDuration;

		ThingWithAllAdditionalTypesBuilder() {
		}

		public ThingWithAllAdditionalTypesBuilder id(Long id) {
			this.id = id;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder booleanArray(boolean[] booleanArray) {
			this.booleanArray = booleanArray;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aByte(byte aByte) {
			this.aByte = aByte;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aChar(char aChar) {
			this.aChar = aChar;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder charArray(char[] charArray) {
			this.charArray = charArray;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aDate(Date aDate) {
			this.aDate = aDate;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aBigDecimal(BigDecimal aBigDecimal) {
			this.aBigDecimal = aBigDecimal;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aBigInteger(BigInteger aBigInteger) {
			this.aBigInteger = aBigInteger;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder doubleArray(double[] doubleArray) {
			this.doubleArray = doubleArray;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aFloat(float aFloat) {
			this.aFloat = aFloat;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder floatArray(float[] floatArray) {
			this.floatArray = floatArray;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder anInt(int anInt) {
			this.anInt = anInt;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder intArray(int[] intArray) {
			this.intArray = intArray;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aLocale(Locale aLocale) {
			this.aLocale = aLocale;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder longArray(long[] longArray) {
			this.longArray = longArray;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aShort(short aShort) {
			this.aShort = aShort;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder shortArray(short[] shortArray) {
			this.shortArray = shortArray;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aPeriod(Period aPeriod) {
			this.aPeriod = aPeriod;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aDuration(Duration aDuration) {
			this.aDuration = aDuration;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder stringArray(String[] stringArray) {
			this.stringArray = stringArray;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder listOfStrings(List<String> listOfStrings) {
			this.listOfStrings = listOfStrings;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder setOfStrings(Set<String> setOfStrings) {
			this.setOfStrings = setOfStrings;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder anInstant(Instant anInstant) {
			this.anInstant = anInstant;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aUUID(UUID aUUID) {
			this.aUUID = aUUID;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aURL(URL aURL) {
			this.aURL = aURL;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aURI(URI aURI) {
			this.aURI = aURI;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder anEnum(SomeEnum anEnum) {
			this.anEnum = anEnum;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder anArrayOfEnums(SomeEnum[] anArrayOfEnums) {
			this.anArrayOfEnums = anArrayOfEnums;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder listOfDoubles(List<Double> listOfDoubles) {
			this.listOfDoubles = listOfDoubles;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aCollectionOfEnums(List<SomeEnum> aCollectionOfEnums) {
			this.aCollectionOfEnums = aCollectionOfEnums;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aTimeZone(TimeZone aTimeZone) {
			this.aTimeZone = aTimeZone;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aZoneId(ZoneId aZoneId) {
			this.aZoneId = aZoneId;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aZeroPeriod(Period aZeroPeriod) {
			this.aZeroPeriod = aZeroPeriod;
			return this;
		}

		public ThingWithAllAdditionalTypesBuilder aZeroDuration(Duration aZeroDuration) {
			this.aZeroDuration = aZeroDuration;
			return this;
		}

		public ThingWithAllAdditionalTypes build() {
			return new ThingWithAllAdditionalTypes(this.id, this.booleanArray, this.aByte, this.aChar, this.charArray, this.aDate, this.aBigDecimal, this.aBigInteger, this.doubleArray, this.aFloat, this.floatArray, this.anInt, this.intArray, this.aLocale, this.longArray, this.aShort, this.shortArray, this.aPeriod, this.aDuration, this.stringArray, this.listOfStrings, this.setOfStrings, this.anInstant, this.aUUID, this.aURL, this.aURI, this.anEnum, this.anArrayOfEnums, this.listOfDoubles, this.aCollectionOfEnums, this.aTimeZone, this.aZoneId, this.aZeroPeriod, this.aZeroDuration);
		}

		public String toString() {
			return "ThingWithAllAdditionalTypes.ThingWithAllAdditionalTypesBuilder(id=" + this.id + ", booleanArray=" + java.util.Arrays.toString(this.booleanArray) + ", aByte=" + this.aByte + ", aChar=" + this.aChar + ", charArray=" + java.util.Arrays.toString(this.charArray) + ", aDate=" + this.aDate + ", aBigDecimal=" + this.aBigDecimal + ", aBigInteger=" + this.aBigInteger + ", doubleArray=" + java.util.Arrays.toString(this.doubleArray) + ", aFloat=" + this.aFloat + ", floatArray=" + java.util.Arrays.toString(this.floatArray) + ", anInt=" + this.anInt + ", intArray=" + java.util.Arrays.toString(this.intArray) + ", aLocale=" + this.aLocale + ", longArray=" + java.util.Arrays.toString(this.longArray) + ", aShort=" + this.aShort + ", shortArray=" + java.util.Arrays.toString(this.shortArray) + ", aPeriod=" + this.aPeriod + ", aDuration=" + this.aDuration + ", stringArray=" + java.util.Arrays.deepToString(this.stringArray) + ", listOfStrings=" + this.listOfStrings + ", setOfStrings=" + this.setOfStrings + ", anInstant=" + this.anInstant + ", aUUID=" + this.aUUID + ", aURL=" + this.aURL + ", aURI=" + this.aURI + ", anEnum=" + this.anEnum + ", anArrayOfEnums=" + java.util.Arrays.deepToString(this.anArrayOfEnums) + ", listOfDoubles=" + this.listOfDoubles + ", aCollectionOfEnums=" + this.aCollectionOfEnums + ", aTimeZone=" + this.aTimeZone + ", aZoneId=" + this.aZoneId + ", aZeroPeriod=" + this.aZeroPeriod + ", aZeroDuration=" + this.aZeroDuration + ")";
		}
	}
}
