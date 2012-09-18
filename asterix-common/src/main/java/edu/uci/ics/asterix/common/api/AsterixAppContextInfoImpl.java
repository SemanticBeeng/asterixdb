package edu.uci.ics.asterix.common.api;

import java.util.Map;
import java.util.Set;

import edu.uci.ics.asterix.common.context.AsterixRuntimeComponentsProvider;
import edu.uci.ics.asterix.common.dataflow.IAsterixApplicationContextInfo;
import edu.uci.ics.hyracks.storage.am.common.api.IIndexLifecycleManagerProvider;
import edu.uci.ics.hyracks.storage.common.IStorageManagerInterface;

public class AsterixAppContextInfoImpl implements IAsterixApplicationContextInfo {

    public static final AsterixAppContextInfoImpl INSTANCE = new AsterixAppContextInfoImpl();

    private static Map<String, Set<String>> nodeControllerMap;

    private AsterixAppContextInfoImpl() {
    }

    @Override
    public IStorageManagerInterface getStorageManagerInterface() {
        return AsterixRuntimeComponentsProvider.INSTANCE;
    }

    public static void setNodeControllerInfo(Map<String, Set<String>> nodeControllerInfo) {
        nodeControllerMap = nodeControllerInfo;
    }

    public static Map<String, Set<String>> getNodeControllerMap() {
        return nodeControllerMap;
    }

    @Override
    public IIndexLifecycleManagerProvider getIndexLifecycleManagerProvider() {
        return AsterixRuntimeComponentsProvider.INSTANCE;
    }

}