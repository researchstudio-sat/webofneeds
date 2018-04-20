# Need(=Post) Structure

Posting something in the Web of Needs is done by creating an RDF structure and sending it to a WoN node.

Here, we explain this structure and how it is interpreted for matching.

## Example

Here is an example of a need content that could be sent to a WoN node:
Below, we'll explain the most important aspects.

```
@prefix need:  <https://node.matchat.org/won/resource/need/> .
@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
@prefix geo:   <http://www.w3.org/2003/01/geo/wgs84_pos#> .
@prefix s:     <http://schema.org/> .
@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix won:   <http://purl.org/webofneeds/model#> .
@prefix dc:    <http://purl.org/dc/elements/1.1/> .

need:sitl5bx3kk96
  a   won:Need ;
  won:hasFacet  won:OwnerFacet ;
  won:is  [ dc:description   "We're giving away an HP color laser printer for free. The model is hp color LaserJet 4650dn. It has not been used in a long time; IIRC it should still work but there are print quality issues.\n\n#printer #laser #hp" ;
    dc:title   "Laser Printer" ;
    won:hasLocation  [ a     s:Place ;
       won:hasBoundingBox  [ won:hasNorthWestCorner  [ a  s:GeoCoordinates ;
             s:latitude   "48.218727" ;
             s:longitude  "16.360141"
             ] ;
           won:hasSouthEastCorner  [ a  s:GeoCoordinates ;
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
    won:hasTag   "laserjet" , "color" , "free" , "printer" , "laser" , "hp"
    ] ;
  won:seeks   [ dc:description   "#printer #laser #printer #color #HP #laserjet #free" ;
    dc:title   "Printer" ;
    won:hasLocation  [ a     s:Place ;
       won:hasBoundingBox  [ won:hasNorthWestCorner  [ a  s:GeoCoordinates ;
             s:latitude   "48.117907" ;
             s:longitude  "16.181831"
             ] ;
           won:hasSouthEastCorner  [ a  s:GeoCoordinates ;
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
    won:hasTag   "printer" , "laser" , "color" , "HP" , "laserjet" , "free"
    ] ;
```

## \<need\> won:is \<description\>

## \<need\> won:seeks \<search-definition\>

## \<need\> won:hasFacet \<facet\>

