# Web of Needs

Finding and cooperating with people. Protocol, not platform. Decentralized. Linked Data. Open Source.

[![Join the chat at https://gitter.im/researchstudio-sat/webofneeds](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/researchstudio-sat/webofneeds?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This software is for people who need something, have something to offer, want to discuss something etc. - in short: who wish to connect with others for some reason.

1. They publish a posting about it online
2. They get recommendations on matching postings created by other people
3. They start chatting with some of the creators of these other postings
4. Maybe they'll meet. Maybe one will buy from the other. Maybe they will start a company, plant a tree, get married, found a party...

That's it, basically. Try our demo on [matchat.org](http://matchat.org)! And then [run your own servers](webofneeds/won-docker/README.md)!

![interaction-diagram](http://researchstudio-sat.github.io/webofneeds/images/interaction-diagram-book.png)

## Overview

The Web of Needs is a decentralized infrastructure that allows people to publish documents on the Web which make it possible to contact each other. The document may contain a description of a product or service required or offered, a description of a problem to be solved with the help of others, an invitation to social activities, or anything else users may think of. Some concrete [use cases are described here](documentation/use-cases-users.md). On the abstract level of description, the document can be said to represent an interest in or a **need for some kind of interaction with others**.

As this document or entity is the central and indivisible building block of the system, we refer to it as an **atom**. Each atom has a globally unique identifier and an owner, i.e., a person or other entity that creates and controls it. When atom owners want to communicate with each other, a connection object is created for each atom involved.

Web of Needs is built out of three main components. **Owner applications** enable users to create and manage their atom objects. They can be any type of UI application like web applications or mobile apps for example. Owner applications publish atoms as RDF documents to **won nodes** on the Web. When atoms are published on the Web, independent **matching services** can crawl them (or subscribe for atom updates at won nodes) and look for suitable matches. A protocol is in place to inform the won nodes and atom owners of possible matches using hint messages. Based on this process atom owners can initiate connections to other atoms and start communication and other transactions.

Anyone can run any of these components. They can all talk to each other.

## Demo

A **demo deployment of the Web of Needs** with a simple owner application, one won node and one matching service can be tested at <https://www.matchat.org>

## Running WoN

There are two ways of running WoN locally:

### 1. Docker

The "fastest" way to run the WoN locally ist to use the provided Docker container and setup. We recommend this solution if you want to try the WoN locally for the first time to have a quick hands on.
For running WoN with Docker locally follow the steps in the [WoN with Docker Documentation](/webofneeds/won-docker/README.md).

### 2. Tomcat

The Tomcat solution of running the WoN locally is mainly for development purposes. If you focus on just one aspect in the WoN you can combine the Docker and the Tomcat setup e.g.: Working on the WebApp, you just need to run the WebApp locally on tomcat and the Node and Matcher still with Docker. To do so we recommend Eclipse as IDE and you can find the setup documentation [here](/documentation/building.md).

## Development

For development we recommend using the [Tomcat-setup mentioned above](#2-tomcat). After having that up and running, depending on what you want to do, you can continue with the following documentation-parts:

### How to write a matcher

Basic information about the matcher service is provided [here](/webofneeds/won-matcher-service/README.md).
To integrate a new matcher, or matching algorithm we recommend to use the matcher skeleton, providing all needed functions for a matcher service in the WoN.

### How to write a bot

Basic information about bots is provided [here](/webofneeds/won-bot/README.md).
To write a new bot, we recommend to use the [bot skeleton](https://github.com/researchstudio-sat/bot-skeleton) as a template, providing all needed functions for a bot in the WoN.

### How to write a WoN web application

Basic information about a WoN web application is provided [here](/webofneeds/won-owner-webapp/README.md)

### How to set up a WoN cloud instance

Information coming soon [here](installation-setting-up-build-environment-in-jenkins.md)

## Further resources

- Learn how to [Run your own WoN services](webofneeds/won-docker/README.md)
- [Build-instructions](/documentation/building.md#building) and [Troubleshooting](documentation/building.md#troubleshooting)
- [Security architecture](webofneeds/won-core/README.md) 
- [Linked Data interface](webofneeds/won-node-webapp/README.md)
- [Matching Service Architecture](webofneeds/won-matcher-service/README.md)
- [Bot Framework](webofneeds/won-bot/README.md) for interacting with WoN nodes programmatically
- [Ontologies](/documentation/ontologies.md) defined for this project

## Papers and further Information

- More detailed description of the Web of Needs can be found [here](http://sat.researchstudio.at/en/web-of-needs)
- [Beyond Data: Building a Web of Needs](http://events.linkeddata.org/ldow2013/papers/ldow2013-paper-13.pdf)
- [The Case for the Web of Needs](http://sat.researchstudio.at/sites/sat.researchstudio.at/files/won_cbi-2014_the_case_for_the_web_of_needs.pdf)
- [Web of Needs - A New Paradigm for E-Commerce](http://sat.researchstudio.at/sites/sat.researchstudio.at/files/won-cbi-2013.pdf)
- [Building a Web of Needs](http://sat.researchstudio.at/sites/sat.researchstudio.at/files/kleedorfer_iswc_2011.pdf)
