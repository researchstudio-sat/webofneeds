# Setting up Frontend Development Environment

### Enabling GZip-compression

If you want to enable gzip-compression on you local tomcat (e.g. because you're tweaking with page-load optimisations), you can enable gzip-compression by adding the following to your tomcat's `server.xml`:


```xml
<Connector
    compression="on"
    compressableMimeType="text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/x-font-ttf"
   .../>
```

### Adding js dependencies
For example, for adding a dependecy to `rdf-formats-common`, do this:

`node_modules\.bin\jspm install github:rdf-ext/rdf-formats-common`

### Updating N3.js

As their npm package assumes usage in node, updating it is a bit extra effort. See <../webofneeds/won-owner-webapp/src/main/webapp/scripts/N3/how-to-update.md> (it should be right next to the built scripts).
