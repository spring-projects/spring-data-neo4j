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

package org.springframework.datastore.graph.neo4j.support;

/**
 * @author Michael Hunger
 * @since 11.09.2010
 */
public final class Tuple2<T1,T2> {
	public final T1 _1;
	public final T2 _2;

	private Tuple2(T1 _1,T2 _2) {
		this._1=_1;
		this._2=_2;
	}
	public static <T1,T2>  Tuple2<T1,T2> _(T1 _1, T2 _2) {
		return new Tuple2<T1,T2>(_1,_2);
	}

    @Override
    public String toString() {
        return String.format("(%s,%s)",_1,_2);
    }
}