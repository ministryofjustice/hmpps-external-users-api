# HMPPS External Users

The External Users service is currently work-in-progress in an attempt to move data and its associated endpoints out of 
hmpps-auth.  External Users (aka auth users) and any associated data will eventually sit in this service and have its
own database.

Until all data can be moved, hmpps-external-users will depend upon the current HMPPS-Auth service, specifically its 
database, auth-db.

# Instructions

## Running Locally
To be able to run the application locally, hmpps-auth needs to be running and so does a local postgres database.
Follow instructions in the hmpps-auth service readme, to run auth against a local postgres database.
Once this is up and running, external-users can be running, using the **dev,local-postgres** profile.  This will mean
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

