This returns all pending proposalsToCancel .. it is like proposaltocancel, but it returns triples containing the proposaltocancel instead of agreements that have been proposed to be cancelled.

 ### oneOpenCancellationPropsoal
 **input**: one-agreement-one-unacceptedcancellation.trig
 **output**: one-agreement-one-unacceptedcancellation.ttl
 **test name**: oneOpenCancellationPropsoal
 
 **input**:
 
 ```
 <https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" .
}

 <https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:proposes event:5669098069340991000 .
}

<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            con:text  "Please go on." ;
            agr:accepts event:557600936467257340 .
}

<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            con:text  "validate" ;
            agr:proposesToCancel event:ow321nn1va6clfidc4s0 .
}
```

**output**:
```
event:1435888415723958200
        agr:proposesToCancel  event:ow321nn1va6clfidc4s0 .
```

 
 
 ### twoOpenCancellationOneCancellationSameProposal
 **input**:2proposal-2agreements-1cancellationproposal-2clauses-noneaccepted.trig
 **output**:2proposal-2agreements-1cancellationproposal-2clauses-noneaccepted.ttl
 **test name**: twoOpenCancellationOneCancellationSameProposal
 
  **input**:
 
 ```
 <https://localhost:8443/won/resource/event/6149800720990867000#content> {
    event:6149800720990867000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "Hello, debugbot!" .
}


<https://localhost:8443/won/resource/event/gv6zk2yqk6o8bl574n36#content-paqe> {
    event:gv6zk2yqk6o8bl574n36
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}

<https://localhost:8443/won/resource/event/1107469913331435500#content> {
    event:1107469913331435500
            con:text  "usage" ;
            agr:proposes event:6149800720990867000 .
}

<https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
    event:cbcccoqqqbec6bxkl3y3
            con:text  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." ;
            agr:proposes event:gv6zk2yqk6o8bl574n36 .
}


<https://localhost:8443/won/resource/event/4846251213444807000#content> {
    event:4846251213444807000
            con:text  "one" ;
            agr:accepts event:cbcccoqqqbec6bxkl3y3 .
}

<https://localhost:8443/won/resource/event/152dum7y56zn95qyernf#content-19tl> {
    event:152dum7y56zn95qyernf
            con:text  "I'm not sure I understand you fully." ;
            agr:accepts event:1107469913331435500 .
}

<https://localhost:8443/won/resource/event/cgqt5h004iql2003me2n#content-lqq7> {
    event:cgqt5h004iql2003me2n
            con:text  "Please go on." ;
            agr:proposesToCancel event:152dum7y56zn95qyernf ;
            agr:proposesToCancel event:4846251213444807000 .
}

```

**output**:   
```
 event:cgqt5h004iql2003me2n
           agr:proposesToCancel event:152dum7y56zn95qyernf ;
           agr:proposesToCancel event:4846251213444807000 .
```

 ### twoProposaltwoAgreementstwoCancellationProposalClausesOneAccepted
 **input**: 2proposal-2agreements-2cancellationproposal-1clauses-oneaccepted.trig
 **output**: 2proposal-2agreements-2cancellationproposal-1clauses-oneaccepted.ttl
 **test name**: twoProposaltwoAgreementstwoCancellationProposalClausesOneAccepted
 
  **input**:
  
  ```
 <https://localhost:8443/won/resource/event/6149800720990867000#content> {
    event:6149800720990867000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "Hello, debugbot!" .
}


<https://localhost:8443/won/resource/event/gv6zk2yqk6o8bl574n36#content-paqe> {
    event:gv6zk2yqk6o8bl574n36
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}

<https://localhost:8443/won/resource/event/1107469913331435500#content> {
    event:1107469913331435500
            con:text  "usage" ;
            agr:proposes event:6149800720990867000 .
}

<https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
    event:cbcccoqqqbec6bxkl3y3
            con:text  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." ;
            agr:proposes event:gv6zk2yqk6o8bl574n36 .
}


<https://localhost:8443/won/resource/event/4846251213444807000#content> {
    event:4846251213444807000
            con:text  "one" ;
            agr:accepts event:cbcccoqqqbec6bxkl3y3 .
}

<https://localhost:8443/won/resource/event/152dum7y56zn95qyernf#content-19tl> {
    event:152dum7y56zn95qyernf
            con:text  "I'm not sure I understand you fully." ;
            agr:accepts event:1107469913331435500 .
}

<https://localhost:8443/won/resource/event/cgqt5h004iql2003me2n#content-lqq7> {
    event:cgqt5h004iql2003me2n
            con:text  "Please go on." ;
            agr:proposesToCancel event:4846251213444807000 .
}

<https://localhost:8443/won/resource/event/4055709708568209400#content> {
    event:4055709708568209400
            con:text  "two" ;
            agr:proposesToCancel event:152dum7y56zn95qyernf .
}

<https://localhost:8443/won/resource/event/8863100035920837000#content> {
    event:8863100035920837000
            con:text  "validate" ;
            agr:accepts event:cgqt5h004iql2003me2n .
}

```
**output**:

