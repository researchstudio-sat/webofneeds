# How to Web of Needs

This file provides an overview over the essential **How To's** to get started with the **Web of Needs**

These **How To's** are mainly focusing on the developers point of view and will help you to start and implement your first WoN instance and application.

If you haven't heard or read anything about the **Web of Needs** and its structure you should follow this [link](/README.md) to get a quick introduction on what the **WoN** is about and how its architecture is structured.

## Overview

- [How to run WoN locally](#how-to-run-won-locally) (=Getting Started)
- [How to write a matcher](#how-to-write-a-matcher)
- [How to write a bot](#how-to-write-a-bot)
- [How to write a WoN web application](#how-to-write-a-won-web-application)
- [How to set up a WoN cloud instance](#how-to-set-up-a-won-cloud-instance)

### How to run WoN locally

There are two ways of running WoN locally:

#### 1. Docker

The "fastest" way to run the WoN locally ist to use the provided Docker container and setup. We recommend this solution if you want to try the WoN locally for the first time to have a quick hands on.
For running WoN with Docker locally follow the steps in the [WoN with Docker Documentation](/webofneeds/won-docker/README.md).

#### 2. Tomcat

The Tomcat solution of running the WoN locally is mainly for development purposes. If you focus on just one aspect in the WoN you can combine the Docker and the Tomcat setup e.g.: Working on the WebApp, you just need to run the WebApp locally on tomcat and the Node and Matcher still with Docker. To do so we recommend Eclipse as IDE and you can find the setup documentation [here](/documentation/build-with-eclipse.md).

### How to write a matcher

Basic information about the matcher service is provided [here](/webofneeds/won-matcher-service/README.md).
To integrate a new matcher, or matching algorithm we recommend to use the matcher skeleton, providing all needed functions for a matcher service in the WoN.

### How to write a bot

Basic information about bots is provided [here](/webofneeds/won-bot/README.md).
To write a new bot, we recommend to use the bot skeleton, providing all needed functions for a bot in the WoN.

### How to write a WoN web application

Basic information about a WoN web application is provided [here](/webofneeds/won-owner-webapp/README.md)

### How to set up a WoN cloud instance

Information coming soon [here](installation-setting-up-build-environment-in-jenkins.md)
