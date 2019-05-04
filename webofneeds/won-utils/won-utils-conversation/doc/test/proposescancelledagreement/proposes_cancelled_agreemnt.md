### This query functions like the agreement function (or AgreementFunction.java), but it utilizes a construct query that builds
### triples instead of building named graphs with a select query
### oneValidProposalToCancel 
**input**: one-agreement-one-cancellation.trig
**output**: one-agreement-one-cancellation.ttl
**test name**: oneValidProposalToCancel
*// This is the case where there is one valid proposal*

**input**:

```
<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            won:textMessage  "two" ;
            agr:proposes event:5669098069340991000 .
}

<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            won:textMessage  "Please go on." ;
            agr:accepts event:557600936467257340 .
}

<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            won:textMessage  "validate" ;
            agr:proposesToCancel event:ow321nn1va6clfidc4s0 .
}

<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            won:textMessage  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:1435888415723958200 .
}

```

 
**output**: 
 
 ```
event:557600936467257340    agr:proposes   event:5669098069340991000 .  
 ```
### twoProposalOneAgreementOneCancellationError
**input**: 2proposal-one-agreement-errormsg-one-cancellation.trig

**input**:

```
<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            won:textMessage  "hi" .
}
<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            won:textMessage  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." ;
agr:proposes event:6671551888677331000 .
}

<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            won:textMessage  "one" .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            won:textMessage  "I'm not sure I understand you fully." ;
            agr:proposes event:5669098069340991000 .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            won:textMessage  "two" ;
            agr:accepts event:usi9yhill1lo2xi70sjx ;
            agr:accepts event:5669098069340991000 .
}
<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            won:textMessage  "validate" ;
            agr:proposesToCancel event:557600936467257340.
}
<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            won:textMessage  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:1435888415723958200 .
}
```

**output**: 

```
event:usi9yhill1lo2xi70sjx agr:proposes event:6671551888677331000 .

```

### twoProposalOneAgreementOneCancellationmsgError
**input**: 2proposal-one-agreement-one-cancellation-msgerror.trig

**input**:

```
<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            won:textMessage  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}

<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            won:textMessage  "one" .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            won:textMessage  "I'm not sure I understand you fully." ;
            agr:proposes event:5669098069340991000 ;
            agr:proposes event:usi9yhill1lo2xi70sjx .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            won:textMessage  "two" ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc .
}
<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            won:textMessage  "validate" ;
            agr:proposesToCancel event:usi9yhill1lo2xi70sjx .
}

<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            won:textMessage  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:1435888415723958200 .
}
```

**output**:  NOTHING

### twoProposalOneAgreementOneCancellation
**input**: 2proposal-one-agreement-one-cancellation.trig

**input**:

```
<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            won:textMessage  "hi" .
}
<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            won:textMessage  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." ;
agr:proposes event:6671551888677331000 .
}

<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            won:textMessage  "one" .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            won:textMessage  "I'm not sure I understand you fully." ;
            agr:proposes event:5669098069340991000 .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            won:textMessage  "two" ;
            agr:accepts event:usi9yhill1lo2xi70sjx ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc .
}
<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            won:textMessage  "validate" ;
            agr:proposesToCancel event:557600936467257340.
}
<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            won:textMessage  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:1435888415723958200 .
}
```

**output**: 

```
event:00d5wzbbf8hzzt2eewcc agr:proposes event:5669098069340991000 .
event:usi9yhill1lo2xi70sjx agr:proposes event:6671551888677331000 .
```

### cancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes
**input**: cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig

**input**:

```
<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            won:textMessage  "hi" .
}

<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            won:textMessage  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}


<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            won:textMessage  "one" ;
            agr:proposes event:6671551888677331000 .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            won:textMessage  "I'm not sure I understand you fully." ;
            agr:accepts event:5669098069340991000 ;
            agr:proposes event:usi9yhill1lo2xi70sjx .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            won:textMessage  "two" ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc ;
            agr:proposesToCancel event:00d5wzbbf8hzzt2eewcc .
}

<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            won:textMessage  "Please go on." ;
            agr:accepts event:557600936467257340 .
}
```

**output**: 

```
event:5669098069340991000
        agr:proposes  event:6671551888677331000 .
```

### oneAgreementOneCancellationTestProposalError
**input**: one-agreement-one-cancellation-proposal-error.trig

**input**:

```
<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            won:textMessage  "one" .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            won:textMessage  "two" ;
            agr:proposes event:5669098069340991000 .
}
<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            won:textMessage  "Please go on." ;
            agr:accepts event:557600936467257340 .
}
<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            won:textMessage  "validate" ;
            agr:proposesToCancel event:557600936467257340 .
}
<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            won:textMessage  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:1435888415723958200 .
}
```

**output**: NOTHING

### oneAgreementTwoCancellationSameAgreement
**input**: one-agreement-two-cancellation-same-agreement.trig

**input**:

```
<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            won:textMessage  "hi" .
}

<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            won:textMessage  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." ;
            agr:proposes  event:6671551888677331000 .
}
<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            won:textMessage  "one" ;
            agr:accepts event:usi9yhill1lo2xi70sjx .
}
<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            won:textMessage  "I'm not sure I understand you fully." ;
            agr:proposesToCancel event:5669098069340991000 .
            
}
<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            won:textMessage  "two" ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc .
}
<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            won:textMessage  "Please go on." ;
            agr:proposesToCancel event:5669098069340991000 .
}
<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            won:textMessage  "validate" ;
            agr:accepts event:ow321nn1va6clfidc4s0 .
}
```

