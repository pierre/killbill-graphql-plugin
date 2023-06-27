/*
 * Copyright 2020-2023 Equinix, Inc
 * Copyright 2014-2023 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.graphql;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;
import org.killbill.billing.plugin.core.config.PluginEnvironmentConfig;
import org.killbill.billing.plugin.core.resources.jooby.PluginApp;
import org.killbill.billing.plugin.core.resources.jooby.PluginAppBuilder;
import org.killbill.billing.plugin.graphql.http.GraphQLHealthcheckServlet;
import org.killbill.billing.plugin.graphql.http.GraphQLServlet;
import org.killbill.billing.plugin.graphql.services.GraphQLHealthcheck;
import org.killbill.billing.plugin.graphql.services.KillBillDataFetcher;
import org.osgi.framework.BundleContext;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

public class GraphQLActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "graphql-plugin";

    private GraphQLConfigurationHandler graphQLConfigurationHandler;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final String region = PluginEnvironmentConfig.getRegion(configProperties.getProperties());

        // Register an event listener for plugin configuration
        graphQLConfigurationHandler = new GraphQLConfigurationHandler(region, PLUGIN_NAME, killbillAPI);
        final Properties globalConfiguration = graphQLConfigurationHandler.createConfigurable(configProperties.getProperties());
        graphQLConfigurationHandler.setDefaultConfigurable(globalConfiguration);

        // Expose a healthcheck
        final Healthcheck healthcheck = new GraphQLHealthcheck();
        registerHealthcheck(context, healthcheck);

        // Create the GraphQL environment
        final InputStream stream = getClass().getClassLoader().getResourceAsStream("schema/schema.graphqls");
        final Reader streamReader = new InputStreamReader(Objects.requireNonNull(stream), Charset.defaultCharset());
        final TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(streamReader);
        final RuntimeWiring runtimeWiring = newRuntimeWiring()
                .type("Query", builder -> builder.dataFetcher("account", new KillBillDataFetcher(killbillAPI)))
                .build();
        final SchemaGenerator schemaGenerator = new SchemaGenerator();
        final GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        final GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();

        // Register the servlets
        final PluginApp pluginApp = new PluginAppBuilder(PLUGIN_NAME, killbillAPI, dataSource, super.clock,
                                                         configProperties).withRouteClass(GraphQLServlet.class)
                                                                          .withRouteClass(GraphQLHealthcheckServlet.class)
                                                                          .withService(healthcheck)
                                                                          .withService(graphQL)
                                                                          .build();
        final HttpServlet httpServlet = PluginApp.createServlet(pluginApp);
        registerServlet(context, httpServlet);

        registerHandlers();
    }

    private void registerHandlers() {
        final PluginConfigurationEventHandler configHandler = new PluginConfigurationEventHandler(graphQLConfigurationHandler);
        dispatcher.registerEventHandlers(configHandler);
    }

    private void registerServlet(final BundleContext context, final Servlet servlet) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Servlet.class, servlet, props);
    }

    private void registerHealthcheck(final BundleContext context, final Healthcheck healthcheck) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Healthcheck.class, healthcheck, props);
    }
}
