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

import java.io.Serializable;
import java.util.*;

import static java.util.Arrays.asList;

/**
 * @author mh
 * @since 09.11.11
 */
public interface MappingPolicy {

    enum Option {
        FIELD_DIRECT, SHOULD_LOAD
    }
    boolean accessField();
    boolean shouldLoad();
    MappingPolicy combineWith(MappingPolicy mappingPolicy);

    public class DefaultMappingPolicy implements MappingPolicy , Serializable {

        private static final long serialVersionUID = 1L;

        private Set<Option> options;

        public DefaultMappingPolicy(Option... options) {
            this(asList(options));
        }

        public DefaultMappingPolicy(final Collection<Option> options) {
            this.options = options.isEmpty() ? EnumSet.noneOf(Option.class) : EnumSet.copyOf(options);
        }

        @Override
        public boolean accessField() {
            return options.contains(Option.FIELD_DIRECT);
        }

        @Override
        public boolean shouldLoad() {
            return options.contains(Option.SHOULD_LOAD);
        }

        public MappingPolicy with(Option...options) {
            return with(asList(options));
        }

        private MappingPolicy with(Collection<Option> optionsList) {
            Collection<Option> combined =new HashSet<Option>(optionsList);
            combined.addAll(this.options);
            combined.remove(null);
            return new DefaultMappingPolicy(combined);
        }

        public MappingPolicy withOut(Option...options) {
            Collection<Option> combined =new HashSet<Option>(this.options);
            combined.removeAll(asList(options));
            return new DefaultMappingPolicy(combined);
        }

        @Override
        public MappingPolicy combineWith(MappingPolicy mappingPolicy) {
            if (mappingPolicy instanceof DefaultMappingPolicy) {
                return with(((DefaultMappingPolicy)mappingPolicy).options);
            }
            return with(mappingPolicy.accessField() ? Option.FIELD_DIRECT : null, mappingPolicy.shouldLoad() ? Option.SHOULD_LOAD : null);
        }

        @Override
        public String toString() {
            return "Policy: "+options.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DefaultMappingPolicy that = (DefaultMappingPolicy) o;

            return options.equals(that.options);

        }

        @Override
        public int hashCode() {
            return options.hashCode();
        }
    }

    public MappingPolicy LOAD_POLICY = new DefaultMappingPolicy(Option.SHOULD_LOAD);
    public MappingPolicy DEFAULT_POLICY = new DefaultMappingPolicy();
    public MappingPolicy MAP_FIELD_DIRECT_POLICY = new DefaultMappingPolicy(Option.FIELD_DIRECT);
}
