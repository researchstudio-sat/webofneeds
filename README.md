webofneeds
==========

[![Join the chat at https://gitter.im/researchstudio-sat/webofneeds](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/researchstudio-sat/webofneeds?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

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
- install docker (we use 1.7.0)
- follow the [instructions here](https://github.com/researchstudio-sat/webofneeds/webofneeds/won-docker/README.md) to set up all the neccessary components of web of needs locally

For Build instructions and more detailed information, [please consult the wiki](https://github.com/researchstudio-sat/webofneeds/wiki).

---

See also: http://events.linkeddata.org/ldow2013/papers/ldow2013-paper-13.pdf

**Build-instructions** can be found in the [project wiki](https://github.com/researchstudio-sat/webofneeds/wiki)

**Security architecture** overview can be found [here](webofneeds/won-core/README.md)

**Linked Data** support in WoN is described [here](webofneeds/won-node-webapp/README.md)
