webofneeds
==========

The central idea of the Web of needs is this: the WWW looks like a marketplace; 
everyone knows what there is for sale, only a few know what is needed. We want to change that.

The central point is the 'need', which we represent as a linked data resource. It names communication
protocol endpoints that can be used to contact its owner and provide information about interesting other
'needs'.

Web of needs is built of 
- won-node: the server component hosting the needs
- won-owner: a simple application allowing for enduser need management  
- and a matching-service: currently implemented as a crawler that contacts needs (via the protocol endpoints) whenever 
   it finds a good match.

Requirements for getting started:
- jdk 1.6 or later
- maven 2 or later
- tomcat 6 or later as application-server
- nodejs

To prevent reinventing the wheel every single day, 
we come back to existing (old) technology for the matching services:
- siren                   -> http://siren.sindice.com/ 
- triplestore(virtuoso)   -> http://virtuoso.openlinksw.com/
- solr                    -> http://lucene.apache.org/solr/
- ldspider                -> https://code.google.com/p/ldspider/

---

won-node
- all needs and offers will be saved there

won-owner
- needs are managed by the won-owner

matching-service
- the matching-service crawls the won-nodes and looks for need and offer entries with corresponding values.


See also: http://events.linkeddata.org/ldow2013/papers/ldow2013-paper-13.pdf

**Build-instructions** can be found in [this projects wiki](https://github.com/researchstudio-sat/webofneeds/wiki)