**output**:

```
event:usi9yhill1lo2xi70sjx agr:proposes  event:6671551888677331000 .
```

##### also add one with both clauses accepted..change up the envelopes for the proposalToCancel too...
 ### twoProposaltwoAgreementstwoCancellationProposalClausesOneAccepted
**input**:  2proposal-2agreements-2cancellationproposal-1clauses-oneaccepted.trig
**output**: 2proposal-2agreements-2cancellationproposal-1clauses-oneaccepted.ttl

**input**: 

```
<https://localhost:8443/won/resource/event/6149800720990867000#content> {
    event:6149800720990867000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            won:textMessage  "Hello, debugbot!" .
}
 
 <https://localhost:8443/won/resource/event/gv6zk2yqk6o8bl574n36#content-paqe> {
    event:gv6zk2yqk6o8bl574n36
            won:textMessage  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}
 
 <https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
    event:cbcccoqqqbec6bxkl3y3
            won:textMessage  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." ;
            agr:proposes event:gv6zk2yqk6o8bl574n36 .
}
 
 <https://localhost:8443/won/resource/event/1107469913331435500#content> {
    event:1107469913331435500
            won:textMessage  "usage" ;
            agr:proposes event:6149800720990867000 .
}
 
 <https://localhost:8443/won/resource/event/4846251213444807000#content> {
    event:4846251213444807000
            won:textMessage  "one" ;
            agr:accepts event:cbcccoqqqbec6bxkl3y3 .
}
 
 <https://localhost:8443/won/resource/event/152dum7y56zn95qyernf#content-19tl> {
    event:152dum7y56zn95qyernf
            won:textMessage  "I'm not sure I understand you fully." ;
            agr:accepts event:1107469913331435500 .
} 
 
 <https://localhost:8443/won/resource/event/4055709708568209400#content> {
    event:4055709708568209400
            won:textMessage  "two" ;
            agr:proposesToCancel event:152dum7y56zn95qyernf .
}
 
 <https://localhost:8443/won/resource/event/cgqt5h004iql2003me2n#content-lqq7> {
    event:cgqt5h004iql2003me2n
            won:textMessage  "Please go on." ;
            agr:proposesToCancel event:4846251213444807000 .
}
 
 <https://localhost:8443/won/resource/event/8863100035920837000#content> {
    event:8863100035920837000
            won:textMessage  "validate" ;
            agr:accepts event:cgqt5h004iql2003me2n .
}

<https://localhost:8443/won/resource/event/uu3ciy3btq6tg90crr3b#content-fswl> {
    event:uu3ciy3btq6tg90crr3b
            won:textMessage  "Ok, I'm going to validate the data in our connection. This may take a while." .
}
```

**output**:

```
event:cbcccoqqqbec6bxkl3y3 agr:proposes  event:gv6zk2yqk6o8bl574n36 .
```

### twoProposaltwoAgreementstwoCancellationProposalClausesTwoAccepted
**input**: 2proposal-2agreements-2cancellationproposal-1clauses-twoaccepted.trig
**output**: 2proposal-2agreements-2cancellationproposal-1clauses-twoaccepted.ttl

**input**:
```

<https://localhost:8443/won/resource/event/gv6zk2yqk6o8bl574n36#content-paqe> {
    event:gv6zk2yqk6o8bl574n36
            won:textMessage  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}

<https://localhost:8443/won/resource/event/6149800720990867000#content> {
    event:6149800720990867000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            won:textMessage  "Hello, debugbot!" .
}

<https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
    event:cbcccoqqqbec6bxkl3y3
            won:textMessage  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." ;
            agr:proposes event:gv6zk2yqk6o8bl574n36 .
}

<https://localhost:8443/won/resource/event/4846251213444807000#content> {
    event:4846251213444807000
            won:textMessage  "one" ;
            agr:accepts event:cbcccoqqqbec6bxkl3y3 .
}

<https://localhost:8443/won/resource/event/1107469913331435500#content> {
    event:1107469913331435500
            won:textMessage  "usage" ;
            agr:proposes event:6149800720990867000 .
}

<https://localhost:8443/won/resource/event/152dum7y56zn95qyernf#content-19tl> {
    event:152dum7y56zn95qyernf
            won:textMessage  "I'm not sure I understand you fully." ;
            agr:accepts event:1107469913331435500 .
} 

<https://localhost:8443/won/resource/event/cgqt5h004iql2003me2n#content-lqq7> {
    event:cgqt5h004iql2003me2n
            won:textMessage  "Please go on." ;
            agr:proposesToCancel event:4846251213444807000 .
}

<https://localhost:8443/won/resource/event/4055709708568209400#content> {
    event:4055709708568209400
            won:textMessage  "two" ;
            agr:proposesToCancel event:152dum7y56zn95qyernf .
}

<https://localhost:8443/won/resource/event/8863100035920837000#content> {
    event:8863100035920837000
            won:textMessage  "validate" ;
            agr:accepts event:cgqt5h004iql2003me2n .
}

<https://localhost:8443/won/resource/event/uu3ciy3btq6tg90crr3b#content-fswl> {
    event:uu3ciy3btq6tg90crr3b
            won:textMessage  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:4055709708568209400 .
}

```