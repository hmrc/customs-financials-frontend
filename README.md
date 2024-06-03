
# Customs Financials Frontend

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A frontend component for the CDS Financials project which aims to provide financial services for customs 
transactions.

| Path                                                                   | Description                                                                                       |
| ---------------------------------------------------------------------  | ------------------------------------------------------------------------------------------------- |
| GET  /import-vat                                                       | Retrieve all import vat certificates                                                           |                
| GET  /postponed-vat                                                    | Retrieve all postponed vat statements                                                          |                
| GET  /adjustments                                                      | Retrieve all securities statements                                                           |                


This applications lives in the "public" zone. It integrates with:

* Secure Payments Service (SPS) / Enterprise Tax Management Platform (ETMP) via the [Customs Financials API](https://github.com/hmrc/customs-financials-api)
* Secure Document Exchange Service (SDES) bulk data API via the [SDES proxy](https://github.com/hmrc/secure-data-exchange-proxy)

In dev/test environments, the upstream services are stubbed out using stub services (see below).

## Running the application locally

The application has the following runtime dependencies:

* `ASSETS_FRONTEND`
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
 
You can use the `CUSTOMS_FINANCIALS_FRONTEND_DEPS` profile in service manager to start up these services without this
project, to help run locally.

Once these services are running, you should be able to do `sbt "run 9876"` to start in `DEV` mode or 
`sbt "start -Dhttp.port=9876"` to run in `PROD` mode.

## Running tests

There is just one test source tree in the `test` folder. Use `sbt test` to run them.

To get a unit test coverage report, you can run `sbt clean coverage test coverageReport`,
then open the resulting coverage report `target/scala-2.11/scoverage-report/index.html` in a web browser.

Test coverage threshold is set at 90% - so if you commit any significant amount of implementation code without writing tests, you can expect the build to fail.

## Feature Switches

Feature switches can be enabled per-environment via the `app-config-<env>` project:

    ...
    features.some-feature: true
    ...

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


## Autocomplete scripts 

This project has Tampermonkey scripts available in tampermonkey directory.

#### Tampermonkey
[Chrome Extension](https://chrome.google.com/webstore/detail/tampermonkey/dhdgffkkebhmkfjojejmpbldmpobfkfo?hl=en)<br>
[Firefox Extension](https://addons.mozilla.org/pl/firefox/addon/tampermonkey/)

## All tests and checks

This is a sbt command alias specific to this project. It will run a scala style check, run unit tests, run integration
tests and produce a coverage report:

> `sbt runAllChecks`
