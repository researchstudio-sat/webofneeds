# Web of Needs Ontologies

## Main ontologies

The main Web of Needs ontologies are those relevant to all or most conceivable application domains. They are organized in modules modules by topic area.
    
* [WoN Core Ontology](./core): describes atoms, connections, and other fundamental concepts.

    `@prefix won: <https://w3id.org/core#> .`
* [WoN Message Ontology](./message): resources for messaging
    
    `@prefix msg: <https://w3id.org/message#> .`

* [WoN Authorization Ontology](./auth): framework for declarative authorization in WoN
    
    `@prefix auth: <https://w3id.org/auth#> .`

* [WoN Agreement Ontology](./agreement): agreements based on message exchange

    `@prefix agr: <https://w3id.org/agreement#> .`

* [WoN Content Ontology](./content): resources for adding content to atoms/messages
    
    `@prefix con: <https://w3id.org/content#> .`

* [WoN Matching Ontology](./matching): vocabulary for expressing matching criteria
    
    `@prefix match: <https://w3id.org/match#> .`

* [WoN Modification Ontology](./modification): extension for modifying messages after sending them
    
    `@prefix mod: <https://w3id.org/mod#> .`

* [WoN Workflow Ontology](./workflow): vocabulary for embedding workflows in WoN conversations.
    
    `@prefix wf: <https://w3id.org/workflow#> .`

## Extensions

Extension ontologies provide resources for specific domains or aspects that may be applicable to many domains.

* [WoN Hold Extension](./ext/hold): enables atoms to 'hold'(own, control) other atoms via connections.

    `@prefix wx-hold: <https://w3id.org/ext/hold#> .`

* [WoN Persona Extension](./ext/persona) 
    
    `@prefix wx-persona: <https://w3id.org/ext/persona#> .`

* [WoN Chat Extension](./ext/chat): provides the chat socket

    `@prefix wx-chat: <https://w3id.org/ext/chat#> .`

* [WoN Buddy Extension](./ext/buddy): enables atoms to become 'buddies' via connections

    `@prefix wx-buddy: <https://w3id.org/ext/buddy#> .`

* [WoN Group Extension](./ext/group): enables group chat

    `@prefix wx-group: <https://w3id.org/ext/group#> .`

* [WoN Bot Extension](./ext/bot): provides resources for bots

    `@prefix wx-bot: <https://w3id.org/ext/bot#> .`

* [WoN Demo Extension](./ext/demo): provides resources for example use cases

    `@prefix wx-demo: <https://w3id.org/ext/demo#> .`

* [WoN ValueFlows Extension](./ext/valueflows): provides resources for using valueflows on top of WoN

    `@prefix wx-vf: <https://w3id.org/ext/valueflows#> .`
    
* [WoN Review Extension](./ext/review): provides the possibility to add reviews to atoms

    `@prefix wx-review: <https://w3id.org/ext/review#> .`

* [WoN Schema Extension](./ext/schema): automatic translation of all (most?) schema.org properties into sockets such that atoms can establish these properties by establishing connections. 

    `@prefix wx-schema: <https://w3id.org/ext/schema#> .`

* [WoN Pokemon Go Extension](./ext/pogo): resources for a pokemon go prototype

    `@prefix wx-pogo: <https://w3id.org/ext/pogo#> .`
