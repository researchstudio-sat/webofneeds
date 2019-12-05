# Hints

For applying migration scripts manually, install the flyway commandline tool, configure db access in `[flyway]/conf/flyway.conf`, e.g:

```
flyway.url=jdbc:postgresql://localhost:5432/won_node
flyway.user=won
flyway.password=won
```

and then run the migrate command: 

```
 ./flyway \
 	-locations=filesystem:c:/DATA/DEV/workspace/webofneeds/webofneeds/won-node/src/main/resources/db/migration \
 	-table=schema_version migrate
 ```