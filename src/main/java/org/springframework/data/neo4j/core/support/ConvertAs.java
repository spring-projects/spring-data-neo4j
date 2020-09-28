package org.springframework.data.neo4j.core.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import org.neo4j.driver.Value;
import org.springframework.core.convert.converter.Converter;

/**
 * @soundtrack Antilopen Gang - Abwasser
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Inherited
@Documented
public @interface ConvertAs {

	Class<? extends Function<?, Value>> writingConverter();

	Class<? extends Function<Value, ?>> readingConverter();
}
