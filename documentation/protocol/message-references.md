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
`Open`, `Close`, `Connect`, the newest message stored in the WoN node 
for the respective connection is selected. If there is no message stored 
for the respective connection, the `Create`
message that created the Need is selected. 
*Note:* The messages that create 
the connection are assigned to the connection, so they will be selected
by Rule 3 for the subsequent messages.
4. Any message in the connection that is still unreferenced is selected.

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


This approach ensures the following invariants on the directed graph 
obtained by all message references. 
1. The `Create` message of a need must not contain a reference
1. Any message other than a `Create` message must contain at least one reference
1. Source and target of a Message reference must be in the same eventContainer
  with the exception of references pointing to the `Create` message.
1. There must be no directed circles in the graph
1. From any message, other than a `Create` message, there must be 
at least one path to the need's `Create` message
1. A Response must always reference the message that it is a response to.

*TODO explain NeedCreatedNotificationMessage*

##  Invariants in SPARQL
SPARQL queries to test message structure invariants
ASK queries must yield false
SELECT/CONSTRUCT queries must yield empty results

```
# The `Create` message of a need must not contain a reference
prefix msg: <http://purl.org/webofneeds/message#>
select ?msg where {
  ?msg msg:hasMessageType msg:CreateMessage;
	   msg:hasPreviousMessage ?msg.
}
```
```
# Any message other than a `Create` message must contain at least one reference
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX msg: <http://purl.org/webofneeds/message#>
PREFIX won: <http://purl.org/webofneeds/model#>
SELECT * WHERE {
  ?msg msg:hasMessageType ?msgType .
  FILTER NOT EXISTS {?msg msg:hasPreviousMessage ?msg2}  
  FILTER (?msgType != msg:CreateMessage && ?msgType != msg:NeedCreatedNotificationMessage)
}
```
```
# Source and target of a Message reference must be in the same 
# eventContainerwith the exception of references pointing to 
# the `Create` message.
prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix msg: <http://purl.org/webofneeds/message#>
prefix won: <http://purl.org/webofneeds/model#>
select * where {
  ?cnt rdfs:member ?msg .
  ?cnt2 rdfs:member ?msg2 .
  ?msg msg:hasPreviousMessage ?msg2 .
  ?msg2 msg:hasMessageType ?targetType .
  filter (?cnt != ?cnt2 && ?targetType != msg:CreateMessage)
}
```
```
# There must be no directed circles in the graph
PREFIX msg: <http://purl.org/webofneeds/message#>
SELECT ?msg WHERE {
  {
  	?msg msg:hasPreviousMessage ?msg .  
  } UNION {
  	?msg msg:hasPreviousMessage ?msg2 . 
  	?msg2 msg:hasPreviousMessage* ?msg
  } 
}
```
```
# From any message, other than a `Create` message, there must be 
# at least one path to the need's `Create` message
PREFIX msg: <http://purl.org/webofneeds/message#>
SELECT ?msg WHERE {
  ?msg msg:hasMessageType ?msgType.
  FILTER NOT EXISTS {
    ?msg msg:hasPreviousMessage* ?createMsg.
    ?createMsg msg:hasMessageType msg:CreateMessage.
  }
  FILTER (?msgType != msg:NeedCreatedNotificationMessage)
}
```

```
# A Response must always reference the message that it is a response to.
PREFIX msg: <http://purl.org/webofneeds/message#>
SELECT * WHERE {
  {
  	?resp a msg:FromSystem .
	?resp msg:isResponseTo ?msg .
  } UNION {
	?resp a msg:FromOwner .
	?resp msg:isResponseTo ?msg .
  } UNION {
	?resp a msg:FromExternal .
	?resp msg:isRemoteResponseTo ?msg .    
  }
  {
  	?resp msg:hasMessageType msg:SuccessResponse.
  } UNION {
    ?resp msg:hasMessageType msg:FailureResponse.
  } 
  FILTER NOT EXISTS {
  	?resp msg:hasPreviousMessage ?msg. 
  }
}
```

## Useful queries for messages

Find all messages in temporal ordering
```
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX msg: <http://purl.org/webofneeds/message#>
PREFIX won: <http://purl.org/webofneeds/model#>
SELECT distinct ?first ?msg ?distance ?text ?msgType ?time ?rem WHERE {
 {
   SELECT distinct ?first ?msg (count (?mid) as ?distance) WHERE {
     ?msg msg:hasPreviousMessage* ?mid .
     ?mid msg:hasPreviousMessage+ ?first .
     FILTER NOT EXISTS {?first msg:hasPreviousMessage ?none}            
   }
   GROUP BY ?msg ?first 
 }
 OPTIONAL {
   ?msg won:hasTextMessage ?text.
   ?msg msg:hasMessageType ?msgType.
 }
 OPTIONAL {
    ?msg msg:hasCorrespondingRemoteMessage ?rem . 
   ?rem won:hasTextMessage ?text.
   ?rem msg:hasMessageType ?msgType.
 }
 ?msg msg:hasReceivedTimestamp ?time.  
} ORDER BY ?first ?distance ?time
```