# killbill-graphql-plugin

GraphQL plugin for Kill Bill.

## Getting Started

To build, run `mvn clean install`. You can then install the plugin locally:

```
kpm install_java_plugin graphql --from-source-file target/graphql-*-SNAPSHOT.jar --destination /var/tmp/bundles
```

## Play

Go to GraphiQL at http://127.0.0.1:8080/plugins/graphql-plugin/graphiql

```graphql
{
    account(idFilter: "58fe781e-3e8d-4c2d-a872-842dabfbacc6") {
        externalKey
        currency
        referenceTime
        timeZone
        billCycleDayLocal
        bundles {
            id
            createdDate
            subscriptions {
                id
                billCycleDayLocal
                billingStartDate
                chargedThroughDate
                lastActivePhase {
                    phaseType
                    recurring {
                        billingPeriod
                        recurringPrice {
                            prices {
                                value
                                currency
                            }
                        }
                    }
                }
            }
            timeline {
                subscriptionEvents {
                    effectiveDate
                    serviceName
                    serviceStateName
                    nextBillingPeriod
                    nextPlan {
                        prettyName
                    }
                }
            }
        }
    }
}
```

Note: in the headers section (bottom), make sure to enter your tenant details:

```json
{
  "x-killbill-apikey": "bob",
  "x-killbill-apisecret": "lazar"
}
```

## Development

In IntelliJ, make sure to enable the Federation setting in `Languages & Frameworks > GraphQL` (see https://github.com/JetBrains/js-graphql-intellij-plugin/issues/297).

## About

Kill Bill is the leading Open-Source Subscription Billing & Payments Platform. For more information about the project, go to https://killbill.io/.
