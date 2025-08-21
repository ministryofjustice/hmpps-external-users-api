# HMPPS External Users

The External Users service is currently work-in-progress in an attempt to move data and its associated endpoints out of 
hmpps-auth.  External Users (aka auth users) and any associated data will eventually sit in this service and have its
own database.

Until all data can be moved, hmpps-external-users will depend upon the current HMPPS-Auth service, specifically its 
database, auth-db.

[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-external-users-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-external-users-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-external-users-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://external-users-api-dev.hmpps.service.justice.gov.uk/webjars/swagger-ui/index.html?configUrl=/v3/api-docs)

# Instructions

## Running Locally
To be able to run the application locally, hmpps-auth needs to be running and so does a local postgres database.
Follow instructions in the hmpps-auth service readme, to run auth against a local postgres database.
Once this is up and running, external-users can be started using the **dev,local-postgres** profile.  This will mean
that external-users will access the local postgres database (auth-db) directly to get its data.

## Running Tests Locally
To mimic the use of auth database tables, a postgres db needs to be created that will contain the data needed for
external-users tests to run successfully.
Currently, this just relies on the ROLES table.
To run tests
* Run a local docker container to start up auth-db

```
docker stop auth-db && docker rm auth-db && docker-compose -f docker-compose-test.yml up
```
