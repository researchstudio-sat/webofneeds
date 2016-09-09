webofneeds
==========

[![Join the chat at https://gitter.im/researchstudio-sat/webofneeds](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/researchstudio-sat/webofneeds?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

E-marketplaces on the worldwide Web are information and transaction silos, which in the general case don’t allow transactions across their boundaries. The consequence is that the Web, often termed the global marketplace, is fragmented along the dimensions of geography, content domain, supply or demand, user base, and many more. This fragmentation makes it inefficient to buy and sell commodities on the Web. 

Web of Needs is an infrastructure that serves as a foundation for a distributed, decentralized e-marketplace on top of the Web, making boundaries between existing systems disappear from the user’s perspective.

Web of Needs standardizes the creation and description of linked data "need" objects that represent supply and demand. 
In addition to this, it allows for independent matching services to connect need objects suitable for a transaction and it defines protocols for the message exchange between such objects. 

Web of needs is built out of three main components
- won-node: server component hosting the linked data (RDF) need objects
- won-owner: user interface application allowing for enduser need management 
- won-matching-service: service that connects to won-nodes and crawls (or subscribes for) needs to calculate matches and send back hint messages for further communication. 

Requirements for getting started:
- install docker (we use 1.7.0)
- follow the [instructions here](webofneeds/won-docker/README.md) to set up all the neccessary components of web of needs locally

---

See also: http://events.linkeddata.org/ldow2013/papers/ldow2013-paper-13.pdf

**Build-instructions** can be found in the [project wiki](https://github.com/researchstudio-sat/webofneeds/wiki)

**Security architecture** overview can be found [here](webofneeds/won-core/README.md)

**Linked Data** support in WoN is described [here](webofneeds/won-node-webapp/README.md)
