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
package org.springframework.data.neo4j.test;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * See <a href="https://github.com/neo4j/neo4j-java-driver/blob/4.4/driver/src/main/java/org/neo4j/driver/internal/util/ServerVersion.java">ServerVersion.java</a>
 *
 * @author Driver Team at Neo4j
 */
public final class ServerVersion {
	public static final String NEO4J_PRODUCT = "Neo4j";

	public static final ServerVersion v5_0_0 = new ServerVersion(NEO4J_PRODUCT, 5, 0, 0);
	public static final ServerVersion v4_4_0 = new ServerVersion(NEO4J_PRODUCT, 4, 4, 0);
	public static final ServerVersion v4_3_0 = new ServerVersion(NEO4J_PRODUCT, 4, 3, 0);
	public static final ServerVersion v4_2_0 = new ServerVersion(NEO4J_PRODUCT, 4, 2, 0);
	public static final ServerVersion v4_1_0 = new ServerVersion(NEO4J_PRODUCT, 4, 1, 0);
	public static final ServerVersion v4_0_0 = new ServerVersion(NEO4J_PRODUCT, 4, 0, 0);
	public static final ServerVersion v3_5_0 = new ServerVersion(NEO4J_PRODUCT, 3, 5, 0);
	public static final ServerVersion v3_4_0 = new ServerVersion(NEO4J_PRODUCT, 3, 4, 0);
	public static final ServerVersion vInDev = new ServerVersion(NEO4J_PRODUCT, Integer.MAX_VALUE, Integer.MAX_VALUE,
			Integer.MAX_VALUE);

	private static final String NEO4J_IN_DEV_VERSION_STRING = NEO4J_PRODUCT + "/dev";
	private static final Pattern PATTERN =
			Pattern.compile("([^/]+)/(\\d+)\\.(\\d+)\\.?(\\d*)([.\\-+])?([\\dA-Za-z-.]*)?");

	private final String product;
	private final int major;
	private final int minor;
	private final int patch;
	private final String stringValue;

	private ServerVersion(String product, int major, int minor, int patch) {
		this.product = product;
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.stringValue = stringValue(product, major, minor, patch);
	}

	public static ServerVersion version(String server) {
		Matcher matcher = PATTERN.matcher(server);
		if (matcher.matches()) {
			String product = matcher.group(1);
			int major = Integer.parseInt(matcher.group(2));
			int minor = Integer.parseInt(matcher.group(3));
			String patchString = matcher.group(4);
			int patch = 0;
			if (patchString != null && !patchString.isEmpty()) {
				patch = Integer.parseInt(patchString);
			}
			return new ServerVersion(product, major, minor, patch);
		} else if (server.equalsIgnoreCase(NEO4J_IN_DEV_VERSION_STRING)) {
			return vInDev;
		} else {
			throw new IllegalArgumentException("Cannot parse " + server);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ServerVersion that = (ServerVersion) o;

		if (!product.equals(that.product)) {
			return false;
		}
		if (major != that.major) {
			return false;
		}
		if (minor != that.minor) {
			return false;
		}
		return patch == that.patch;
	}

	@Override
	public int hashCode() {
		return Objects.hash(product, major, minor, patch);
	}

	public boolean greaterThan(ServerVersion other) {
		return compareTo(other) > 0;
	}

	public boolean greaterThanOrEqual(ServerVersion other) {
		return compareTo(other) >= 0;
	}

	public boolean lessThan(ServerVersion other) {
		return compareTo(other) < 0;
	}

	public boolean lessThanOrEqual(ServerVersion other) {
		return compareTo(other) <= 0;
	}

	private int compareTo(ServerVersion o) {
		if (!product.equals(o.product)) {
			throw new IllegalArgumentException(
					"Comparing different products '" + product + "' with '" + o.product + "'");
		}
		int c = Integer.compare(major, o.major);
		if (c == 0) {
			c = Integer.compare(minor, o.minor);
			if (c == 0) {
				c = Integer.compare(patch, o.patch);
			}
		}

		return c;
	}

	@Override
	public String toString() {
		return stringValue;
	}

	private static String stringValue(String product, int major, int minor, int patch) {
		if (major == Integer.MAX_VALUE && minor == Integer.MAX_VALUE && patch == Integer.MAX_VALUE) {
			return NEO4J_IN_DEV_VERSION_STRING;
		}
		return String.format("%s/%s.%s.%s", product, major, minor, patch);
	}
}
