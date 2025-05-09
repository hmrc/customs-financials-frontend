# Customs Financials Frontend

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Coverage](https://img.shields.io/badge/test_coverage-90%-green.svg)](/target/scala-2.11/scoverage-report/index.html) [![Accessibility](https://img.shields.io/badge/WCAG2.2-AA-purple.svg)](https://www.gov.uk/service-manual/helping-people-to-use-your-service/understanding-wcag)

A micro-frontend service - This service provides a hub/entry point to access the different financial services for HMRC customs.

The front end services on this domain are built following GDS standards to WCAG 2.2 AA

[GOV.UK design system](https://design-system.service.gov.uk/)
    
[WCAG 2.2](https://www.gov.uk/service-manual/helping-people-to-use-your-service/understanding-wcag)

<!-- todo: provide more context on gds and wcag -->

<!-- todo: mention how to access wcag review docs -->

## Running the service

*From the root directory*

`sbt run` - starts the service locally.
`sbt runAllChecks` - Will run all checks required for a successful build

### Required dependencies

There are a number of dependencies requored to run the service.

The easiest way to get started with these is via the service manager CLI - you can find the instalation guide [here](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-service-manager.html)

| Command                                          | Description |
| --------                                         | ------- |
| `sm2 --start CUSTOMS_FINANCIALS_ALL`             | Runs all deps |
| `sm2 -s`                                         | Shows running services |
| `sm2 --stop CUSTOMS_MANAGE_AUTHORITIES_FRONTEND` | Kills the micro service  |
| `sbt run`                                        | (from root dir) to compile the current service with your changes |

### Runtime Dependancies

* `AUTH`
* `AUTH_LOGIN_STUB`
* `AUTH_LOGIN_API`
* `BAS_GATEWAY`
* `CA_FRONTEND`
* `SSO`
* `USER_DETAILS`
* `CUSTOMS_FINANCIALS_API`
* `CUSTOMS_FINANCIALS_HODS_STUB`
* `CUSTOMS_FINANCIALS_SDES_STUB`
* `CONTACT_FRONTEND`

<!-- todo: verify this list -->

### Enrolments

Once the service is running you can access a test account by enrolling to the servce via [auth-login-stub/gg-sign-in](http://localhost:9949/auth-login-stub/gg-sign-in) using a redirect url, enrolment key, identifier name and value. Heres an example of a commonly used account (happy path)

Redirect URL - `/customs/payment-records`

| Enrolment Key	| Identifier Name | Identifier Value | Status |
| -------- | ------- | ------- | ------- | 
| `HMRC-CUS-ORG` | `EORINumber`| `GB744638982000` | `activated` |

## Testing

The minimum requirement for test coverage is 90%. Builds will fail when below this threshhold

<!-- todo: add more context about testing standards or how to test if required -->

### Unit Tests

| Command    | Description |
| -------- | ------- |
| `test` | Runs unit tests locally |
| `sbt "test:testOnly *TEST_FILE_NAME*"` | runs tests for a single file |

### Coverage

| Command    | Description |
| -------- | ------- |
| `sbt clean coverage test coverageReport` | Generates a unit test coverage report that you can find here target/scala-2.11/scoverage-report/index.html  |

## Available Routes

You can find a list of microservice specific routes here - `customs-financials-frontend/conf/app.routes`

Due to the microservice configuration of the systems, a number of enpoints in this service are used by external services...

<!-- todo: list any services of interest -->

## Feature Switches

Feature switches can be enabled per-environment via the `app-config-<env>` project

    features.some-feature: true

*Don't* enable features in `application.conf`, as this will apply globally by default,
so there's a risk of WIP features being exposed in production.
Instead, enable features locally using

    sbt "run -Dfeatures.some-feature-name=true"

In non-production environments,
you can also toggle features on or off in a running microservice instance
by performing a HTTP GET against

    /customs-financials/test-only/feature/<feature>/<enable|disable>

eg.

    $ curl localhost:9000/customs-financials/test-only/feature/report-a-problem/disable
    Disabled feature report-a-problem
    
Note that the microservice must be running with test-only routes explicitly enabled,
via this switch in the `app-config-<env>`, the service manager microservice profile,
or just via `sbt run` locally:

    "-Dapplication.router=testOnlyDoNotUseInAppConf.Routes"

<!-- todo: potentially redraft this section for consiseness (if thats a word) -->

## Helpful commands

| Command    | Description |
| -------- | ------- |
| `runAllChecks`        | Runs all standard code checks |
| `clean`               | Cleans code |
| `compile`             | x |
| `coverage`            | x |
| `test`                | x |
| `it/test`             | x |
| `scalafmtCheckAll`    | x |
| `scalastyle`          | x |
| `Test/scalastyle`     | x |
| `coverageReport`      | Produces a code coverage report |
| `sbt "test:testOnly *TEST_FILE_NAME*"` | runs tests for a single file |

<!-- todo: add missing descriptions and additional helpful commands  -->

<!-- todo: add a main point of contact(s) for the repo -->

