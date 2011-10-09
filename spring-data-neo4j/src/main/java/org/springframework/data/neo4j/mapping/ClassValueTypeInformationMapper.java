/**
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.neo4j.mapping;

import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
* @author mh
* @since 09.10.11
*/
public class ClassValueTypeInformationMapper implements TypeInformationMapper {
    @Override
    public TypeInformation<?> resolveTypeFrom(Object alias) {
        if (!(alias instanceof Class)) {
            return null;
        }

        return ClassTypeInformation.from((Class<?>) alias);
    }

    @Override
    public Object createAliasFor(TypeInformation<?> type) {
        return type.getType();
    }
}
