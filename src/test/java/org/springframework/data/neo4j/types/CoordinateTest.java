package org.springframework.data.neo4j.types;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class CoordinateTest {

	@Test
	public void equalsFalse(){

		//Arrange
		Coordinate coordinate = new Coordinate(23, 54);

		//Act & Assert
		assertEquals(coordinate.equals(new Object()), false);
	}

	@Test
	public void equalsTrue(){

		//Arrange
		Coordinate coordinate = new Coordinate(23, 54);
		Coordinate coordinateTest = new Coordinate(23, 54);

		//Act & //Assert
		assertEquals(coordinate.equals(coordinateTest), true);
	}
}
