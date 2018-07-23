/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.conversion;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.typeconversion.CompositeAttributeConverter;
import org.springframework.data.geo.Point;

/**
 * Converts latitude and longitude properties on a node entity to an instance of Point and vice-versa.
 *
 * @see Point
 * @author Jasper Blues
 */
public class PointConverter implements CompositeAttributeConverter<Point> {

	@Override
	public Map<String, ?> toGraphProperties(Point point) {
		Map<String, Double> properties = new HashMap<>();
		if (point != null) {
			properties.put("latitude", point.getX());
			properties.put("longitude", point.getY());
		}
		return properties;
	}

	@Override
	public Point toEntityAttribute(Map<String, ?> map) {
		Double latitude = (Double) map.get("latitude");
		Double longitude = (Double) map.get("longitude");
		if (latitude != null && longitude != null) {
			return new Point(latitude, longitude);
		}
		return null;
	}

}
