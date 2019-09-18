# Message References

In WoN, messages are chained using cryptographic signatures. This serves
to ensure non-repudiability of the message history - both sides of a conversation
can prove the message history.

This property is enforced by the WoN nodes. Whenever a WoN node receives
a message from another node, an owner, a matcher, or from an internal
process, it adds an envelope with a timestamp etc to state that it got the
message. Then it adds message references. These are references in the following form:

```
<messageUri> msg:previousMessage <previousMessageUri>
```

Where `messageUri` is the URI of the message currently being processed and
`previousMessageUri` is the URI of the referenced message.
In addition to this triple, a signature reference is added to the message:

_TODO: add signature reference_

The message references serve the following purposes:

1. No message except for the last one that is sent in a conversation is
   unreferenced.
2. All messages except for the one creating an atom contain references

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
   message that created the Atom is selected.
   _Note:_ The messages that create
   the connection are assigned to the connection, so they will be selected
   by Rule 3 for the subsequent messages.
4. Any message in the connection that is still unreferenced is selected.

_Note:_ due to asynchronous message processing in a WoN node, message ordering
may not be total - messages may appear to happen at the same time. Therefore,
the graph of message references described here is not acyclic. It defines
a partial ordering on the messages. The timestamp can be used as a tie-breaker
if timestamps differ, otherwise, some messages have to be regarded as simultaneous.

_Note:_ due to network latency and asynchronous processing, it is possible that two
messages `A` and `B` may be represented on particpant `Pa`'s WoN node as `A`
happening before `B`, while they may, on `Pa`'s counterpart `Pb`'s WoN node
as `B` happening before `A`. This may happen independently of which participants
send those messages. In order to detect such a case and take measures to avoid
confusion, both participants should be able to access each other's messages.
_TODO_ there may be a way to detect such a case solely based on message references.

This approach ensures the following invariants on the directed graph
obtained by all message references.

1. The `Create` message of an atom must not contain a reference
1. Any message other than a `Create` message must contain at least one reference
1. Source and target of a Message reference must be in the same messageContainer
   with the exception of references pointing to the `Create` message.
1. There must be no directed circles in the graph
1. From any message, other than a `Create` message, there must be
   at least one path to the atom's `Create` message
1. A Response must always reference the message that it is a response to.

_TODO explain AtomCreatedNotificationMessage_

## Invariants in SPARQL

SPARQL queries to test message structure invariants
ASK queries must yield false
SELECT/CONSTRUCT queries must yield empty results

```
# The `Create` message of an atom must not contain a reference
prefix msg: <https://w3id.org/won/message#>
select ?msg where {
  ?msg msg:messageType msg:CreateMessage;
	   msg:previousMessage ?msg.
}
```

```
# Any message other than a `Create` message must contain at least one reference
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX msg: <https://w3id.org/won/message#>
PREFIX won: <https://w3id.org/won/core#>
SELECT * WHERE {
  ?msg msg:messageType ?msgType .
  FILTER NOT EXISTS {?msg msg:previousMessage ?msg2}
  FILTER (?msgType != msg:CreateMessage && ?msgType != msg:AtomCreatedNotificationMessage)
}
```

```
# Source and target of a Message reference must be in the same
# messageContainerwith the exception of references pointing to
# the `Create` message.
prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix msg: <https://w3id.org/won/message#>
prefix won: <https://w3id.org/won/core#>
select * where {
  ?cnt rdfs:member ?msg .
  ?cnt2 rdfs:member ?msg2 .
  ?msg msg:previousMessage ?msg2 .
  ?msg2 msg:messageType ?targetType .
  filter (?cnt != ?cnt2 && ?targetType != msg:CreateMessage)
}
```

```
# There must be no directed circles in the graph
PREFIX msg: <https://w3id.org/won/message#>
SELECT ?msg WHERE {
  {
  	?msg msg:previousMessage ?msg .
  } UNION {
  	?msg msg:previousMessage ?msg2 .
  	?msg2 msg:previousMessage* ?msg
  }
}
```

```
# From any message, other than a `Create` message, there must be
# at least one path to the atom's `Create` message
PREFIX msg: <https://w3id.org/won/message#>
SELECT ?msg WHERE {
  ?msg msg:messageType ?msgType.
  FILTER NOT EXISTS {
    ?msg msg:previousMessage* ?createMsg.
    ?createMsg msg:messageType msg:CreateMessage.
  }
  FILTER (?msgType != msg:AtomCreatedNotificationMessage)
}
```

```
# A Response must always reference the message that it is a response to.
PREFIX msg: <https://w3id.org/won/message#>
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
  	?resp msg:messageType msg:SuccessResponse.
  } UNION {
    ?resp msg:messageType msg:FailureResponse.
  }
  FILTER NOT EXISTS {
  	?resp msg:previousMessage ?msg.
  }
}
```

## Useful queries for messages

Find all messages in temporal ordering

```
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX msg: <https://w3id.org/won/message#>
PREFIX won: <https://w3id.org/won/core#>
SELECT distinct ?first ?msg ?distance ?text ?msgType ?time ?rem WHERE {
 {
   SELECT distinct ?first ?msg (count (?mid) as ?distance) WHERE {
     ?msg msg:previousMessage* ?mid .
     ?mid msg:previousMessage+ ?first .
     FILTER NOT EXISTS {?first msg:previousMessage ?none}
   }
   GROUP BY ?msg ?first
 }
 OPTIONAL {
   ?msg con:text ?text.
   ?msg msg:messageType ?msgType.
 }
 OPTIONAL {
    ?msg msg:correspondingRemoteMessage ?rem .
   ?rem con:text ?text.
   ?rem msg:messageType ?msgType.
 }
 ?msg msg:receivedTimestamp ?time.
} ORDER BY ?first ?distance ?time
```
