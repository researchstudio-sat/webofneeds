webofneeds
==========

[![Join the chat at https://gitter.im/researchstudio-sat/webofneeds](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/researchstudio-sat/webofneeds?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

E-marketplaces on the worldwide Web are information and transaction silos, which in the general case don’t allow transactions across their boundaries. The consequence is that the Web, often termed the global marketplace, is fragmented along the dimensions of geography, content domain, supply or demand, user base, and many more. This fragmentation makes it inefficient to buy and sell commodities on the Web. 

**Web of Needs is an infrastructure that serves as a foundation for a distributed, decentralized e-marketplace on top of the Web, making boundaries between existing systems disappear from the user’s perspective.**

Our overall goal is to create a decentralized infrastructure that allows people to publish documents on the Web which make it possible to contact each other, and this should only happen if all parties involved have an interest in doing so. The said document may contain a description of a product or service required or offered, a description of a problem to be solved with the help of others, an invitation to social activities, or anything else users may think of. On this abstract level of description, the document can be said to represent an interest in or a **need for some kind of interaction with others**. 

Therefore, we refer to this document as a **need**. It is the central entity of the system we propose. Each need has a globally unique identifier and an owner, i.e., a person or other entity that creates and controls it. When need owners want to communicate with each other, a connection object is created for each need involved. 

Web of needs is built out of three main components. **Owner applications** enable users to create and manage their need objects. They can be any type of UI application like web applications or mobile apps for example. Owner applications publish needs as RDF documents to **won nodes** on the Web. When needs are published on the Web, independent **matching services** can crawl them (or subscribe for need updates at won nodes) and look for suitable matches. A protocol is in place to inform the won nodes and need owners of possible matches using hint messages. Based on this process need owners can initiate connections to other needs and start communication and other transactions.

A **demo deployment of the Web of Needs** with a simple owner application, one won node and one matching service can be tested at www.matchat.org

If you want to **set up your own deployment of Web of Needs** components, here you find requirements for **getting started**:
- install docker (we use 1.7.0)
- follow the [instructions here](webofneeds/won-docker/README.md) to set up all the neccessary components of web of needs locally

---

See also: http://events.linkeddata.org/ldow2013/papers/ldow2013-paper-13.pdf

**Build-instructions** can be found in the [project wiki](https://github.com/researchstudio-sat/webofneeds/wiki)

**Security architecture** overview can be found [here](webofneeds/won-core/README.md)

**Linked Data** support in WoN is described [here](webofneeds/won-node-webapp/README.md)
