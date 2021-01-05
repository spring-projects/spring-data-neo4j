/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.integration.conversion.domain;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.ByteArrayBase64Converter;

/**
 * @author Adam George
 * @author Luanne Misquitta
 * @author Michael J. Simons
 */
public class SiteMember {

	@Id @GeneratedValue private Long id;

	@Convert(ByteArrayBase64Converter.class) private byte[] profilePictureData;

	private BigInteger years;

	@Property(name = "rounding") private List<RoundingMode> roundingModes;

	public byte[] getProfilePictureData() {
		return this.profilePictureData;
	}

	public void setProfilePictureData(byte[] profilePictureData) {
		this.profilePictureData = profilePictureData;
	}

	public BigInteger getYears() {
		return years;
	}

	public void setYears(BigInteger years) {
		this.years = years;
	}

	public List<RoundingMode> getRoundingModes() {
		return roundingModes;
	}

	public void setRoundingModes(List<RoundingMode> roundingModes) {
		this.roundingModes = roundingModes;
	}
}
