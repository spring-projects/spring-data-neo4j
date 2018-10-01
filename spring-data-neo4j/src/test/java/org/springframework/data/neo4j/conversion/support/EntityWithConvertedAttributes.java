package org.springframework.data.neo4j.conversion.support;

import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.typeconversion.Convert;

@NodeEntity
public class EntityWithConvertedAttributes {
	@Id @GeneratedValue private Long id;

	private String name;

	@Property
	@Convert(Converters.ConvertedClassToStringConverter.class)
	private ConvertedClass convertedClass;

	@Property
	@Convert(Converters.ListToStringConverter.class)
	private List<Double> doubles;

	@Convert(Converters.DoubleToStringConverter.class)
	private Double theDouble;

	public EntityWithConvertedAttributes() {}

	public EntityWithConvertedAttributes(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setConvertedClass(ConvertedClass convertedClass) {
		this.convertedClass = convertedClass;
	}

	public void setDoubles(List<Double> doubles) {
		this.doubles = doubles;
	}

	public void setTheDouble(Double theDouble) {
		this.theDouble = theDouble;
	}
}
