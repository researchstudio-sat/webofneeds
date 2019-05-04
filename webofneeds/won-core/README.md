## WoN Security architecture


The WoN Atoms and Nodes are [web identities](doc/web-identity.md) - they are identified by 
resource URIs and are in possession of digital certificates. This means the Atoms don't have to be linked to an 
identity of the user, providing the ground for anonymous Atoms and contributing to user privacy protection. Unless a 
user publishes his own personal information as part of Atom semantic data, it is not possible to make a conclusion 
about the real identity of a user by crawling publicly available WoN linked data. 


All the data exchanged in WoN are formed as Message resources. They are RDF datasets, composed of named graphs, made 
available as linked data. All the messages issued by Node or Atom [are signed](doc/message-signatures.md) by them. 
The receiving or viewing party processes the message or data only if it is well-formed and verifies against the 
expected signer. The node always publishes the messages it receives or sends, together with their signatures - as a 
result the messages become part of linked data.


During the [data transmission](doc/data-channels.md), the data is protected by TLS. This contributes to users' 
privacy by protecting their data during message exchange from unauthorized disclosure, modification and replay. 


After transmission, the WoN messages are published as linked data by Nodes. To further protect user privacy, WoN 
enforces restricted access to some of such data - since it can contain sensitive information. For example, a user 
wants to send a message with his telephone number to another user. That other user should be able to access the 
message, while it should not be available publicly. WoN protocol imposes usage of WebID-TLS based 
[access control](doc/access-control.md) for such cases.

In order to support this architecture, the [authentication](doc/authentication.md) in WoN is performed at 
linked data level (authentication of Atoms, Nodes and their Messages) and application level (authentication of Owner, 
Node and Matcher applications). 

The very rough overview of the classes involved in secure environment setup and usage can be found 
[here](doc/security-classes.pdf).

### Security Future work
* Key revocation, including when a key is compromised, is not yet accounted for in our architecture;
*	Integration of user definable access control rules to their data, i.e. 
[web access control](https://www.w3.org/wiki/WebAccessControl);
*	Possibility to link user Atom with external Identity and internal identities, useful for cases when users don't 
want to use Web of Needs anonymously, such as companies, or for group Atoms;
* Web Identity for Matcher and Owner applications - possibly as special Atom resources;
*	Making use of Matcher identity for its reputation and trust;
* Improvement of message metadata for communication traceability, - i.e. implementation of previous 
message/originator linking including their signature referencing.
