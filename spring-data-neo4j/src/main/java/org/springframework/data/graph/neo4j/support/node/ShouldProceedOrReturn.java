/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.support.node;

/**
 * @author Michael Hunger
 * @since 29.08.2010
 */
public class ShouldProceedOrReturn {
    public final boolean proceed;
    public final Object value;

    public ShouldProceedOrReturn() {
        this.proceed = true;
        this.value = null;
    }

    public ShouldProceedOrReturn(final Object value) {
        this.proceed = false;
        this.value = value;
    }

    public ShouldProceedOrReturn(final boolean proceed, final Object value) {
        this.proceed = proceed;
        this.value = value;
    }
}
