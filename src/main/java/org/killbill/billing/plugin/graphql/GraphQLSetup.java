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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.plugin.graphql.dto.AccountDTO;
import org.killbill.billing.plugin.graphql.services.KillBillDataFetcher;

import com.apollographql.federation.graphqljava.Federation;
import com.apollographql.federation.graphqljava._Entity;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

public class GraphQLSetup {

    public static GraphQL create(final OSGIKillbillAPI killbillAPI) {
        final InputStream stream = GraphQLSetup.class.getClassLoader().getResourceAsStream("schema/schema.graphqls");
        final Reader streamReader = new InputStreamReader(Objects.requireNonNull(stream), Charset.defaultCharset());
        final TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(streamReader);
        final DataFetcher<AccountDTO> killBillDataFetcher = new KillBillDataFetcher(killbillAPI);
        final RuntimeWiring runtimeWiring = newRuntimeWiring()
                .type("Query", builder -> builder.dataFetcher("account", killBillDataFetcher)
                                                 .dataFetcher("accountByKey", killBillDataFetcher))
                .build();
        final SchemaGenerator schemaGenerator = new SchemaGenerator();
        final GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        // This fixes: Validation error (FieldUndefined@[_service]) : Field '_service' in type 'Query' is undefined ('query __ApolloGetServiceDefinition__ { _service{sdl} }')
        final GraphQLSchema federatedGraphQLSchema = Federation.transform(graphQLSchema)
                                                               .fetchEntities(env -> env.<List<Map<String, Object>>>getArgument(_Entity.argumentName)
                                                                                        .stream()
                                                                                        .map(reference -> null)
                                                                                        .collect(Collectors.toList()))
                                                               .resolveEntityType(env -> null)
                                                               .build();

        return GraphQL.newGraphQL(federatedGraphQLSchema).build();
    }
}