```
event:4055709708568209400  agr:proposesToCancel event:152dum7y56zn95qyernf .
```

 ###  twoProposaltwoAgreementstwoCancellationProposalClausesBothAccepted
 **input**: 2proposal-2agreements-2cancellationproposal-1clauses-bothaccepted.trig
 **output**: 2proposal-2agreements-2cancellationproposal-1clauses-bothaccepted.ttl
 **test name**: twoProposaltwoAgreementstwoCancellationProposalClausesBothAccepted
 
  **input**:
  
  ```
  <https://localhost:8443/won/resource/event/6149800720990867000#content> {
    event:6149800720990867000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "Hello, debugbot!" .
}
 
 <https://localhost:8443/won/resource/event/gv6zk2yqk6o8bl574n36#content-paqe> {
    event:gv6zk2yqk6o8bl574n36
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}
 
 <https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
    event:cbcccoqqqbec6bxkl3y3
            con:text  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." ;
            agr:proposes event:gv6zk2yqk6o8bl574n36 .
}
 
 <https://localhost:8443/won/resource/event/1107469913331435500#content> {
    event:1107469913331435500
            con:text  "usage" ;
            agr:proposes event:6149800720990867000 .
}
 
 <https://localhost:8443/won/resource/event/4846251213444807000#content> {
    event:4846251213444807000
            con:text  "one" ;
            agr:accepts event:cbcccoqqqbec6bxkl3y3 .
}
 
 <https://localhost:8443/won/resource/event/152dum7y56zn95qyernf#content-19tl> {
    event:152dum7y56zn95qyernf
            con:text  "I'm not sure I understand you fully." ;
            agr:accepts event:1107469913331435500 .
} 
 
 <https://localhost:8443/won/resource/event/4055709708568209400#content> {
    event:4055709708568209400
            con:text  "two" ;
            agr:proposesToCancel event:152dum7y56zn95qyernf .
}
 
 <https://localhost:8443/won/resource/event/cgqt5h004iql2003me2n#content-lqq7> {
    event:cgqt5h004iql2003me2n
            con:text  "Please go on." ;
            agr:proposesToCancel event:4846251213444807000 .
}
 
 <https://localhost:8443/won/resource/event/8863100035920837000#content> {
    event:8863100035920837000
            con:text  "validate" ;
            agr:accepts event:cgqt5h004iql2003me2n .
}

<https://localhost:8443/won/resource/event/uu3ciy3btq6tg90crr3b#content-fswl> {
    event:uu3ciy3btq6tg90crr3b
            con:text  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:4055709708568209400 .
}
  ```
  
  **output**: NOTHING
 
