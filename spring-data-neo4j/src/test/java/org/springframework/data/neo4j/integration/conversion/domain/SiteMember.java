/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
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
