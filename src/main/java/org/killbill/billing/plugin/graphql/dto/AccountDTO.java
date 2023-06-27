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

package org.killbill.billing.plugin.graphql.dto;

import java.util.Collection;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.boilerplate.AccountImp;
import org.killbill.billing.entitlement.api.SubscriptionBundle;

public class AccountDTO extends AccountImp {

    public Collection<SubscriptionBundle> bundles;

    public AccountDTO(final Account account) {
        super(new AccountImp.Builder<>().source(account).build());
    }

    public Collection<SubscriptionBundle> getBundles() {
        return bundles;
    }

    public void setBundles(final Collection<SubscriptionBundle> bundles) {
        this.bundles = bundles;
    }
}
