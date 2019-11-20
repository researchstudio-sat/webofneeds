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

The properties ensure that multiple messages can be aggregated in one dataset without affecting their interpretation. In a multi-message dataset, all named graphs that belong to a message can be identified by removing the fragment identifier (`#[graphId]`) from their graph URI. This yields the message URI.

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
| `msg:atom` | used in atom-specific messages (Create, Replace, Activate, Deactivate, Delete) as well as their Responses, to specify the atom |
| `msg:connection`  |  used in messages of type msg:SuccessResponse when acknowledging a message in an connection's message container |
| `msg:previousMessage`| links to an earlier message |
| `msg:senderSocket` | indicates the socket from which the message was sent |
| `msg:recipientSocket`| indicates the socket at which the message is addressed |
| `msg:respondingTo`| used in response messages to link to the message being responded to |
| `msg:respondingToMesageType`| used in response messages to indicate the type of message being responded to |

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

| Property | Description |
| --------- | -----------
| `msg:signer` |  links to the public key for its verification |
| `msg:signedGraph` | links to the graphs within the message that are signed by this signature |
| `msg:hash` | specifies the Base58-encoded multihash of the signed graphs |
| `msg:signatureValue` | specifies the Base64-encoded ECDSA signature value obtained signing the hash with the key |
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

# Complete Example
Full example of a WoN message, in this case, a Create message:
```
<wm:/W1jfE1q9XN9EhKUTxKTAFwapuc6CyoJGYV5nEkhMfakMKS#content-rf6e> {
    atom:i573rg5eohhwqh77285g
            a                  won:Atom ;
            dc:title           "Test Atom 2" ;
            cert:key           [ cert:PublicKey  [ a                  won:ECCPublicKey ;
                                                   won:ecc_algorithm  "EC" ;
                                                   won:ecc_curveId    "secp384r1" ;
                                                   won:ecc_qx         "f6514f811722ed756de7ccc789df1236cb051f17d39c3936c0f649bd7cda06056a3508685612a58a23ce5a273e4aa4e6" ;
                                                   won:ecc_qy         "727fab734bcb1164db084948b5c67462ed2c6a88896c13de2f6321f42cb660da97fe6f34656ccd924865942bcdd6cb83"
                                                 ] ] ;
            won:defaultSocket  <https://localhost:8443/won/resource/atom/i573rg5eohhwqh77285g#chatSocket> ;
            won:socket         <https://localhost:8443/won/resource/atom/i573rg5eohhwqh77285g#chatSocket> , <https://localhost:8443/won/resource/atom/i573rg5eohhwqh77285g#holdableSocket> , <https://localhost:8443/won/resource/atom/i573rg5eohhwqh77285g#socket1> ;
            match:flag         match:UsedForTesting .
    
    <https://localhost:8443/won/resource/atom/i573rg5eohhwqh77285g#chatSocket>
            won:socketDefinition  <https://w3id.org/won/ext/chat#ChatSocket> .
    
    <https://localhost:8443/won/resource/atom/i573rg5eohhwqh77285g#holdableSocket>
            won:socketDefinition  <https://w3id.org/won/ext/hold#HoldableSocket> .
    
    <https://localhost:8443/won/resource/atom/i573rg5eohhwqh77285g#socket1>
            won:socketDefinition  <https://w3id.org/won/ext/chat#ChatSocket> .
}

<wm:/W1jfE1q9XN9EhKUTxKTAFwapuc6CyoJGYV5nEkhMfakMKS#envelope> {
    <wm:/W1jfE1q9XN9EhKUTxKTAFwapuc6CyoJGYV5nEkhMfakMKS>
            a                    msg:FromOwner ;
            msg:atom             atom:i573rg5eohhwqh77285g ;
            msg:content          <wm:/W1jfE1q9XN9EhKUTxKTAFwapuc6CyoJGYV5nEkhMfakMKS#content-rf6e> ;
            msg:messageType      msg:CreateMessage ;
            msg:protocolVersion  "1.0" ;
            msg:timestamp        1574083632172 .
    
    <wm:/W1jfE1q9XN9EhKUTxKTAFwapuc6CyoJGYV5nEkhMfakMKS#envelope>
            a                      msg:EnvelopeGraph ;
            rdfg:subGraphOf        <wm:/W1jfE1q9XN9EhKUTxKTAFwapuc6CyoJGYV5nEkhMfakMKS> ;
            msg:containsSignature  <wm:/W1jfE1q9XN9EhKUTxKTAFwapuc6CyoJGYV5nEkhMfakMKS#content-rf6e-sig> .
    
    <wm:/W1jfE1q9XN9EhKUTxKTAFwapuc6CyoJGYV5nEkhMfakMKS#content-rf6e-sig>
            a                               msg:Signature ;
            msg:hasVerificationCertificate  atom:i573rg5eohhwqh77285g ;
            msg:signatureValue              "MGYCMQDV/cWOD61AguKBkCRVdma10bEFZaNDc00VIHOfNzX6k/8LuWgfyaPi3wVszScrfUECMQCBoicpa2t5V+Cz8WCo52dIJUWlP4Y8JqMd8IisPsq6qZHP79716hTgkg1BM0UBIEs=" ;
            msg:hash                        "W1dMkKqey5ZKLdi7gbsxYmkNZ2XRjU9Jq8UTHVPV3Qt9ES" ;
            msg:publicKeyFingerprint        "W1nQKZrBKwuo9MQbChv5tir2uZA2hHX5izrEiYH98v6nzC" ;
            msg:signedGraph                 <wm:/W1jfE1q9XN9EhKUTxKTAFwapuc6CyoJGYV5nEkhMfakMKS#content-rf6e> .
}

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


