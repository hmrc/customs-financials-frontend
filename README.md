# customs-financials-frontend

## About
This public-facing microservice is part of the CDS Financials service. It is designed to work in tandem with the [customs-financials-api](https://github.com/hmrc/customs-financials-api) back-end service.

It is the hub and entry point for traders to access the different HMRC customs financial services (cash account, duty deferment, guarantee account, financial documents and account authorities). It also lets traders search for accounts they have authority to use and view their contact details.

It integrates with:
- Secure Payments Service (SPS) / Enterprise Tax Management Platform (ETMP) via [customs-financials-api](https://github.com/hmrc/customs-financials-api)
- Secure Data Exchange Service (SDES) bulk data API via the [SDES proxy](https://github.com/hmrc/secure-data-exchange-proxy)


| Key                             | Value                                                                                                                                                                       |
|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Digital service                 | CDS Financials                                                                                                                                                              |
| Local port                      | `9876`                                                                                                                                                                      |
| Base path                       | `/customs/payment-records`                                                                                                                                                  |
| Back-end                        | [customs-financials-api](https://github.com/hmrc/customs-financials-api) (port `9878`)                                                                                      |
| Acceptance tests                | [customs-financials-acceptance-tests](https://github.com/hmrc/customs-financials-acceptance-tests)                                                                          |
| Performance tests               | [customs-financials-performance-tests](https://github.com/hmrc/customs-financials-performance-tests)                                                                        |
| Stubs used in Local and Staging | [customs-financials-hods-stub](https://github.com/hmrc/customs-financials-hods-stub), [customs-financials-sdes-stub](https://github.com/hmrc/customs-financials-sdes-stub) |

## How to Run this Service

### Prerequisites
Use [Service Manager](#service-manager-profiles) to start all the services that CDS Financials depends on:
```bash
sm2 --start CUSTOMS_FINANCIALS_ALL
```

### Running the service locally
To run this service from source (for example, to test your own changes), stop the instance started by Service Manager and run it with sbt:
```bash
sm2 --stop CUSTOMS_FINANCIALS_FRONTEND
sbt run
```

The service starts on port `9876`. To access it, sign in through the auth login stub at http://localhost:9949/auth-login-stub/gg-sign-in with one of the [required enrolments](#enrolment-required), and set the redirect URL to `/customs/payment-records`.

## How to Test this Service

### Local

#### Unit tests
```bash
sbt test
```

To run the tests in a single file:
```bash
sbt "testOnly *TEST_FILE_NAME*"
```

#### Pre-push check
The `runAllChecks` sbt alias compiles the code, runs the unit and integration (`it/test`) tests, checks formatting and code style, and produces the coverage report.
It is good practice to run it just before pushing to GitHub.

```bash
sbt runAllChecks
```

The minimum requirement for statement coverage is 90%. Builds fail when the project drops below this threshold. The coverage report is written to `target/scala-3.3.4/scoverage-report/index.html`.

#### Acceptance tests (smoke and regression)
The acceptance tests for this service are in [customs-financials-acceptance-tests](https://github.com/hmrc/customs-financials-acceptance-tests). For information about how to run them, and which environments they run against, see its [README](https://github.com/hmrc/customs-financials-acceptance-tests#readme).

#### Performance tests
The performance tests for this service are in [customs-financials-performance-tests](https://github.com/hmrc/customs-financials-performance-tests). For information about how to run them, and which environments they run against, see its [README](https://github.com/hmrc/customs-financials-performance-tests#readme).

### Manual Testing in QA and Staging
After a successful deployment, manual testing is performed in the QA and/or Staging environments by signing in through the auth login stub (`https://<host>/auth-login-stub/gg-sign-in`) with one of the [required enrolments](#enrolment-required).

## Service Catalogue
- [customs-financials-frontend in the MDTP Catalogue](https://catalogue.tax.service.gov.uk/repositories/customs-financials-frontend)

## Jenkins Pipeline
- The front-end build and deployment jobs are listed on the [MDTP Catalogue page](https://catalogue.tax.service.gov.uk/repositories/customs-financials-frontend) for this repository.
- The acceptance test Jenkins jobs (smoke and regression) are listed in the [customs-financials-acceptance-tests README](https://github.com/hmrc/customs-financials-acceptance-tests#readme).
- The performance tests run weekly in the [customs-financials-performance-tests Jenkins job](https://performance.tools.staging.tax.service.gov.uk/job/Borders%20and%20Trade%20Live%20Services/job/CDS%20Financials/job/customs-financials-performance-tests/), defined in [performance-test-jobs](https://github.com/hmrc/performance-test-jobs/blob/main/jobs/live/bordersAndTrade_live_services.groovy).

## Service Manager Profiles
These profiles are defined in [service-manager-config](https://github.com/hmrc/service-manager-config).

| Profile                  | Use                                         | Services started                                                                                                                                                                                                         |
|--------------------------|---------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `CUSTOMS_FINANCIALS_ALL` | Running and developing this service locally | Includes `CUSTOMS_FINANCIALS_FRONTEND`, `CUSTOMS_FINANCIALS_API`, `CUSTOMS_FINANCIALS_HODS_STUB`, `CUSTOMS_FINANCIALS_SDES_STUB`, `AUTH`, `AUTH_LOGIN_STUB`, `AUTH_LOGIN_API`, `BAS_GATEWAY`, `CA_FRONTEND`, `SSO`, `USER_DETAILS`, `CONTACT_FRONTEND`. See [service-manager-config](https://github.com/hmrc/service-manager-config/blob/main/profiles.json) |

## Endpoints

All paths are relative to `/customs/payment-records` and need an authenticated user with the [required enrolment](#enrolment-required). The full list is in `conf/app.routes`.

| Method | Path                             | Purpose                                                                    | Sample request (query / form body) | Response                                               |
|--------|----------------------------------|----------------------------------------------------------------------------|------------------------------------|--------------------------------------------------------|
| GET    | `/`                              | Service entry point: the customs financials home page with account tiles   | –                                  | `200`<br>Accounts unavailable: `303` → `/account-unavailable` |
| GET    | `/account-unavailable`           | Home page shown when account details cannot be retrieved                   | –                                  | `200`                                                  |
| GET    | `/parts-of-service-unavailable`  | Home page shown without accounts                                           | –                                  | `200`                                                  |
| GET    | `/authority-to-use`              | Search for accounts you have authority to use                              | –                                  | `200`                                                  |
| POST   | `/authority-to-use`              | Submit an EORI or account number search                                    | `value=GB744638982000`             | `303` → `/authorities-search-results`<br>Invalid: `400` |
| GET    | `/authorities-search-results`    | Results of the authorities search                                          | –                                  | `200`<br>No results: `303` → `/authorities-no-results` |
| GET    | `/authorities-no-results`        | No matching authorities found                                              | –                                  | `200`                                                  |
| GET    | `/request-authorities-csv`       | Request a CSV of the authorities granted to you                            | –                                  | `200`                                                  |
| GET    | `/your-contact-details`          | View your contact details                                                  | –                                  | `200`                                                  |
| GET    | `/verify-your-email`             | Shown when the user's email address is unverified                          | –                                  | `200`                                                  |
| GET    | `/undeliverable-email`           | Shown when the user's email address is undeliverable                       | –                                  | `200`                                                  |
| GET    | `/not-subscribed-for-cds`        | Shown when the user is not subscribed to CDS                               | –                                  | `200`<br>Not signed in: `303` → `bas-gateway` sign-in  |
| GET    | `/help/accessibility`            | Accessibility statement                                                    | –                                  | `200`                                                  |
| GET    | `/logout`                        | Sign out and go to the feedback survey                                     | –                                  | `303` → `bas-gateway` sign-out                         |
| GET    | `/logout-no-survey`              | Sign out without the feedback survey                                       | –                                  | `303` → `bas-gateway` sign-out                         |

## Enrolment Required
To use the service, sign in through the auth login stub with the `HMRC-CUS-ORG` enrolment and one of these sample EORI numbers:

| Enrolment Key  | Identifier Name | Identifier Value |
|----------------|-----------------|------------------|
| `HMRC-CUS-ORG` | `EORINumber`    | `GB744638982000` |
| `HMRC-CUS-ORG` | `EORINumber`    | `GB744638982001` |

## Developer Notes

### Feature Flags

Feature flags are used appropriately on different envs and could be updated in customs-cash-account-frontend.yaml on pertinent env

### Enable features
| Command                                       | Description                                        |
|-----------------------------------------------|----------------------------------------------------|
| `sbt "run -Dfeatures.some-feature-name=true"` | enables a feature locally without risking exposure |

Feature switches can be enabled per-environment via the `app-config-<env>` project in customs-cash-account-frontend.conf/.yaml

### Available feature flags

The list of feature flags and what they are responsible for:

| Flag                                     | Default | Description                                                                                                                                     |
|------------------------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `fixed-system-time`                      | `false` | Uses a fixed date and time in `DateTimeService` by enabling stub data for current and requested statements. Only enabled in the Development environment. |
| `xi-eori-enabaled`                       | `true`  | Enables the XI EORI related API calls.                                                                                                          |
| `cash-account-v2-enabled`                | `false` | Enables the new cash account design.                                                                                                            |
| `home-page-links-enabled`                | `false` | Controls the display of the 'Account authorities' and 'Find accounts you have authority to use' links. Must be false in all environments.       |
| `authorities-notification-panel-enabled` | `false` | Enables the notifications panel.                                                                                                                |
| `eu-eori-enabled`                        | `true`  | Enables the EU EORI feature.                                                                                                                    |

### Scalafmt
The code is formatted with [sbt-scalafmt](https://scalameta.org/scalafmt/docs/installation.html#sbt), using the rules in `.scalafmt.conf`. It is also style-checked with Scalastyle, using `scalastyle-config.xml` (and `test/test-scalastyle-config.xml` for test code).

Check that all project files are formatted as expected:

```bash
sbt scalafmtCheckAll
```

Format all project files:

```bash
sbt scalafmtAll
```

Run the code style checks:

```bash
sbt scalastyle Test/scalastyle
```

## License
This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
