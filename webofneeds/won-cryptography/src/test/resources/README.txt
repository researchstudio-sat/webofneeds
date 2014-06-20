Next steps:

Use algorithm from signingframework but change signature and signed data representation a bit,
namely, don't use nested graphs.

2 solutions possible:

(1)
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