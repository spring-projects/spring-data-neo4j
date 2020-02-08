/*
 * Copyright 2011-2020 the original author or authors.
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
