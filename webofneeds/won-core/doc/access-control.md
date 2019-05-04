#	Access Control

## 1.	Linked data READ access

Web of Needs manages the access control to linked data stored on the Node via implementation of WebID-TLS protocol 
and application of pre-defined access control rules. Linked data stored by Nodes include Atom data, messages between 
Atom and Node, data about Atom connections, messages exchanged between Atoms and other Atoms, Nodes and Matchers on 
those connections. In order to protect user privacy, WoN enforces restricted access to some of such data - since it 
can contain sensitive information. For example, a user wants to send a message with his telephone number to another 
user. That other user should be able to access the message, while it should not be available publicly. WoN protocol 
imposes usage of WebID+TLS based access control for such cases. In current implementation, all the message 
resources have restricted access - only the message sender Atom, sender Node, receiver Atom and receiver Node are 
allowed to access it. To all other requesters the access is denied. Other linked data resources are publicly 
available in the current implementation (no authentication is required). 

### 1.1.	WebID-TLS for access control
In order to decide whether the party accessing the restricted resource has right for such access, this party must be 
authenticated. WebID+TLS offers the following approach:
*	Apply standard TLS for proving that the requester really possesses the private key for the certificate he presents 
when accessing the protected resource;
*	Apply WebID verification for proving that the requester is really the one he says (in his certificate) he is: a 
WebID URI specified in the certificate a requester presents, must resolve to a resource where the public key is 
specified and is exactly the same as the public key in the certificate.

WoN implements such approach by specifying message resources published on Node as those for which HTTPS access is 
required, and by configuring and implementing WebID verification filter for accessing these message resources. It is 
possible to use WebID+TLS in WoN in the first place, because Atoms and Nodes are identities with their own 
certificates and with the corresponding public key published in their resource profile. Therefore, when they act as 
clients accessing resources on Node, it is possible to authenticate them with WebID+TLS. According to WebID+TLS, the 
WebID URI is specified as part of the certificate in the alternative names field.


After the requester is authenticated, the access control rules for the resource define whether the access to that 
resource can be granted to the requester.


The benefits of using WebID+TLS for access control on Node are following:
*	Node doesn't have to know anything about users accessing its resources – no user data bases, which is benefitial 
both in terms of privacy and it terms of scalability;
*	Flexible about access control rules specification: as mentioned above, currently we use default rules defining 
message access, but it is possible to extend it, so that the users themselves define access to their resources via 
RDF resource definition.

### 1.2.	Access control rules
Currently, WoN uses pre-defined rules for access control of messages (only senders and receivers can access it), and 
all other resources are publicly accessible. The pre-defined access control rules should in future be replaced, or 
completed with the rules, defined by the users for their resources themselves, such as according to web access 
control draft standard. It should also be considered, to make
 it possible for users to define access to their resources on per-named-graph basis. For example, an Atom definition 
 could consist of public parts, such as information about a user selling a couch, and private parts, such as user 
 address, which he can provide only to the user with clear interest to buy the couch.

Current WoN **Linked data READ access control** is implemented in [won-node-webapp module](../../won-node-webapp). 
See also [documentation](../../won-node-webapp/doc/linked-data-access.md) there.

## 2.	READ access to JMS queues
On JMS channels in WoN the consumer access to queues on broker that are intended for specific Owner applications is 
restricted, so that no other application can access it. In other words, messages for Atoms managed by Owner 
application X cannot be read by Owner application Y, even if the broker has trusted and has JMS channels with both 
Owner applications.

This access control is implemented in WoN by configuring broker to use MessageAuthorizationPolicy
and [implementing this policy](../../won-cryptography/src/main/java/won/cryptography/service/MessageOwnerConsumptionPolicy.java) based on Owner 
public key pinning. The queues that are intended for consumption by a particular Owner application contain that 
application's ID in the queue name. This ID, in its turn, is the hash of the Owner's certificate. If the consumer is 
able to provide a certificate (verified over TLS) that corresponds to the hash of the given Owner ID, the access is 
granted. Otherwise the access is denied.

## 3.	WRITE access
Above we have considered linked data resources access on Node for READ action. When creating linked data itself, i.e.
 WRITE actions on Nodes, authentication and authorization is implemented via digital signatures. The following rules 
 apply here:
*	Each action that can result in a linked data published on Node must be a message;
*	Such message must be well-formed WoN message signed by the message creators.

Authentication of message senders and authorization for WRITE right (publishing) is done by verifying the signature 
against the expected, according to the message type, message senders and recipients, signers. For example, if a Node 
receives a message signed by Atom X with recipient Atom Y on already established communication between these two 
Atoms and the signatures in the message verify, the Node will accept this message and will WRITE it as part of the 
existing communication between these Atoms (publish it). Otherwise the message will be discarded. More details on 
digital signatures can be found in [message digital signatures](message-signatures.md).






