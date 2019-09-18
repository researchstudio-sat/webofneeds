## Requirements

At least maven 3.3.3

## War-files and Main Profiles

- `/webofneeds/won-node-webapp/target/won-node-webapp-[version].war`: the web application for the WoN node (the linked data server and communication node)
  - skip this using the maven profile `skip-node-webapp-war`
  - skip building the war file and just exploding it in the target folder with the maven profile `skip-node-webapp-war-but-explode`
- `webofneeds/won-owner-webapp/won-owner-webapp-[version].war`: the user-facing application that talks to WoN nodes in the background
  - skip the complete 'frontend' install and generation (i.e. npm install) before building the webapp with the maven profile `skip-frontend`
  - skip the 'frontend' install but do generation (i.e. resource munging with gulp) before building the webapp with the maven profile `skip-frontend-all-but-gulp`
  - skip building the webapp using the maven profile `skip-owner-webapp-war`
  - skip building the war file and just exploding the webapp in the target folder with the maven profile `skip-owner-webapp-war-but-explode`
- `/webofneeds/won-bot/target/bots.jar`: a jar file that includes all the necessary dependencies for running some Bot implementations. Note that this might be deprecated information, as we're currently in the process of moving the bots to seperate repositories
    - skip this using the maven profile `skip-bot-uberjar`
- `/webofneeds/won-matcher-service/target/won-matcher-service.jar`: a jar file that includes all the necessary dependencies for running the main matching service
  - skip this using the maven profile `skip-matcher-uberjar`
- `/webofneeds/won-matcher-rescal/target/won-matcher-rescal.jar`: a jar file that includes all the necessary dependencies for running a [RESCAL](https://github.com/nzhiltsov/Ext-RESCAL) based matcher.
  - skip this using the maven profile `skip-matcher-rescal-uberjar`

## Other maven profiles

In addition to the profiles mentioned above, there are several profiles available for building in Maven:

- `copy-module-dependencies` - copies dependencies to `<module-dir>/target/copiedDependencies` in lifecycle phase `validate`
- `copy-project-dependencies` - copies dependencies to `<exec-dir>/target/copiedDependencies` in lifecycle phase `validate`
- `skip-dependencies` - sets many dependencies' scopes to `provided`. Causes faster builds with smaller war files.
- `no-warn` - suppresses warnings
- `skip-tests` - skips the tests