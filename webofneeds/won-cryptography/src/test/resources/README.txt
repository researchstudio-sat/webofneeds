If there are parts of the data that are signed and parts that
are not (e.g. triples that are going to change frequently) then
we require a named graphs representation to be able to specify
which parts of the data the signature refers to.

Also, if there are parts of the data that are signed by different
participants, then we also require the named graphs to be able
to specify which part is signed by which signature.

Moreover, if the data signed by participant 1 has to be resigned
(possible with added data) by participant 2, and the signature of
participant 1 is to be kept in the data, then the named graph
are also required to refer to the signed by participant 1 data.



Next steps:

Use algorithm from signingframework but change signature and signed data representation a bit,
namely, don't use nested graphs.

for that one atom:
- add class similar to Assembler class and implement own assemble() method by adding signature triples
in the default graph with the signed data graph as subject (not to use nested graphs)
- subclass all classes implementing Signing algorithm and override assemble() method to call
our own Assembler implementation
- map Jena graph representation
- wrap the algoritms or implement separate WonSigner/Verifier to decompose named graphs data into
separate graphs and perform on each of them the canonicalize/hash/sign/assemble methods
of the algorithm, and then merge the graphs and signature triples together into one dataset


Define more clear the versioning scenario (connections and won atom):
- possible simple solution for won atom:
  HTTP:
  example.com/resource12/ -> 303 example.com/resource12.rdf -> example.com/resource12/20140705120036.rdf
  example.com/resource12/20140705120036/ -> 303 example.com/resource12/20140705120036.rdf
  RDF:
  @prefix dc:      <http://purl.org/dc/elements/1.1/> .
  @prefix pav:     <http://purl.org/pav/> .
  @prefix owl:     <http://www.w3.org/2002/07/owl#> .
  _:wonatom owl:sameAs <example.com/resource12/20140705120036/>;
            dc:isVersionOf <example.com/resource12/>
            pav:previousVersion <http://www.example.com/resource12/20140617095504>;


Define how we represent the signature
- already decided: named graphs
- to be done: make an example in trig for different stages of the atom (creation by owner,
final creation by won node, after connections are added, etc.) with the signature(s), then
it will be clear what we are still missing in our signature definition
- to be done: find which signature rdf vocabulary to use or define own own
- for now I will use the Signingframework vocabulary since that's already implemented


Signature representation - 2 solutions possible:

(1) # currently chosen by Florian solution
When signing signed data (data in graph A and triples containing signature of A),
sign only the triples containing signature of A inside a new graph B. The signature itself
of B describe with triples.

This implies that the party that wants to verify the data will have to varify first data
in B with signature of B, then varify the data in A with signature of A.

// 1) verify the G2 (in this example => verify G1 signature) with the signature of G2
// 2) verify the G1 with signature of G1 contained in G2
# G1 is a graph that contains some triples
G1 {
_:bnode1
    rdf:type foaf:Person ;
    foaf:name "Manu Sporny" ;
    foaf:homepage <http://manu.sporny.org/> .
}

# 2 is a graph that contains a triple "G1 sig:signature [ ... ]"
G2 {
  G1 sig:signature [
      rdf:type sig:JsonldSignature ;
      sig:signer <http://manu.sporny.org/webid#key-5> ;
      sig:signatureValue
"OGQzNGVkMzVmMmQ3ODIyOWM32MzQzNmExMgoYzI4ZDY3NjI4NTIyZTk=" . ] .
}

# the default graph contains a triple "G2 sig:signature [ ... ]"
G2 sig:signature [
  rdf:type sig:JsonldSignature ;
  sig:signer <http://authority.payswarm.com/webid#key-873> ;
  sig:signatureValue
"kMzVmMVDIyOWM32MzI4ZDY3NjI4mQ3OOGQzNGNTIyZTkQzNmExMgoYz=" . ] .



(2)
When signing signed data (data in graph A and triples containing signature of A),
copy the triples from A into a new graph B, and copy triples containing signature of
A into that graph B. The signature itself of B describe with triples.

This implies that the party that wants to verify the data will have to varify data
in B with signature of B, and it should only trust the data from the graph B and
in that case no varification of the data in A is necessary. But the RDF representation will
be messy. In this case, maybe the graph A should be removed at all.
