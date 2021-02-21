# Web of Needs Ontologies

## Main ontologies

The main Web of Needs ontologies are those relevant to all or most conceivable application domains. They are organized in modules modules by topic area.
    
* [WoN Core Ontology](./core) (prefix `won:`): describes atoms, connections, and other fundamental concepts.
* [WoN Message Ontology](./message) (prefix `msg:`): resources for messaging
* [WoN Authorization Ontology](./auth) (prefix `auth:`): framework for declarative authorization in WoN
* [WoN Agreement Ontology](./agreement) (prefix `agr:`): agreements based on message exchange
* [WoN Content Ontology](./content) (prefix `con:`): resources for adding content to atoms/messages
* [WoN Matching Ontology](./matching) (prefix `match:`): vocabulary for expressing matching criteria
* [WoN Modification Ontology](./modification) (prefix `mod:`): extension for modifying messages after sending them
* [WoN Workflow Ontology](./workflow) (prefix `wf:`): vocabulary for embedding workflows in WoN conversations.

## Extensions

Extension ontologies provide resources for specific domains or aspects that may be applicable to many domains.

* [WoN Hold Extension](./ext/hold) (prefix:`wx_hold`): enables atoms to 'hold'(own, control) other atoms via connections.
* [WoN Persona Extension](./ext/persona) (prefix:`wx_persona`) 
* [WoN Chat Extension](./ext/chat) (prefix:`wx_chat`): provides the chat socket
* [WoN Buddy Extension](./ext/buddy) (prefix:`wx_buddy`): enables atoms to become 'buddies' via connections
* [WoN Group Extension](./ext/group) (prefix:`wx_group`): enables group chat
* [WoN Bot Extension](./ext/bot) (prefix:`wx_bot`): provides resources for bots
* [WoN Demo Extension](./ext/demo) (prefix:`wx_demo`): provides resources for example use cases
* [WoN Review Extension](./ext/review) (prefix:`wx_review`): provides the possibility to add reviews to atoms
* [WoN Schema Extension](./ext/schema) (prefix:`wx_schema`): automatic translation of all (most?) schema.org properties into sockets such that atoms can establish these properties by establishing connections. 
* [WoN Pokemon Go Extension](./ext/pogo) (prefix:`wx_pogo`): resources for a pokemon go prototype

