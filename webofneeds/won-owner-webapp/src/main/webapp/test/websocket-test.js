
var jsontest ={
    "@graph" : [ {
        "@graph" : [ {
            "@id" : "http://example.com/responseMessage/837ddj",
            "@type" : "msg:CreateResponseMessage",
            "msg:hasReceiverNeed" : {
                "@id" : "http://localhost:8080/won/resource/need/920094381212434400"
            },
            "msg:hasResponseStateProperty" : {
                "@id" : "msg:SuccessResponse"
            },
            "msg:hasSenderNode" : {
                "@id" : "http://localhost:8080/won"
            },
            "msg:refersTo" : {
                "@id" : "http://localhost:8080/won/resource/need/920094381212434400/event/34543242134"
            }
        } ],
        "@id" : "http://example.com/responseMessage/837ddj#data"
    }, {
        "@graph" : [ {
            "@id" : "http://example.com/responseMessage/837ddj#data",
            "@type" : "msg:EnvelopeGraph"
        } ],
        "@id" : "urn:x-arq:DefaultGraphNode"
    } ],
    "@context" : {
        "msg" : "http://purl.org/webofneeds/message#"
    }
}


function testRdf() {
   /* parser = new rdf.JsonLdParser();
    var storePromise = null;
    parser.parse(jsontest, function(graph) {
        console.log("graph parsed. now adding to store");
        storePromise = store.add("abcd:xyz", graph);
    }, 'https://www.example.com/john/card');
    storePromise.then(function() {
        console.log("testing match");
        var matchPromise = store.match("abcd:xyz", null, "http://purl.org/webofneeds/message#hasReceiverNeed", null, null);
    });*/
    var store = new rdfstore.Store();
    store.setPrefix("ex", "http://example.org/people/");
    store.setPrefix("msg","http://purl.org/webofneeds/message#");

    store.load("application/ld+json", jsontest, "ex:test", function(success, results) {
        console.log("success:" + success + ", results: " + results);
    });
    var graph = store.rdf.createGraph();
    store.graph("ex:test", function(success, mygraph) {
        // process graph here
        //var triples = [];
        //var triples = mygraph.match("ex:test", null, "http://purl.org/webofneeds/message#hasReceiverNeed", null, null);

        for (var i = 0; i < mygraph.triples.length; i++) {
            console.log("triple:" + mygraph.triples[i]);
        }

        var triples = mygraph.match(null, store.rdf.createNamedNode(store.rdf.resolve('msg:hasReceiverNeed')),null);
        for (var i = 0; i < triples.length; i++) {
            console.log("triple:" + triples[i]);
        }
    });
    store.node("http://example.com/responseMessage/837ddj#data","ex:test", function(success, mygraph) {
        // process graph here
        //var triples = [];
        //var triples = mygraph.match("ex:test", null, "http://purl.org/webofneeds/message#hasReceiverNeed", null, null);

        for (var i = 0; i < mygraph.triples.length; i++) {
            console.log("triple:" + mygraph.triples[i]);
        }
    });


    var  parseResult = JSONLDParser.parser.parse(jsontest, "ex:test");
}