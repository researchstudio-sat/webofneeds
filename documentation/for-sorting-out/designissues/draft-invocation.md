# Invocation

This document describes how a bot (or user) executes an operation it offers (for example, invoking a Web Service)
 
## Assumptions
1. The bot knows how to connect to a suitable counterpart atom.
1. The bot's atom specifies an operation, connected to the atom via `[atom]/won:performs`.
1. The bot has stated [Information Requirements](draft-stating-information-requirements.md) and obtained the required information.
1. The bot has [populated the parameters](draft-parameters-for-Web-Services.md) for invoking the Web Service method
 
## Invocation, part 1: Proposal
In that state, the bot creates a *proposal* (see our [DeSemWeb2017 paper](http://ceur-ws.org/Vol-1934/contribution-07.pdf)), covering
* The messages containing the parameters
* A message describing the operation
    * stating the operation ([atom] `won:executes` [operation])
    * attaching parameters to the operation using property paths (one for each parameter)
* A message referencing all messages containing relevant data (parameters and execution) using `agr:proposes`

## Invocation, part 2: Accepting
When receiving a proposal the counterpart can send a message referencing the proposing message using `agr:accepts`.

## Return value
Some operations may produce a return value, but most will at least require some sort of confirmation that the operation was at least attempted.
In order to represent such a confirmation, the callee can send messages indicating status (for longer operations, status updates may be helpful, in others, only one message indicates the operation was executed or attempted (in the case of no return value).
