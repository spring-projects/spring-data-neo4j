/*
 * Copyright (c) 2023-2024 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.springframework.data.falkordb.core.schema;

import org.apiguardian.api.API;

/**
 * Internal ID generator that relies on FalkorDB's internal ID mechanism. This generator
 * does not actually generate IDs but serves as a marker to indicate that FalkorDB's
 * internal ID should be used.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class InternalIdGenerator implements IdGenerator<Long> {

	@Override
	public Long generateId(String primaryLabel, Object entity) {
		// This should never be called as it's handled internally by the framework
		throw new UnsupportedOperationException("Internal ID generation is handled by FalkorDB");
	}

}
