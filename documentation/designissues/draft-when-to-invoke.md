# Knowing when to invoke a Web Service

This document describes a possible approach used by a bridging bot to determine when to invoke a Web Service.
 
## Assumptions
1. The bot can how to connect to a suitable counterpart need.
1. The bot has stated [Information Requirements](draft-stating-information-requirements.md) and obtained the required information.
1. The bot has [populated the parameters](draft-parameters-for-Web-Services.md) for invoking the Web Service method
 
## Getting the counterpart to trigger the invocation
 
### In-band communication
The simple solution for triggering the invocation (essentially, performing a 'commit' on a transaction between the two needs) is using special messages. The bot can announce 'ready to commit', the counterpart sends a 'commit', and if the latter does not happen, the bot sends an 'abort' after some timeout.

### Out-of-band communication
Another option is to create a second connection between the two needs (the bot's and the counterpart's), that has a different 'Facet' - e.g, the BAPCParticipant/BAPCCoordinator facets. Thus, the transaction can be part of a bigger, distributed transaction between more parties, and the coordinator's WoN node can coordinate the distributed transaction (or rather, the business activity).
In that case, during the setup of the business activity, the last message exchanged in the other connection should be referenced such that it is clear for both sides which state of the conversation the participants are trying to agree to.
 
