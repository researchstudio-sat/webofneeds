# Delivery Chain

The messages involved in the delivery and acknowledgment of a message logically form a chain. (If you are strict about it, the structure is a DAG.)
This structure is called a *delivery chain*.

## Structure

These structures are different depending on the type of message.
* *atom-specific* messages are only exchanged between an atom's owner and the WoN node (CREATE, DELETE, REPLACE, DEACTIVATE, ACTIVATE). They are stored in the atom's message container.
* *connection-specific* messages are exchanged between two atoms. Each atom maintains a *connection* entity for this communication channel. The messages are stored in each connection's message container.

In the following depictions, the arrows are labeled with their RDF property names.

In the case of an *atom-specific* message, it consists of the message `m1` and the WoN node's success response `s1`:

![Delivery chain in an atom's message container ](img/atom-delivery-chain.png)

In the case of a *connection-specific* message, it consists of the message `m1A` and the WoN node's success response `s1A` (*sender response*) as well as 
the recipient node's success response `s1B` (*recipient response*). This structure is the same for two atoms that are hosted by the same node; logically, the response can be said to come from the atom. 

Note: In the following, we always identify the left side with `A` and the right side with `B`

![Delivery chain in a connection's message container ](img/conn-delivery-chain.png)

## Message Delivery
The following depictions show which messages are exchanged. Each arrow in the sequence diagram represents one RDF dataset being sent from one participant to another. The object(s) depicted above the arrow represent the WoN messages encoded in the RDF dataset.

### Delivery for an Atom's Message Container

Simple in/out behavior:

![Atom message exchange](img/atom-message-exchange.png)

Message 2 contains `m1` and `s1`, not just `s1`, as one might suspect. `m1` is called an *echo*, and it is delivered to all clients registered as the owner of the atom that sends `m1`, along with the WoN node's response. Thus, when one client sends a message, all clients are informed of that message immediately.

### Delivery for an Connection's Message Container

Message `m1` travels from `ownerA` to `nodeA`, to `nodeB`, to `ownerB`. The responses `s1A` and `s1B` are delivered to both `ownerA` and `ownerB`. The sequence is depicted in the following diagram.

![Connection message exchange](img/conn-message-exchange.png)             

After these exchanges, both owners/nodes end up with `m1`, `s1A`, and `s1B`.

Message 2 contains `m1` and `s1A`, not just `s1A`, as one might suspect. `m1` is called an *echo*, and it is delivered to all clients registered as the owner of the atom that sends `m1`, along with the WoN node's response. Thus, when one client sends a message, all clients are informed of that message immediately.

Message `s1B` is sent back to the `A` side without repeating `m1` and `s1A`, because the `A` side already knows them. In order to emphasize that `s1B` references them, we still draw the respective arrows pointing into blank space.

## Message Log Integrity

Each message is content-addressed, i.e., its URI is calculated based on its entire content. It is thus not possible to change a message after calculating its URI without breaking this relationship. 

By referencing earlier messages in a conversation, the conversation becomes immutable in the sense that alteration of any part of it can be detected. Each message is also signed by its author, so no other participant could modify the contents without detection. In combination, only the last message in a conversation could be modified (and subsequently its URI recalculated) by one of the participants.

In WoN it is the responsibility of the WoN nodes to link new messages to earlier ones. They do this by adding message references to the SuccessResponses they generate. (FailureResponses are never used in this way). These references are realized using the RDF property 
`msg:previousMessage`, which always point to an earlier SuccessResponse. Thus, a SuccessResponse gets *confirmed*. The actual user-generated (or system-generated) messages are not confirmed directly - they are referenced by SuccessResponses (via `msg:respondingTo`) that eventually get confirmed.

The confirmation algorithm differs for an atom's message container and for a connection's message container.

### Atom's Message Container

An atom's message container only holds user-generated messages, system-generated messages, and the atom's responses.

#### Confirmation Algorithm

For each message container a *confirmation list* is maintained, which contains the URIs of those messages that have not been confirmed yet. 
* Each time a SuccessResponse is appended to the message container, the messages it confirms are removed from the confirmation list.
* Each time a SuccessResponse is created, all URIs in the confirmation list are added as `msg:previousMessage` properties. 

#### Examples

![Chain in atom's message container](img/atom-message-container.png)

In this case, `m1`, `m2`, and `m3` were processed sequentially, therefore their SuccessResponses link up in a chain. 

When messages are processed in parallel, the chain forks. A subsequent SuccessResponse then contain references to multiple previous messages and thereby reunites the forked chains:

![Chain in atom's message container](img/atom-message-container-fork.png)



### Connection's Message Container

An connection's message container holds user-generated messages and system-generated messages from both atoms, as well as both atom's responses.

In this setting, confirmation is the mechanism to inform the communication partner of the receipt of one of their responses. Once we know that our response was received by our partner, we know that we both have all messages of that delivery chain in our message containers.

#### Confirmation Algorithm

For each message container a *confirmation list* `C` is maintained, which contains the URIs of those messages that have not been confirmed yet. 
* Each time a SuccessResponse `s` from the other connection is appended to the message container
    - The messages it confirms transitively are removed from the confirmation list:
        + load all messages referenced by `s` via `msg:previousMessage` - these are our responses 
        + extract from them the URIs *they* reference via `msg:previousMessage` and remove them from `C`
    - `s` is added to `C`
* Each time a SuccessResponse is created, all URIs in the confirmation list are added as `msg:previousMessage` properties. 

Simply put, we confirm our partner's SuccessResponse `s` until we receive a SuccessResponse `s'` that confirms one of our messages that confirms `s`.

#### Examples

In the following examples, `A` denotes the connection of the atom on the left side, `B` denotes the connection of the atom on the right side. `s1A` is the SuccessResponse from `A`, in response to message `m1`, `s1B`is the response of `B`, and so on.

##### Example 1: one message from each side

![One message from each side](img/conn-message-container.png)

After the first delivery chain is *finished* (both responses are present), `C(B) = {s1A}` and ` C(A) = {s1B}`. In the second delivery chain, when `B` confirms `s1A` it is actually the second time it refrences it. `B` must do that because `s1B` might have been lost or delayed. `A` confirms `s1B` because it is its first chance to do so.

##### Example 2: two messages from the same side

![Two messages from same side](img/conn-message-container-same-side.png)

After the first delivery chain is finished, as in the first example,`C(B) = {s1A}` and `c(A) = {s1B}`. However, now the message comes from the owner of `A`. The difference is the order in which `s2A` and `s2B` are created: `s2A` confirms `s1B`, which, according to the algorithm, removes `s1A` from `C(B)`. Therefore, `s2B` only references `s2A` - which, upon receipt by `A` removes `s1B` from `C(A)`.

##### Example 3: lost response

![Three messages from same side, response is late or lost](img/conn-message-container-late-response.png)

This example is a possible continuation of  Example 2, in which `s2B` is late or lost. The consequence is that `s1B` is not removed from `C(A)`. When `A` creates the response `s3A`, it therefore references `s1B` again. The lost response also causes `s3B` to reference `s2A` again. Only when `A` actually receives `s3B` will `s1B` be removed from `C(A)`.

##### Example 4: two simultaneous messages

![Two simultenaous messages from either side](img/conn-message-container-2simultaneous.png)

Here, `m2` and `m3` are sent roughly the same time, such that they are both processed by the sending node before they are both processed by the receiving node. Their delivery chains do not influence each other. After this exchange, `C(A) = {s2B, s3B}` and  `C(B) = {s2A, s3A}`. 
                          
##### Example 5: delayed (or almost simultaneous) message

![Delayed message](img/conn-message-container-delayed-message.png)

After `m1` is finished, `B` sends `m2`. It is processed by the sending node, but then gets delayed. Meanwhile, A sends `m3`, which is processed completely. Only then, the receiving node processes `m2`. The references in the delivery chain of `m3` change the confirmation lists, such that `s2B` references  `s3B` instead of `s1B`. 

The delay need not be long: if the timing in Example 4 is was a little different, the outcome might be this. Alternatively, `m2` could be delayed for much longer, letting many more exchanges happen before the response `s2A` is added to the container. 

It is always possible for a node to create this situation artificially by referencing responses from earlier in the chain. For the recipient node, this is indistinguishable from a message that really is delayed by the network. Conversely, a node can delay a *recipient response* artificially and create it only after having processed other messages in the container; this, too, is indistinguishable from an actual network delay for the original sender. 

