## Matching Service Architecture

Matching is currently done by setting up an instance of a *won-matcher-service* module which provides functionality to connect
to won nodes, crawl them for new atoms and subscribe for atom event life cycle updates. It also provides functionality to
send hint messages to won nodes if matches between atoms have been found. Furthermore a matching service has its own
local RDF store where is saves crawled atoms as well as its own event bus to include matching algorithms for instance.

The matching service sends events (e.g. atom-created-events) on its local event bus if it detects new atoms from
crawling won nodes for example. Also it sends hint messages out to won nodes if hint events are published on its
local event bus.

The matching service is based on the Actor-framework [Akka](http://akka.io/). That means its components (actors) can be
distributed and scaled up quite easily. If you start up the main java program (see [deployment and docker files](../won-docker) for how different components are started up), a Akka cluster (seed) node is started up to which
other Akka cluster nodes can connect to. The matching service provides the basic functionality for matching but does
not execute a matching algorithm itself.

### Solr Online Matcher

The Solr matching algorithm from the module [won-matcher-solr](../won-matcher-solr) is the default online matcher
that actually computes matches in real time. It saves received atoms into a Solr index and generates queries for them
to find similar atoms in the index to generate matches.

It is integrated into the matching service by starting an Akka cluster node that connects itself to the matching
service cluster node. It implements an actor, [MatcherPubSubActor]
(../won-matcher-solr/src/main/java/won/matcher/solr/actor/MatcherPubSubActor.java), that registers itself with the matching service event bus and handles events messages from and to the matching service.

After registration the Solr matcher will receive events (e.g. most important atom-created-events) that the matching
service send on its event bus. The Solr matcher will also compute matches from incoming atoms and send them back as
hint events to the matching service via the event to be published.

### Integrating a matching algorithm

If you want to implement a new matching algorithm, have a look at [won-matcher-solr](../won-matcher-solr) module. As described you have to implement an actor that subscribes and publishes to the matching services event bus. Therefore you need to set up a
Akka cluster node that connects itself with the matching service.

You can run this matching algorithm remotely (in relation to the matching service) and still communicate over the
distributed event bus.

For all components discussed you find how the docker images are build and deployed in the [won-docker module](../won-docker)









