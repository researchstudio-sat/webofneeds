# WoN Messages

Messages are the main communcation medium in WoN. They are used to create and modify atoms, as well as to exchange information between diferent atoms.

Messages have the following properties
* A message is an RDF dataset.
* Each message has a URI of the form `wm:/[id] `, where `id` is a Base58 encoded multihash of its content.
* Each graph of the *message dataset* is a named graph (default graph is empty).
* All graph names are of the form `[message-uri]#[graphId]`, where [graphId] matches `[a-zA-Z0-9]+`
* Each message has an *envelope graph*, which is named `[message-uri]#envelope` 
* Each message has a *signature graph*, which is named `[message-uri]#signature`
* A message may have one or more *content graphs*, which can have any identifier except the two mentioned before.
* The envelope graph contains addressing and type information
* The signature graph contains a signature made with the public key of the atom or the WoN node that sent the message. 
* The content graphs may contain arbitrary data.
* The message dataset does not contain any other graphs.

## Envelope
The envelope contains type and addressing information. This information is given in the form of properties 
of the message URI, i.e, triples of the form [message-uri] [property] [value].

The following properties are used:

| Property | Description |
| -------- | ----------- |
| `msg:messageType`| denotes the message type, for example, msg:CreateMessage |
| `msg:timestamp` | denotes the timestamp when the message was created |
| `msg:protocolVersion` | indicates the protocol version, currently `"1.0"`.
| `msg:content`| links to one of the message's content graphs |
| `msg:atom` | used in messages of type msg:SuccessResponse when acknowledging a message in an atom's message container  |
| `msg:connection`  |  used in messages of type msg:SuccessResponse when acknowledging a message in an connection's message container |
| `msg:previousMessage`| links to an earlier message |
| `msg:senderSocket` | indicates the socket from which the message was sent |
| `msg:recipientSocket`| indicates the socket at which the message is addressed |

Not all properties are used in all message types. The definition of mandatory and optional properties per type can be found in [WonMessageType.java](webofneeds/won-core/src/main/java/won/protocol/message/WonMessageType.java)

The envelope graph contains a the name of the graph, i.e. `[message-uri]#envelope` as a resource that is defined to be a subgraph of the message.

## Content
The content of a WoN message may contain arbitrary RDF triples, with only one exception: the message namespace (default prefix `msg:`)
may not be used. A common use case is sending text messages, in this case the `con:text` property from the
content namespace (default prefix `con:`) is used.

Example:
```
@prefix msg:   <https://w3id.org/won/message#> .
@prefix con:   <https://w3id.org/won/content#> .

# Envelope:
<wm:/W1q3ZULNznsgdSmmA4HqRgpLvYJfZyhgexRd2eurTzb1mH#envelope> {
    <wm:/W1q3ZULNznsgdSmmA4HqRgpLvYJfZyhgexRd2eurTzb1mH>
            msg:content          <wm:/W1q3ZULNznsgdSmmA4HqRgpLvYJfZyhgexRd2eurTzb1mH#content-fips> ;
    # ... (other envelope triples omitted)
}

# Content graph: 
<wm:/W1q3ZULNznsgdSmmA4HqRgpLvYJfZyhgexRd2eurTzb1mH#content-fips> {
    <wm:/W1q3ZULNznsgdSmmA4HqRgpLvYJfZyhgexRd2eurTzb1mH>
            con:text  "Nice, we are connected!" .
}
```

## Signature

The message signature is represented as RDF triples. The RDF resource denoting the signature is the same as the name of the graph it 
is contained in, i.e., `[message-uri]#signature`. A signature can sign one or more graphs. In most cases, a signature signs all graphs 
of a message. Messages of type CREATE_ATOM and REPLACE are special in that respect. For these messages, each content graph is
signed separately and the signatures are part of the content graph.

The follwing properties are used to specify a signature:

| Property | Descripiton |
| --------- | -----------
| `msg:signer` |  links to the public key for its verification |
| `msg:signedGraph` | links to the graphs within the message that are signed by this signature |
| `msg:hash` | specifies the Base58-encoded multihash of the signed graphs |
| `msg:signatureValue` | specifies the value obtained signing the hash with the key |
| `msg:publicKeyFingerprint` | specifies the Base58-encoded multihash of the public key |


Example:
```
@prefix msg:   <https://w3id.org/won/message#> .
@prefix atom:  <https://localhost:8443/won/resource/atom/> .
@prefix sig:   <http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#> .

<wm:/W1jfE1q9XN9EhKUTxKTAFwapuc6CyoJGYV5nEkhMfakMKS#signature> {
    <wm:/W1jfE1q9XN9EhKUTxKTAFwapuc6CyoJGYV5nEkhMfakMKS#signature>
            a                               msg:Signature ;
            msg:hasVerificationCertificate  atom:i573rg5eohhwqh77285g ;
            msg:signatureValue              "MGUCMDuZ8mDOEagNZBCH7aHvoNsFZVzgNmI7WFy2p2OpqolIOafDycNNmSuapUDpaxIOKwIxAO3beItRo4QYsA+4+6Iu7hPSJCnniQ0/9bkl27jS/W8oS8Q7iVwIiwxKq2/5XkuCaA==" ;
            msg:hash                        "W1p9hqRotr7VsYrvD4kWH1yE5RBLyyNNvPyu1BE6EFKpVh" ;
            msg:publicKeyFingerprint        "W1nQKZrBKwuo9MQbChv5tir2uZA2hHX5izrEiYH98v6nzC" ;
            msg:signedGraph                 <wm:/W1jfE1q9XN9EhKUTxKTAFwapuc6CyoJGYV5nEkhMfakMKS#envelope> .
}

```




