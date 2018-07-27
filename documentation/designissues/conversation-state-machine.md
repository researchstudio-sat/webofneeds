# Conversation State Machines

## Requirements
We use conversations to represent all sorts of relationships, many of them intrinsically stateful processes, such as a transport job. We want to represent these processes on top of the agreement protocol. We need:
* unambiguous representation of process state
* determine possible next actions per participant

We cannot use the conversation state in the WoN protocol to represent a higher-level state as it is used 
to reflect the state of the communication channel and that should be its only use.

Moreover, the possible states and their transitions can be very different depending on the use case.

Note: As we are designing an end-to-end protocol that the clients have to agree on, we are flexible. We can design something simple at first and add other protocols later without having to worry about the WoN nodes.

## Solution 1: Finite State Machines

Using the agreement protocol the conversation partners have the ability to agree on RDF data. 
Let them agree on a set of triples that defines a finite state machine.
The definition of that state machine contains 
* a set of states
* the indication of the initial state
* a definition of state transitions that depend on events. 
* a set of events

That definition can be interpreted on both sides of the conversation - with identical results on both sides.
The result of such an interpretation is the state after the last event that happened.
The interpretation only takes triples into account that have been agreed upon. 

### Implementation
This protocol is a higher-level, end-to-end protocol. It has to be implemented by all clients. 
Important questions:
* Who proposes a state machine and how are they authored?
	* Bots: have predefined state machine templates
	* Users in owner webapp: maybe also predefined templates? In the beginning: TTL picker and some predefined ones in text files.
* How does one check if a proposed state machine is acceptable?
	* In the beginning: look closely
	* later: visualize it? calculate formal proofs? Probably, if it is too complicated to analyze visually, it should be broken up in multiple smaller ones.
	* later: only reference an external definition (the generally accepted state machine for transport, for example), and compare checksums
	
### Validity of Events 
We said that state machines only take data into account that are agreed upon. That may be cumbersome, because the state will not change when you state that an event took place, only when your counterpart agrees it did. We could include data that has been proposed, but that's inaccurate: If I propose something, it is clear that it may not be agreed upon. Moreover, I could propose multiple, exclusive things (not implemented yet but planned). However, when I say: 'I just delivered your package' that is not a proposition. From my point of view it immediately receives truth status. Later, my counterpart may reject this assertion, or explicitly accept it. We might introduce a new message type into the agreement protocol, 'agr:asserts', that adds the asserted triples to the agreement triples immediately without the need to be accepted, but can later be rejected by the counterpart or retracted by the asserter, in which case the triples are removed again. We'll have to think about how that influences the rest of the protocol, especially, what happens to agreements made between an assertion and its rejection? (intuition: they become invalid and have to be renewed)

### Format
Example
```
_:state1 a wsm:State;
		 a wsm:InitialState;
		 dc:title 'Start'.
		 
_:state2 a wsm:State;
		 dc:title 'Payment received'.

_:state3 a wsm:State;
		 dc:title 'Service rendered'.
		 
_:state4 a wsm:State;
		 a wsm:TerminalState;
		 dc:title 'All Done'.
		 
_:transition1 a wsm:Transition;
			wsm:triggeredBy _:paymentEvent;
			wsm:fromState :state1;
			wsm:toState :state2;		 
		 
_:transition2 a wsm:Transition;
			wsm:triggeredBy _:serviceEvent;
			wsm:fromState :state1;
			wsm:toState :state3;		 

_:transition1 a wsm:Transition;
			wsm:triggeredBy _:serviceEvent;
			wsm:fromState :state2;
			wsm:toState :state4;		 

_:transition3 a wsm:Transition;
			wsm:triggeredBy _:paymentEvent;
			wsm:fromState :state3;
			wsm:toState :state4;		 	
				 
```
With events:
```
(in ex:msg1 from one of the participants)
ex:msg1 wsm:hasEvent _:paymentEvent.

(in ex:msg1 from one of the participants)
ex:msg2 wsm:hasEvent _:serviceEvent.

```

## Solution 2: Petri Nets

Same as Solution 1, but using Petri Nets instead of FSMs. 

Arguments for: 
* smaller networks in case of parallel tasks than with FSMs
* simple and clear execution semantics

Arguments against:
* Not as easy to understand as BPMN
* Tooling not as good as for BPMN
* There are many flavors. We may have to choose.


## Solution 3: BPMN

Same as Solution 2, but using Petri Nets instead of FSMs. Main arguments for that: smaller networks in case of parallel tasks

Arguments for:
1. Tooling, Community, Documentation,...
2. If we want more complex things processes, choreographies etc., we can build them

Arguments against:
1. A lot of dead weight
2. No unambiguous semantics (I believe)

