##Web Identity in WoN


### Identify of WoN communicating entities

Atoms, and Nodes as communicating entities in WoN are identified by resource URIs, expose their resource description as 
linked data, and are also in possession of digital self-signed certificates. Their resource RDF content, among other 
semantic data about the Atom and Node, contains the public key of their digital certificate. Atoms and Nodes sign 
their communication with the private key of their digital certificate. Their publicly available public key is used by
WoN parties to verify the authenticity of the data created and signed by the Atoms or Nodes, and for authentication 
and authorization of Atoms and Nodes. Proof of the identity is the possession of private key for the corresponding 
public key and certificate. The possession is proved via public key cryptography. 

**NOTE:** Matcher is not yet implemented as having a web identity, neither is the Owner. But both of them also have a 
self-signed certificate used to authenticate them.


### Public key in resource description

We intend for WoN communicating entities to be compatible with 
[WebID specifications](https://www.w3.org/2005/Incubator/webid/spec/). 
Below is an example of part of an Atom resource description in TRIG format: 


```
@prefix woncrypt: <https://w3id.org/won/core#> .
@prefix atom:  <https://localhost:8443/won/resource/atom/> .
@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
@prefix cert:  <http://www.w3.org/ns/auth/cert#> .
@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix won:   <https://w3id.org/won/core#> .
@prefix ldp:   <http://www.w3.org/ns/ldp#> .
@prefix dc:    <http://purl.org/dc/elements/1.1/> .

<https://localhost:8443/won/resource/event/fik80dx3gleh12mworbu#content-zv4y> {
  atom:sklrhgbltxlgylnz7goe
    a                     won:Atom ;
    won:hasBasicAtomType  won:Demand ;
    won:content        [ a  won:AtomContent ;
                               dc:title               "WANTED: external CD burner."^^xsd:string ;
                               dc:description "Mine broke. Atom a new one ASAP."^^xsd:string
                          ] ;
    won:socket          won:OwnerSocket ;
    won:atomModality   [ a  won:AtomModality ] ;
    cert:key              [ cert:PublicKey  
                            [ a woncrypt:ECCPublicKey ;
                                woncrypt:ecc_algorithm  "EC" ;
                                woncrypt:ecc_curveId    "secp384r1" ;
                                woncrypt:ecc_qx         "45a44e476c42d38f80f42202ba94644316c6cd0e1b068819961cc707deb02bbf50a726edd6694c298856152ebb83c51c" ;
                                woncrypt:ecc_qy         "8ddc287d9e1eedcef26e37eee4052ccfb30d56e09adb2e8302dde187a0d59acbe1f4a2591b6fb49807d36d6ad68c554c"
                            ] 
                          ] .
}
```


In the resource description of a WoN communicating entity, the public key information is introduced using the 
[Cert Ontology](http://www.w3.org/ns/auth/cert). Namely, the subject (Atom or Node URI) with property 
<http://www.w3.org/ns/auth/cert#key> introduces the Public Key object via the 
<http://www.w3.org/ns/auth/cert#PublicKey> property. The vocabulary for representing the public key information is the 
vocabulary created by us within the WoN project: we use Elliptic Curves (EC) for public-key cryptography and we found 
no public Ontology for representing Elliptic Curve data in RDF. The Certificate Ontology only covers representation 
of the RSA Public Key. 

We use Class <https://w3id.org/won/core#ECCPublicKey> to define the type of the key, and use properties
* <https://w3id.org/won/core#ecc_algorithm>
* <https://w3id.org/won/core#curveId>
* <https://w3id.org/won/core#ecc_qx>
* <https://w3id.org/won/core#ecc_qy>

to define the public key itself. This information is sufficient to be able to reconstruct EC public key with any 
common cryptographic library and apply it for signature validation or encryption. We use 
[Bouncy Castle](https://www.bouncycastle.org/) library  for generating and storing the Atoms certificates. We use 
384-Bit elliptic curve, curve id secp384r1. Initially, we considered using the curve with id brainpoolp384r1 from 
[Brainpool Standard](https://www.ietf.org/rfc/rfc5639.txt), as it 
is considered one of the safest curves. Later, during the implementation and applying the curve as part of TLS, we 
faced the problem that standard TLS implementations, e.g. in browsers and web servers, support only 
[limited number of curves](https://tools.ietf.org/html/draft-ietf-tls-rfc4492bis-06#section-5.1.1), brainpoolp384r1 
not being among them. Therefore, we have chosen to use secp384r1, one of the standard NIST curves.

**NOTE:** We should follow the cryptographic algorithms support by TLS standard and its implementations in browsers and 
web servers, because they plan to add support to more secure curves.


### Relation between Atom and a User identity

An Identities of the Atoms don't have to be linked to identities of the users, providing the ground for anonymous 
Atoms and contributing to user privacy protection. Unless a user publishes his 
own personal information as part of Atom semantic data, it is not possible to make a conclusion about the real identity 
of a user by crawling publicly available WON linked data.
 
 
### Key and certificate generation

For Atoms, key pairs and corresponding self-signed certificates are generated when they are being created on the Owner 
application server-side. 
They are also stored there. This brings certain benefits - user does not have to deal with Atoms' key management and 
GUI is not complicated by authenticating on per-Atom basis. But it also bring a weakness - if an owner application is 
compromised, all the atoms generated at it are also compromised.


For Nodes, Matchers and Owners, the certificates that certify their identity as WoN communicating entity are either 
generated when first deployed, or can be provided. In the first case they 
are generated in the same way as for Atoms, and are self-signed certificates. In the second case they can be issued by 
anyone, e.g. also self-signed, or issued and 
signed by a trusted CA. They even can be the same certificate as the one used for server authentication on the 
application level. If provided, they have to be in the keystore type supported by WoN applications. Currently it is 
"UBER" format with BouncyCastle security provider, as defined in 
[KeyStoreService](../src/main/java/won/cryptography/service/KeyStoreService.java)

How to configure keystore properties is described in [Authentication](authentication.md) section.



  
