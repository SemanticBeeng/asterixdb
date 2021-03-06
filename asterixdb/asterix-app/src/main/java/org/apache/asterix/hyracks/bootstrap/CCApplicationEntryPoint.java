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
package org.apache.asterix.hyracks.bootstrap;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.asterix.api.http.servlet.APIServlet;
import org.apache.asterix.api.http.servlet.AQLAPIServlet;
import org.apache.asterix.api.http.servlet.ClusterAPIServlet;
import org.apache.asterix.api.http.servlet.ConnectorAPIServlet;
import org.apache.asterix.api.http.servlet.DDLAPIServlet;
import org.apache.asterix.api.http.servlet.FeedServlet;
import org.apache.asterix.api.http.servlet.QueryAPIServlet;
import org.apache.asterix.api.http.servlet.QueryResultAPIServlet;
import org.apache.asterix.api.http.servlet.QueryServiceServlet;
import org.apache.asterix.api.http.servlet.QueryStatusAPIServlet;
import org.apache.asterix.api.http.servlet.ShutdownAPIServlet;
import org.apache.asterix.api.http.servlet.UpdateAPIServlet;
import org.apache.asterix.api.http.servlet.VersionAPIServlet;
import org.apache.asterix.app.external.ExternalLibraryUtils;
import org.apache.asterix.app.external.FeedLifecycleListener;
import org.apache.asterix.common.api.AsterixThreadFactory;
import org.apache.asterix.common.api.IClusterManagementWork.ClusterState;
import org.apache.asterix.common.config.AsterixExternalProperties;
import org.apache.asterix.common.config.AsterixMetadataProperties;
import org.apache.asterix.common.utils.ServletUtil.Servlets;
import org.apache.asterix.compiler.provider.AqlCompilationProvider;
import org.apache.asterix.compiler.provider.SqlppCompilationProvider;
import org.apache.asterix.event.service.ILookupService;
import org.apache.asterix.messaging.CCMessageBroker;
import org.apache.asterix.metadata.MetadataManager;
import org.apache.asterix.metadata.api.IAsterixStateProxy;
import org.apache.asterix.metadata.bootstrap.AsterixStateProxy;
import org.apache.asterix.metadata.cluster.ClusterManager;
import org.apache.asterix.om.util.AsterixAppContextInfo;
import org.apache.asterix.om.util.AsterixClusterProperties;
import org.apache.hyracks.api.application.ICCApplicationContext;
import org.apache.hyracks.api.application.ICCApplicationEntryPoint;
import org.apache.hyracks.api.client.HyracksConnection;
import org.apache.hyracks.api.client.IHyracksClientConnection;
import org.apache.hyracks.api.lifecycle.LifeCycleComponentManager;
import org.apache.hyracks.api.messages.IMessageBroker;
import org.apache.hyracks.control.cc.ClusterControllerService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class CCApplicationEntryPoint implements ICCApplicationEntryPoint {

    private static final Logger LOGGER = Logger.getLogger(CCApplicationEntryPoint.class.getName());
    private static final String HYRACKS_CONNECTION_ATTR = "org.apache.asterix.HYRACKS_CONNECTION";
    private static final String ASTERIX_BUILD_PROP_ATTR = "org.apache.asterix.PROPS";

    private Server webServer;
    private Server jsonAPIServer;
    private Server feedServer;

    private static IAsterixStateProxy proxy;
    private ICCApplicationContext appCtx;
    private IMessageBroker messageBroker;

    @Override
    public void start(ICCApplicationContext ccAppCtx, String[] args) throws Exception {
        messageBroker = new CCMessageBroker((ClusterControllerService) ccAppCtx.getControllerService());
        this.appCtx = ccAppCtx;

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Starting Asterix cluster controller");
        }

        appCtx.setThreadFactory(new AsterixThreadFactory(new LifeCycleComponentManager()));
        GlobalRecoveryManager.INSTANCE = new GlobalRecoveryManager((HyracksConnection) getNewHyracksClientConnection());

        AsterixAppContextInfo.initialize(appCtx, getNewHyracksClientConnection(), GlobalRecoveryManager.INSTANCE);

        proxy = AsterixStateProxy.registerRemoteObject();
        appCtx.setDistributedState(proxy);

        AsterixMetadataProperties metadataProperties = AsterixAppContextInfo.getInstance().getMetadataProperties();
        MetadataManager.INSTANCE = new MetadataManager(proxy, metadataProperties);

        AsterixAppContextInfo.getInstance().getCCApplicationContext()
                .addJobLifecycleListener(FeedLifecycleListener.INSTANCE);

        AsterixExternalProperties externalProperties = AsterixAppContextInfo.getInstance().getExternalProperties();
        setupWebServer(externalProperties);
        webServer.start();
        setupJSONAPIServer(externalProperties);
        jsonAPIServer.start();

        setupFeedServer(externalProperties);
        feedServer.start();
        ExternalLibraryUtils.setUpExternaLibraries(false);
        ClusterManager.INSTANCE.registerSubscriber(GlobalRecoveryManager.INSTANCE);

        ccAppCtx.addClusterLifecycleListener(ClusterLifecycleListener.INSTANCE);
        ccAppCtx.setMessageBroker(messageBroker);
    }

    @Override
    public void stop() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Stopping Asterix cluster controller");
        }
        AsterixStateProxy.unregisterRemoteObject();
        // Stop servers
        webServer.stop();
        jsonAPIServer.stop();
        feedServer.stop();
        // Make sure servers are stopped before proceeding
        webServer.join();
        jsonAPIServer.join();
        feedServer.join();
    }

    private IHyracksClientConnection getNewHyracksClientConnection() throws Exception {
        String strIP = appCtx.getCCContext().getClusterControllerInfo().getClientNetAddress();
        int port = appCtx.getCCContext().getClusterControllerInfo().getClientNetPort();
        return new HyracksConnection(strIP, port);
    }

    private void setupWebServer(AsterixExternalProperties externalProperties) throws Exception {

        webServer = new Server(externalProperties.getWebInterfacePort());

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        IHyracksClientConnection hcc = getNewHyracksClientConnection();
        context.setAttribute(HYRACKS_CONNECTION_ATTR, hcc);

        webServer.setHandler(context);
        context.addServlet(new ServletHolder(new APIServlet()), "/*");
    }

    private void setupJSONAPIServer(AsterixExternalProperties externalProperties) throws Exception {
        jsonAPIServer = new Server(externalProperties.getAPIServerPort());

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        IHyracksClientConnection hcc = getNewHyracksClientConnection();
        context.setAttribute(HYRACKS_CONNECTION_ATTR, hcc);
        context.setAttribute(ASTERIX_BUILD_PROP_ATTR, AsterixAppContextInfo.getInstance());

        jsonAPIServer.setHandler(context);

        // AQL rest APIs.
        context.addServlet(new ServletHolder(new QueryAPIServlet(new AqlCompilationProvider())),
                Servlets.AQL_QUERY.getPath());
        context.addServlet(new ServletHolder(new UpdateAPIServlet(new AqlCompilationProvider())),
                Servlets.AQL_UPDATE.getPath());
        context.addServlet(new ServletHolder(new DDLAPIServlet(new AqlCompilationProvider())),
                Servlets.AQL_DDL.getPath());
        context.addServlet(new ServletHolder(new AQLAPIServlet(new AqlCompilationProvider())), Servlets.AQL.getPath());

        // SQL++ rest APIs.
        context.addServlet(new ServletHolder(new QueryAPIServlet(new SqlppCompilationProvider())),
                Servlets.SQLPP_QUERY.getPath());
        context.addServlet(new ServletHolder(new UpdateAPIServlet(new SqlppCompilationProvider())),
                Servlets.SQLPP_UPDATE.getPath());
        context.addServlet(new ServletHolder(new DDLAPIServlet(new SqlppCompilationProvider())),
                Servlets.SQLPP_DDL.getPath());
        context.addServlet(new ServletHolder(new AQLAPIServlet(new SqlppCompilationProvider())),
                Servlets.SQLPP.getPath());

        // Other APIs.
        context.addServlet(new ServletHolder(new QueryStatusAPIServlet()), Servlets.QUERY_STATUS.getPath());
        context.addServlet(new ServletHolder(new QueryResultAPIServlet()), Servlets.QUERY_RESULT.getPath());
        context.addServlet(new ServletHolder(new QueryServiceServlet()), Servlets.QUERY_SERVICE.getPath());
        context.addServlet(new ServletHolder(new ConnectorAPIServlet()), Servlets.CONNECTOR.getPath());
        context.addServlet(new ServletHolder(new ShutdownAPIServlet()), Servlets.SHUTDOWN.getPath());
        context.addServlet(new ServletHolder(new VersionAPIServlet()), Servlets.VERSION.getPath());
        context.addServlet(new ServletHolder(new ClusterAPIServlet()), Servlets.CLUSTER_STATE.getPath());
    }

    private void setupFeedServer(AsterixExternalProperties externalProperties) throws Exception {
        feedServer = new Server(externalProperties.getFeedServerPort());

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        IHyracksClientConnection hcc = getNewHyracksClientConnection();
        context.setAttribute(HYRACKS_CONNECTION_ATTR, hcc);

        feedServer.setHandler(context);
        context.addServlet(new ServletHolder(new FeedServlet()), "/");

    }

    @Override
    public void startupCompleted() throws Exception {
        // Notify Zookeeper that the startup is complete
        ILookupService zookeeperService = ClusterManager.getLookupService();
        if (zookeeperService != null) {
            // Our asterix app runtimes tests don't use zookeeper
            zookeeperService.reportClusterState(AsterixClusterProperties.INSTANCE.getCluster().getInstanceName(),
                    ClusterState.ACTIVE);
        }
    }
}
