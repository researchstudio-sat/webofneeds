# Atom(=Post) Structure

Posting something in the Web of Needs is done by creating an RDF structure and sending it to a WoN node.

Here, we explain this structure and how it is interpreted for matching.

## Example

Here is an example of an atom content that could be sent to a WoN node:
Below, we'll explain the most important aspects.

```
@prefix atom:  <https://node.matchat.org/won/resource/atom/> .
@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
@prefix geo:   <http://www.w3.org/2003/01/geo/wgs84_pos#> .
@prefix s:     <http://schema.org/> .
@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix won:   <https://w3id.org/won/core#> .
@prefix dc:    <http://purl.org/dc/elements/1.1/> .

atom:sitl5bx3kk96
  a   won:Atom ;
  won:socket  won:OwnerSocket ;
  won:is  [ dc:description   "We're giving away an HP color laser printer for free. The model is hp color LaserJet 4650dn. It has not been used in a long time; IIRC it should still work but there are print quality issues.\n\n#printer #laser #hp" ;
    dc:title   "Laser Printer" ;
    won:location  [ a     s:Place ;
       con:boundingBox  [ con:northWestCorner  [ a  s:GeoCoordinates ;
             s:latitude   "48.218727" ;
             s:longitude  "16.360141"
             ] ;
           con:southEastCorner  [ a  s:GeoCoordinates ;
             s:latitude   "48.218828" ;
             s:longitude  "16.360241"
             ]
           ] ;
       s:geo   [ a  s:GeoCoordinates ;
           s:latitude   "48.218778" ;
           s:longitude  "16.360191"
           ] ;
       s:name  "8, Thurngasse, KG Alsergrund, Alsergrund, Wien, 1090, Österreich"
       ] ;
    con:tag   "laserjet" , "color" , "free" , "printer" , "laser" , "hp"
    ] ;
  match:seeks   [ dc:description   "#printer #laser #printer #color #HP #laserjet #free" ;
    dc:title   "Printer" ;
    won:location  [ a     s:Place ;
       con:boundingBox  [ con:northWestCorner  [ a  s:GeoCoordinates ;
             s:latitude   "48.117907" ;
             s:longitude  "16.181831"
             ] ;
           con:southEastCorner  [ a  s:GeoCoordinates ;
             s:latitude   "48.322668" ;
             s:longitude  "16.577513"
             ]
           ] ;
       s:geo   [ a  s:GeoCoordinates ;
           s:latitude   "48.220210" ;
           s:longitude  "16.371216"
           ] ;
       s:name  "Wien, Österreich"
       ] ;
    con:tag   "printer" , "laser" , "color" , "HP" , "laserjet" , "free"
    ] ;
```

## \<atom\> won:is \<description\>

`won:is` is used to link the atom's URI to RDF structures that describe it positively. If present, the description can be used to display the atom in user interfaces. There is almost no restriction as to the data that can be added here. However, the webofneeds implementation only uses a few things here:

- `dc:title`
- `dc:description'
- `con:tag`
- `won:location` expecting a structure of `s:Place`, `s:name`, `s:geo` and `con:boundingBox` as shown in the example.

## \<atom\> match:seeks \<search-definition\>

`match:seeks`links the atom's URI to one or more searches. Matchers should use these to search within other atom's descritpions (i.e., their content referenced by `won:is`)
The matcher of the current demo system looks for the properties listed above in the content referenced by `match:seeks` and searches for matches in the other atoms' description, and also vice versa.

## \<atom\> match:flag \<flag\>

`match:flag` is used to define certain binary properties
the following flags are defined:

- `match:WhatsAround` : An Atom specifying this flag indicates that matchers should not match with anything but the location within this atom. The intention of this Flag is to simply find other atoms around a certain location.
- `match:NoHintForCounterpart`: An Atom specifying this flag indicates that matchers should not send Hint messages to the counterpart in case of a match. If the NoHintForMe flag is also present, matchers should disregard this atom completely.
- `match:NoHintForMe`: An Atom specifying this flag indicates that matchers should not send Hint messages to that Atom. If the NoHintForCounterpart flag is also present, matchers should disregard this atom completely.
- `match:UsedForTesting`: An Atom specifying this flag indicates that it is created for testing purposes. This may trigger diagnostic behaviour in matchers and bots. In the current implementation, the DebugBot will connect with the atom.

## \<atom\> won:socket \<socket\>

Sockets will be explained in a later update, for now it's sufficient to know that won:OwnerSocket is the only in use.
