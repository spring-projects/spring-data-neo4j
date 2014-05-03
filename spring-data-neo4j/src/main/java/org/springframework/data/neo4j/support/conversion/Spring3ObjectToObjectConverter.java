package org.springframework.data.neo4j.support.conversion;

/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.springframework.core.convert.converter.ConditionalGenericConverter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * This class exists (possibly only as an interim measure) to re-introduce some
 * previously lost functionality as a result of moving from Spring 3.2.X - > 4.X,
 * specifically providing the ability to be backwards compatible with regards
 * to the Object -> String conversion scenario. This functionality changed in
 * Spring 4.X and there is current debate as to whether this is a regression
 * or intended behaviour. For more information, please
 * see https://jira.spring.io/browse/SPR-11693, however in the interim this
 * class will provide the original functionality so that when used in a Spring4
 * context, the functionality should still work.
 *
 * This is basically a copy of the older Spring 3.2.8
 * {@link org.springframework.core.convert.support.ObjectToObjectConverter} class.
 *
 */
public final class Spring3ObjectToObjectConverter implements ConditionalGenericConverter {

    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
    }

    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (sourceType.getType().equals(targetType.getType())) {
            // no conversion required
            return false;
        }
        return hasValueOfMethodOrConstructor(targetType.getType(), sourceType.getType());
    }

    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        Class<?> sourceClass = sourceType.getType();
        Class<?> targetClass = targetType.getType();
        Method method = getValueOfMethodOn(targetClass, sourceClass);
        try {
            if (method != null) {
                ReflectionUtils.makeAccessible(method);
                return method.invoke(null, source);
            }
            else {
                Constructor<?> constructor = getConstructor(targetClass, sourceClass);
                if (constructor != null) {
                    return constructor.newInstance(source);
                }
            }
        }
        catch (InvocationTargetException ex) {
            throw new ConversionFailedException(sourceType, targetType, source, ex.getTargetException());
        }
        catch (Throwable ex) {
            throw new ConversionFailedException(sourceType, targetType, source, ex);
        }
        throw new IllegalStateException("No static valueOf(" + sourceClass.getName() +
                ") method or Constructor(" + sourceClass.getName() + ") exists on " + targetClass.getName());
    }

    static boolean hasValueOfMethodOrConstructor(Class<?> clazz, Class<?> sourceParameterType) {
        return getValueOfMethodOn(clazz, sourceParameterType) != null || getConstructor(clazz, sourceParameterType) != null;
    }

    private static Method getValueOfMethodOn(Class<?> clazz, Class<?> sourceParameterType) {
        return ClassUtils.getStaticMethod(clazz, "valueOf", sourceParameterType);
    }

    private static Constructor<?> getConstructor(Class<?> clazz, Class<?> sourceParameterType) {
        return ClassUtils.getConstructorIfAvailable(clazz, sourceParameterType);
    }

}

