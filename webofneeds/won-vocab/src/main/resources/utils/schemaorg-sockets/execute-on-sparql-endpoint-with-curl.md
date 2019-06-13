# How To Generate the schema.org Socket Definitions
1. Download the vocabulary
The schema.org vocabulary can be downloaded here: https://schema.org/docs/developers.html#defs
2 Import into your RDF store 
3. Execute the sparql query in this folder with curl on a sparql endpoint

Assuming that the graph 'wons' on the specified endpoint contains the schema.org vocabulary, this query generates the corresponding socket definitions:
```
curl -i -H "Accept: application/x-turtle" --data-urlencode query@sparql-construct-for-generating-socketdefs.rq http://localhost:9999/blazegraph/namespace/wons/sparql > won-ext-schema.ttl
``` 
4. remember to remove the response headers