# Message References
In WoN, messages are chained using cryptographic signatures. This serves 
to ensure non-repudiability of the message history - both sides of a conversation 
can prove the message history. 
  
This property is enforced by the WoN nodes. Whenever a WoN node receives 
a message from another node, an owner, a matcher, or from an internal 
process, it adds an envelope with a timestamp etc to state that it got the 
message. Then it adds message references. These are references in the following form:
```
<messageUri> msg:hasPreviousMessage <previousMessageUri>
```
Where `messageUri` is the URI of the message currently being processed and 
```previousMessageUri``` is the URI of the referenced message. 
In addition to this triple, a signature reference is added to the message:

*TODO: add signature reference*

The message references serve the following purposes:
1. No message except for the last one that is sent in a conversation is 
unreferenced.
2. All messages except for the one creating a need contain references

Thus, each `Create` message is the root of an unmodifiable, directed graph
of messages.

In order to determine which messages are to be referenced, the following 
algorithm is applied to populate a set `referencedMessages`of messages 
to be referenced: 
1. If the message is a `Create` message, no messages are selected.
2. If the message is a ResponseMessage (`SuccessResponse` or `FailureResponse`), 
the message that is being responded to is selected.
3. If the message is specific to a connection (`ConnectionMessage` , 
`Open`, `Close`, `Connect` (if it is used in an already existing connection)) 
the newest message stored in the WoN node for the respective connection 
is selected.
4. If the message creates a connection (`Connect`, `Hint`), the `Create`
message that created the Need is selected. *Note:* The messages that create 
the connection are assigned to the connection, so they will be selected
by Rule 3 for the subsequent messages.
5. Any message in the connection that is still unreferenced is selected.

*Note:* due to asynchronous message processing in a WoN node, message ordering 
may not be total - messages may appear to happen at the same time. Therefore,
the graph of message references described here is not acyclic. It defines 
a partial ordering on the messages. The timestamp can be used as a tie-breaker 
if timestamps differ, otherwise, some messages have to be regarded as simultaneous.
 
*Note:* due to network latency and asynchronous processing, it is possible that two 
messages `A` and `B` may be represented on particpant `Pa`'s WoN node as `A` 
happening before `B`, while they may, on `Pa`'s counterpart `Pb`'s WoN node 
as `B` happening before `A`. This may happen independently of which participants 
send those messages. In order to detect such a case and take measures to avoid
confusion, both participants should be able to access each other's messages.
*TODO* there may be a way to detect such a case solely based on message references. 




