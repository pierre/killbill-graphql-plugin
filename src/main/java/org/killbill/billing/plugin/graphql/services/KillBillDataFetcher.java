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

package org.killbill.billing.plugin.graphql.services;

import java.util.List;
import java.util.UUID;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.entitlement.api.SubscriptionBundle;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.plugin.graphql.dto.AccountDTO;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.callcontext.boilerplate.TenantContextImp;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;

public class KillBillDataFetcher implements DataFetcher<AccountDTO> {

    private final OSGIKillbillAPI killbillAPI;

    public KillBillDataFetcher(final OSGIKillbillAPI killbillAPI) {
        this.killbillAPI = killbillAPI;
    }

    @Override
    public AccountDTO get(final DataFetchingEnvironment env) throws Exception {
        // See magic in GraphQLServlet
        final Tenant tenant = env.getGraphQlContext().get("killbill_tenant");
        final TenantContext context = new TenantContextImp.Builder<>().withTenantId(tenant.getId()).build();

        // As defined in the GraphQL schema
        final String idFilter = env.getArgument("idFilter");
        final String keyFilter = env.getArgument("keyFilter");

        final Account account;
        if (idFilter != null) {
            final UUID accountId = UUID.fromString(idFilter);
            account = killbillAPI.getAccountUserApi().getAccountById(accountId, context);
        } else {
            account = killbillAPI.getAccountUserApi().getAccountByKey(keyFilter, context);
        }
        final AccountDTO accountDTO = new AccountDTO(account);

        final DataFetchingFieldSelectionSet selectionSet = env.getSelectionSet();
        if (selectionSet.contains("bundles/*")) {
            final List<SubscriptionBundle> bundles = killbillAPI.getSubscriptionApi()
                                                                .getSubscriptionBundlesForAccountId(account.getId(), context);
            accountDTO.setBundles(bundles);
        }

        return accountDTO;
    }
}
