/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.hivesterix.serde.lazy;

import org.apache.hadoop.io.LongWritable;

import edu.uci.ics.hivesterix.serde.lazy.LazyUtils.VLong;
import edu.uci.ics.hivesterix.serde.lazy.objectinspector.primitive.LazyLongObjectInspector;

/**
 * LazyObject for storing a value of Long.
 * 
 * <p>
 * Part of the code is adapted from Apache Harmony Project.
 * 
 * As with the specification, this implementation relied on code laid out in <a
 * href="http://www.hackersdelight.org/">Henry S. Warren, Jr.'s Hacker's
 * Delight, (Addison Wesley, 2002)</a> as well as <a
 * href="http://aggregate.org/MAGIC/">The Aggregate's Magic Algorithms</a>.
 * </p>
 * 
 */
public class LazyLong extends
		LazyPrimitive<LazyLongObjectInspector, LongWritable> {

	public LazyLong(LazyLongObjectInspector oi) {
		super(oi);
		data = new LongWritable();
	}

	public LazyLong(LazyLong copy) {
		super(copy);
		data = new LongWritable(copy.data.get());
	}

	/**
	 * The reusable vLong for decoding the long.
	 */
	VLong vLong = new LazyUtils.VLong();

	@Override
	public void init(byte[] bytes, int start, int length) {
		if (length == 0) {
			isNull = true;
			return;
		} else
			isNull = false;

		LazyUtils.readVLong(bytes, start, vLong);
		assert (length == vLong.length);
		if (length != vLong.length)
			throw new IllegalStateException("parse long: length mismatch");
		data.set(vLong.value);
	}

}