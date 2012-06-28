/*
 * Copyright 2009-2010 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.uci.ics.hyracks.storage.am.lsm.rtree.impls;

import java.util.concurrent.atomic.AtomicInteger;

import edu.uci.ics.hyracks.api.dataflow.value.IBinaryComparatorFactory;
import edu.uci.ics.hyracks.storage.am.common.api.ICursorInitialState;
import edu.uci.ics.hyracks.storage.am.common.api.ISearchOperationCallback;
import edu.uci.ics.hyracks.storage.am.common.api.ITreeIndexAccessor;
import edu.uci.ics.hyracks.storage.am.common.api.ITreeIndexFrameFactory;
import edu.uci.ics.hyracks.storage.am.common.ophelpers.MultiComparator;
import edu.uci.ics.hyracks.storage.am.lsm.common.impls.LSMHarness;
import edu.uci.ics.hyracks.storage.common.buffercache.ICachedPage;

public class LSMRTreeCursorInitialState implements ICursorInitialState {

    private int numberOfTrees;
    private ITreeIndexFrameFactory rtreeInteriorFrameFactory;
    private ITreeIndexFrameFactory rtreeLeafFrameFactory;
    private ITreeIndexFrameFactory btreeLeafFrameFactory;
    private MultiComparator btreeCmp;
    private MultiComparator hilbertCmp;
    private ITreeIndexAccessor[] rTreeAccessors;
    private ITreeIndexAccessor[] bTreeAccessors;
    private AtomicInteger searcherRefCount;
    private final boolean includeMemRTree;
    private final LSMHarness lsmHarness;
    private final int[] comparatorFields;

    private ISearchOperationCallback searchCallback;

    public LSMRTreeCursorInitialState(int numberOfTrees, ITreeIndexFrameFactory rtreeLeafFrameFactory,
            ITreeIndexFrameFactory rtreeInteriorFrameFactory, ITreeIndexFrameFactory btreeLeafFrameFactory,
            MultiComparator btreeCmp, ITreeIndexAccessor[] rTreeAccessors, ITreeIndexAccessor[] bTreeAccessors,
            AtomicInteger searcherRefCount, boolean includeMemRTree, LSMHarness lsmHarness, int[] comparatorFields,
            IBinaryComparatorFactory[] linearizerArray, ISearchOperationCallback searchCallback) {
        this.numberOfTrees = numberOfTrees;
        this.rtreeLeafFrameFactory = rtreeLeafFrameFactory;
        this.rtreeInteriorFrameFactory = rtreeInteriorFrameFactory;
        this.btreeLeafFrameFactory = btreeLeafFrameFactory;
        this.btreeCmp = btreeCmp;
        this.rTreeAccessors = rTreeAccessors;
        this.bTreeAccessors = bTreeAccessors;
        this.searcherRefCount = searcherRefCount;
        this.includeMemRTree = includeMemRTree;
        this.lsmHarness = lsmHarness;
        this.comparatorFields = comparatorFields;
        this.hilbertCmp = MultiComparator.create(linearizerArray);
        this.searchCallback = searchCallback;
    }

    public MultiComparator getHilbertCmp() {
        return hilbertCmp;
    }

    public int[] getComparatorFields() {
        return comparatorFields;
    }

    public int getNumberOfTrees() {
        return numberOfTrees;
    }

    public ITreeIndexFrameFactory getRTreeInteriorFrameFactory() {
        return rtreeInteriorFrameFactory;
    }

    public ITreeIndexFrameFactory getRTreeLeafFrameFactory() {
        return rtreeLeafFrameFactory;
    }

    public ITreeIndexFrameFactory getBTreeLeafFrameFactory() {
        return btreeLeafFrameFactory;
    }

    public MultiComparator getBTreeCmp() {
        return btreeCmp;
    }

    @Override
    public ICachedPage getPage() {
        return null;
    }

    @Override
    public void setPage(ICachedPage page) {
    }

    public ITreeIndexAccessor[] getRTreeAccessors() {
        return rTreeAccessors;
    }

    public ITreeIndexAccessor[] getBTreeAccessors() {
        return bTreeAccessors;
    }

    public boolean getIncludeMemComponent() {
        return includeMemRTree;
    }

    public AtomicInteger getSearcherRefCount() {
        return searcherRefCount;
    }

    public LSMHarness getLSMHarness() {
        return lsmHarness;
    }

    @Override
    public ISearchOperationCallback getSearchOperationCallback() {
        return searchCallback;
    }

    @Override
    public void setSearchOperationCallback(ISearchOperationCallback searchCallback) {
        this.searchCallback = searchCallback;
    }

}