/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.asterix.runtime.evaluators.functions;

import java.io.DataOutput;
import java.io.IOException;

import org.apache.asterix.formats.nontagged.AqlSerializerDeserializerProvider;
import org.apache.asterix.om.base.ABoolean;
import org.apache.asterix.om.functions.AsterixBuiltinFunctions;
import org.apache.asterix.om.types.ATypeTag;
import org.apache.asterix.om.types.BuiltinType;
import org.apache.asterix.om.types.EnumDeserializer;
import org.apache.asterix.om.types.hierachy.ATypeHierarchy;
import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluator;
import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluatorFactory;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.dataflow.value.ISerializerDeserializer;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.data.std.api.IPointable;
import org.apache.hyracks.data.std.primitive.BooleanPointable;
import org.apache.hyracks.data.std.primitive.UTF8StringPointable;
import org.apache.hyracks.data.std.primitive.VoidPointable;
import org.apache.hyracks.data.std.util.ArrayBackedValueStorage;
import org.apache.hyracks.dataflow.common.data.accessors.IFrameTupleReference;

public class EditDistanceStringIsFilterableEvaluator implements IScalarEvaluator {

    protected final ArrayBackedValueStorage resultStorage = new ArrayBackedValueStorage();
    protected final DataOutput output = resultStorage.getDataOutput();
    protected final IPointable stringPtr = new VoidPointable();
    protected final IPointable edThreshPtr = new VoidPointable();
    protected final IPointable gramLenPtr = new VoidPointable();
    protected final IPointable usePrePostPtr = new VoidPointable();

    protected final IScalarEvaluator stringEval;
    protected final IScalarEvaluator edThreshEval;
    protected final IScalarEvaluator gramLenEval;
    protected final IScalarEvaluator usePrePostEval;

    @SuppressWarnings("unchecked")
    private final ISerializerDeserializer<ABoolean> booleanSerde = AqlSerializerDeserializerProvider.INSTANCE
            .getSerializerDeserializer(BuiltinType.ABOOLEAN);

    private final UTF8StringPointable utf8Ptr = new UTF8StringPointable();

    public EditDistanceStringIsFilterableEvaluator(IScalarEvaluatorFactory[] args, IHyracksTaskContext context)
            throws AlgebricksException {
        stringEval = args[0].createScalarEvaluator(context);
        edThreshEval = args[1].createScalarEvaluator(context);
        gramLenEval = args[2].createScalarEvaluator(context);
        usePrePostEval = args[3].createScalarEvaluator(context);
    }

    @Override
    public void evaluate(IFrameTupleReference tuple, IPointable result) throws AlgebricksException {
        resultStorage.reset();

        stringEval.evaluate(tuple, stringPtr);
        edThreshEval.evaluate(tuple, edThreshPtr);
        gramLenEval.evaluate(tuple, gramLenPtr);
        usePrePostEval.evaluate(tuple, usePrePostPtr);

        // Check type and compute string length.
        ATypeTag typeTag = EnumDeserializer.ATYPETAGDESERIALIZER
                .deserialize(stringPtr.getByteArray()[stringPtr.getStartOffset()]);
        if (!typeTag.equals(ATypeTag.STRING)) {
            throw new AlgebricksException(AsterixBuiltinFunctions.EDIT_DISTANCE_STRING_IS_FILTERABLE.getName()
                    + ": expects input type STRING as first argument, but got " + typeTag + ".");
        }
        utf8Ptr.set(stringPtr.getByteArray(), stringPtr.getStartOffset() + 1, stringPtr.getLength());
        int strLen = utf8Ptr.getStringLength();

        // Check type and extract edit-distance threshold.
        long edThresh = 0;
        try {
            edThresh = ATypeHierarchy.getIntegerValue(edThreshPtr.getByteArray(), edThreshPtr.getStartOffset());
        } catch (HyracksDataException e1) {
            throw new AlgebricksException(e1);
        }

        // Check type and extract gram length.
        long gramLen = 0;
        try {
            gramLen = ATypeHierarchy.getIntegerValue(gramLenPtr.getByteArray(), gramLenPtr.getStartOffset());
        } catch (HyracksDataException e1) {
            throw new AlgebricksException(e1);
        }

        // Check type and extract usePrePost flag.
        typeTag = EnumDeserializer.ATYPETAGDESERIALIZER
                .deserialize(usePrePostPtr.getByteArray()[usePrePostPtr.getStartOffset()]);
        if (!typeTag.equals(ATypeTag.BOOLEAN)) {
            throw new AlgebricksException(AsterixBuiltinFunctions.EDIT_DISTANCE_STRING_IS_FILTERABLE.getName()
                    + ": expects input type BOOLEAN as fourth argument, but got " + typeTag + ".");
        }
        boolean usePrePost = BooleanPointable.getBoolean(usePrePostPtr.getByteArray(),
                usePrePostPtr.getStartOffset() + 1);

        // Compute result.
        long numGrams = usePrePost ? strLen + gramLen - 1 : strLen - gramLen + 1;
        long lowerBound = numGrams - edThresh * gramLen;
        try {
            if (lowerBound <= 0 || strLen == 0) {
                booleanSerde.serialize(ABoolean.FALSE, output);
            } else {
                booleanSerde.serialize(ABoolean.TRUE, output);
            }
        } catch (IOException e) {
            throw new AlgebricksException(e);
        }
        result.set(resultStorage);
    }
}