### oneClosedCancellationOneCancellationErrorSameProposal
 **input**: 2proposal-2agreements-1cancellationproposal-2clauses-onefail.trig
 **output**: 2proposal-2agreements-1cancellationproposal-2clauses-onefail.ttl
 **test name**: oneClosedCancellationOneCancellationErrorSameProposal
 
  **input**:
 
 ```
 <https://localhost:8443/won/resource/event/6149800720990867000#content> {
   event:6149800720990867000
           won:socket        won:OwnerSocket ;
           won:targetSocket  won:OwnerSocket ;
           con:text  "Hello, debugbot!" .
}


<https://localhost:8443/won/resource/event/gv6zk2yqk6o8bl574n36#content-paqe> {
   event:gv6zk2yqk6o8bl574n36
           con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}

<https://localhost:8443/won/resource/event/1107469913331435500#content> {
   event:1107469913331435500
           con:text  "usage" ;
           agr:proposes event:6149800720990867000 .
}

<https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
   event:cbcccoqqqbec6bxkl3y3
           con:text  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." ;
           agr:proposes event:gv6zk2yqk6o8bl574n36 .
}


<https://localhost:8443/won/resource/event/4846251213444807000#content> {
   event:4846251213444807000
           con:text  "one" ;
           agr:accepts event:cbcccoqqqbec6bxkl3y3 .
}

<https://localhost:8443/won/resource/event/152dum7y56zn95qyernf#content-19tl> {
   event:152dum7y56zn95qyernf
           con:text  "I'm not sure I understand you fully." ;
           agr:accepts event:1107469913331435500 .
}

<https://localhost:8443/won/resource/event/cgqt5h004iql2003me2n#content-lqq7> {
   event:cgqt5h004iql2003me2n
           con:text  "Please go on." ;
           agr:proposesToCancel event:152dum7y56zn95qyernf ;
           agr:proposesToCancel event:4055709708568209400 .
}

<https://localhost:8443/won/resource/event/8863100035920837000#content> {
   event:8863100035920837000
           con:text  "validate" ;
           agr:accepts event:cgqt5h004iql2003me2n .
}
 ```
 
   **output**: NOTHING

### twoClosedCancellationOneCancellationSameProposal
 **input**: 2proposal-2agreements-1cancellationproposal-2clauses-bothsucceed.trig
 **output**: 2proposal-2agreements-1cancellationproposal-2clauses-bothsucceed.ttl
 **test name**: oneClosedCancellationOneCancellationErrorSameProposal
 
  **input**:
  
  ```
  <https://localhost:8443/won/resource/event/6149800720990867000#content> {
   event:6149800720990867000
           won:socket        won:OwnerSocket ;
           won:targetSocket  won:OwnerSocket ;
           con:text  "Hello, debugbot!" .
}


<https://localhost:8443/won/resource/event/gv6zk2yqk6o8bl574n36#content-paqe> {
   event:gv6zk2yqk6o8bl574n36
           con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}

<https://localhost:8443/won/resource/event/1107469913331435500#content> {
   event:1107469913331435500
           con:text  "usage" ;
           agr:proposes event:6149800720990867000 .
}

<https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
   event:cbcccoqqqbec6bxkl3y3
           con:text  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." ;
           agr:proposes event:gv6zk2yqk6o8bl574n36 .
}


<https://localhost:8443/won/resource/event/4846251213444807000#content> {
   event:4846251213444807000
           con:text  "one" ;
           agr:accepts event:cbcccoqqqbec6bxkl3y3 .
}

<https://localhost:8443/won/resource/event/152dum7y56zn95qyernf#content-19tl> {
   event:152dum7y56zn95qyernf
           con:text  "I'm not sure I understand you fully." ;
           agr:accepts event:1107469913331435500 .
}

<https://localhost:8443/won/resource/event/cgqt5h004iql2003me2n#content-lqq7> {
   event:cgqt5h004iql2003me2n
           con:text  "Please go on." ;
           agr:proposesToCancel event:152dum7y56zn95qyernf ;
           agr:proposesToCancel event:4846251213444807000 .
}

<https://localhost:8443/won/resource/event/8863100035920837000#content> {
   event:8863100035920837000
           con:text  "validate" ;
           agr:accepts event:cgqt5h004iql2003me2n .
}
  ```
  
   **output**: NOTHING