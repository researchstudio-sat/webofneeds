# Populating Parameters for Web Service Invocation
 
This documents describes a possible approach for how a Bot can act as a bridge between a Web API and WoN.
   
The main idea is that the bot connects to a counterpart atom and obtains all the information required to invoke the service.
 
It is assumed that the bot is programmed for a specific Web API, so questions of parameter conversion, service invocation etc. are handled by code specifically written for the API in question. We can assume the bot extends a base implementation that provides hooks or template methods that can be overriden. 

The question we want to answer here is: How can such a mechanism work and where should the generic solution end?  

## Assumptions 

1. The bot somehow has received the information that some counterpart atom AtomX is suitable for engaging in a conversation.  
1. The bot is able to create an atom specifically for this conversation, and connect with AtomX.
1. The bot is able to construct an rdf dataset containing all the information relevant to the connection (the dataset that is used to generat the data graph when [Stating Information Requirements](draft-stating-information-requirements.md))
1. The bot is able to communicate its information requirements and to determine when they are met (again, see [Stating Information Requirements](draft-stating-information-requirements.md))
1. The bot has a way of making a parameterized API call to the target Web API (for whichever methods it requires to fulfill its purpose).
1. The bot has a way of determining when the counterpart actually triggers the invocation. (see [Knowing when to invoke a Web Service](draft-when-to-invoke.md)) 

## Obtaining the data for invocation

The bot uses a predefined SPARQL query that is evaluated on the data. 
The API-speific bot implementation uses the query result to make the API call(s) required to fulfill the bot's purpose.
 
## Handling Web API responses
Responses can be copied to the communication channel as-is, wrapped in msg:textMessage triples, or the bot can use a generic or API specific translation mechanism.



