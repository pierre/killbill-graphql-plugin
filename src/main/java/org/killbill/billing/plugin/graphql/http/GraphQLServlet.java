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

package org.killbill.billing.plugin.graphql.http;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jooby.Result;
import org.jooby.Results;
import org.jooby.Status;
import org.jooby.mvc.Body;
import org.jooby.mvc.GET;
import org.jooby.mvc.Local;
import org.jooby.mvc.OPTIONS;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.killbill.billing.tenant.api.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.ExecutionInput;
import graphql.ExecutionInput.Builder;
import graphql.ExecutionResult;
import graphql.GraphQL;

@Singleton
@Path("/")
public class GraphQLServlet {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLServlet.class);

    private final GraphQL graphQL;

    @Inject
    public GraphQLServlet(final GraphQL graphQL) {
        this.graphQL = graphQL;
    }

    @OPTIONS
    @Path("*")
    public Result getDataPreflight() {
        // CORS headers are already set by Kill Bill core
        return Results.noContent();
    }

    @GET
    @Path("/graphiql")
    public Result graphiql(@Local @Named("killbill_tenant") final Optional<Tenant> tenant) {
        final InputStream stream = getClass().getClassLoader().getResourceAsStream("index.html");
        final InputStreamReader inputStreamReader = new InputStreamReader(Objects.requireNonNull(stream), Charset.defaultCharset());
        return Results.with(inputStreamReader, Status.OK).header("Content-Type", "application/html");
    }

    @POST
    @Path("/graphql")
    public Result graphql(@Local @Named("killbill_tenant") final Optional<Tenant> tenant,
                          @Body final GraphQLRequest request) {
        final Builder builder = ExecutionInput.newExecutionInput();
        tenant.ifPresent(value -> builder.graphQLContext(Map.of("killbill_tenant", value)));

        final ExecutionInput executionInput = builder.query(request.query).build();
        final ExecutionResult executionResult = graphQL.execute(executionInput);

        return Results.with(executionResult.toSpecification(), Status.OK).header("Content-Type", "application/json");
    }

    static class GraphQLRequest {

        public String query;
        public String operationName;
        public String variables;
        public String extensions;
    }
}
