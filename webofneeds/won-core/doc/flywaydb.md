## FlywayDB - Continuous Database Migration
We currently use 2 postgreSQL Databases for our Project (one for the won-owner-webapp, and the other one for won-node-webapp),
the ddl of those databases are migrated and validated via FlywayDB, the jpa/hibernate configuration solely validates the schema
after flyway has migrated the database when the respective containers are started.

We use Flyway to make Databases Schema Changes

# Database Baseline
* To start with pre-existing Databases you need to run the flyway commandline tool (https://flywaydb.org/getstarted/download)
for your respective System.
See https://flywaydb.org/getstarted/firststeps/commandline for the initial configuration of the commandline tool.
* To create a baseline for preexisting databases please follow the steps from this link: https://flywaydb.org/documentation/existing


# HowTo Create a schema change:
1. Create a SQL File within the Projects resources/db/migration folder (within won-owner-webapp or won-node-webapp)
    - For Naming Conventions please lookup preexisting files or the flyway documentation (https://flywaydb.org/documentation/migration/java)
2. Write all the necessary Schema Updates/Changes in Form of valid PostgreSQL statments.
3. Update your Hibernate annotated POJO's according to the changed Schema
3. Enjoy!

# Hints and Traps
* Be aware that already executed migration-steps can only be reverted via a rollback (with commandline tool) or another update script.
* Flyway DB creates and stores a checksum for each migrationfile so any alteration of migrationfiles that have already been done to a given database will result in an error during deployment