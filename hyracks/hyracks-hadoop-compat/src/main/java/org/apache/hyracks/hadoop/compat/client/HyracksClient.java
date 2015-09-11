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
package org.apache.hyracks.hadoop.compat.client;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.hyracks.api.client.HyracksConnection;
import org.apache.hyracks.api.job.JobFlag;
import org.apache.hyracks.api.job.JobId;
import org.apache.hyracks.api.job.JobSpecification;
import org.apache.hyracks.api.job.JobStatus;
import org.apache.hyracks.hadoop.compat.util.ConfigurationConstants;
import org.apache.hyracks.hadoop.compat.util.Utilities;

public class HyracksClient {

    private static HyracksConnection connection;
    private static final String jobProfilingKey = "jobProfilingKey";
    Set<String> systemLibs;

    public HyracksClient(Properties clusterProperties) throws Exception {
        initialize(clusterProperties);
    }

    private void initialize(Properties properties) throws Exception {
        String clusterController = (String) properties.get(ConfigurationConstants.clusterControllerHost);
        connection = new HyracksConnection(clusterController, 1098);
        systemLibs = new HashSet<String>();
        for (String systemLib : ConfigurationConstants.systemLibs) {
            String systemLibPath = properties.getProperty(systemLib);
            if (systemLibPath != null) {
                systemLibs.add(systemLibPath);
            }
        }
    }

    public HyracksClient(String clusterConf, char delimiter) throws Exception {
        Properties properties = Utilities.getProperties(clusterConf, delimiter);
        initialize(properties);
    }

    public JobStatus getJobStatus(JobId jobId) throws Exception {
        return connection.getJobStatus(jobId);
    }

    public HyracksRunningJob submitJob(String applicationName, JobSpecification spec) throws Exception {
        String jobProfilingVal = System.getenv(jobProfilingKey);
        boolean doProfiling = ("true".equalsIgnoreCase(jobProfilingVal));
        JobId jobId;
        if (doProfiling) {
            System.out.println("PROFILING");
            jobId = connection.startJob(spec, EnumSet.of(JobFlag.PROFILE_RUNTIME));
        } else {
            jobId = connection.startJob(spec);
        }
        HyracksRunningJob runningJob = new HyracksRunningJob(jobId, spec, this);
        return runningJob;
    }

    public HyracksRunningJob submitJob(String applicationName, JobSpecification spec, Set<String> userLibs)
            throws Exception {
        return submitJob(applicationName, spec);
    }

    public void waitForCompleton(JobId jobId) throws Exception {
        connection.waitForCompletion(jobId);
    }
}
