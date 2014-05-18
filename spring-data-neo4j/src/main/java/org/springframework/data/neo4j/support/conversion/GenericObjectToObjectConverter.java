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

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

/**
 * Performs Generic Fallback Object to Object conversion
 *
 */
public final class GenericObjectToObjectConverter implements GenericConverter {

    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
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

    private static Method getValueOfMethodOn(Class<?> clazz, Class<?> sourceParameterType) {
        return ClassUtils.getStaticMethod(clazz, "valueOf", sourceParameterType);
    }

    private static Constructor<?> getConstructor(Class<?> clazz, Class<?> sourceParameterType) {
        return ClassUtils.getConstructorIfAvailable(clazz, sourceParameterType);
    }

}

