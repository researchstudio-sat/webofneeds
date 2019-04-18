### oneProposalTwoAcceptsFirstRetractedFirstCancelled
**input**: oneProposalTwoAcceptsFirstRetractedFirstCancelled.trig
**output**: oneProposalTwoAcceptsFirstRetractedFirstCancelled.trig
**test name**:  oneProposalTwoAcceptsFirstRetractedFirstCancelled

**input**:

```
<https://localhost:8443/won/resource/event/6149800720990867000#content> {
    event:6149800720990867000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            won:textMessage  "Hello, debugbot!" .
}

<https://localhost:8443/won/resource/event/1107469913331435500#content> {
    event:1107469913331435500
            won:textMessage  "usage" ;
            agr:proposes  event:6149800720990867000 .
}

<https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
    event:cbcccoqqqbec6bxkl3y3
            won:textMessage  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." ;
            agr:accepts event:1107469913331435500 .
}

<https://localhost:8443/won/resource/event/152dum7y56zn95qyernf#content-19tl> {
    event:152dum7y56zn95qyernf
            won:textMessage  "I'm not sure I understand you fully." ;
            mod:retracts event:cbcccoqqqbec6bxkl3y3 .
} 

<https://localhost:8443/won/resource/event/cgqt5h004iql2003me2n#content-lqq7> {
    event:cgqt5h004iql2003me2n
            won:textMessage  "Please go on." ;
            agr:accepts event:1107469913331435500 .
}

<https://localhost:8443/won/resource/event/8863100035920837000#content> {
    event:8863100035920837000
            won:textMessage  "validate" ;
            agr:proposesToCancel event:cbcccoqqqbec6bxkl3y3 .
}

<https://localhost:8443/won/resource/event/uu3ciy3btq6tg90crr3b#content-fswl> {
    event:uu3ciy3btq6tg90crr3b
            won:textMessage  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts  event:8863100035920837000 .
}
```
**expected output**: NOTHING

**actual output**:

```
<https://localhost:8443/won/resource/event/cgqt5h004iql2003me2n> {
    <https://localhost:8443/won/resource/event/6149800720990867000>
            <https://w3id.org/won/core#socket>
                    <https://w3id.org/won/core#OwnerSocket> ;
            <https://w3id.org/won/core#targetSocket>
                    <https://w3id.org/won/core#OwnerSocket> ;
            <https://w3id.org/won/core#textMessage>
                    "Hello, debugbot!" .
}
```

### oneAgreementProposalRetracted
**input**: one-agreement-proposes-retracted.trig
**output**: one-agreement-proposes-retracted.trig
**test name**:  oneAgreementProposalRetracted

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
            mod:retracts event:557600936467257340 .
}
```

**expected output**:

```
<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0> {
    <https://localhost:8443/won/resource/event/5669098069340991000>
            <https://w3id.org/won/core#textMessage>
                    "one" .
}
```

### oneAgreementAcceptsRetracted
**input**: one-agreement-accepts-retracted.trig
**output**: one-agreement-accepts-retracted.trig
**test name**:  oneAgreementAcceptsRetracted

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

<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            won:textMessage  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            mod:retracts event:ow321nn1va6clfidc4s0 .
}
```

**expected output**:

```
<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0> {
    <https://localhost:8443/won/resource/event/5669098069340991000>
            <https://w3id.org/won/core#textMessage>
                    "one" .
}
```
### oneAgreementProposalRetractedb4Accept
**input**: one-agreement-proposes-retracted-b4-accept.trig
**output**: one-agreement-proposes-retracted-b4-accept.trig
**test name**:  oneAgreementProposalRetractedb4Accept

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

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            won:textMessage  "I'm not sure I understand you fully." ;
            mod:retracts event:usi9yhill1lo2xi70sjx .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340 won:textMessage  "two" ;
    agr:accepts event:usi9yhill1lo2xi70sjx .
}

```

**expected output**: NOTHING

### retractProposalTocancelAfterAgreement
**input**: one-agreement-proposaltocancel-retracted.trig
**output**: one-agreement-proposaltocancel-retracted.trig
**test name**:  retractProposalTocancelAfterAgreement

```
<https://localhost:8443/won/resource/event/6149800720990867000#content> {
    event:6149800720990867000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            won:textMessage  "Hello, debugbot!" .
}

<https://localhost:8443/won/resource/event/1107469913331435500#content> {
    event:1107469913331435500
            won:textMessage  "usage" ;
            agr:proposes  event:6149800720990867000 .
}

<https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
    event:cbcccoqqqbec6bxkl3y3
            won:textMessage  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." ;
            agr:accepts event:1107469913331435500 .
}

<https://localhost:8443/won/resource/event/8863100035920837000#content> {
    event:8863100035920837000
            won:textMessage  "validate" ;
            agr:proposesToCancel event:cbcccoqqqbec6bxkl3y3 .
}

<https://localhost:8443/won/resource/event/uu3ciy3btq6tg90crr3b#content-fswl> {
    event:uu3ciy3btq6tg90crr3b
            won:textMessage  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts  event:8863100035920837000 .
}

<https://localhost:8443/won/resource/event/6834006177130613000#content> {
    event:6834006177130613000
            won:textMessage  "validate" ;
            mod:retracts event:8863100035920837000 .
}
```

**expected output**:  NOTHING

**present output**:

```
<https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3> {
    <https://localhost:8443/won/resource/event/6149800720990867000>
            <https://w3id.org/won/core#socket>
                    <https://w3id.org/won/core#OwnerSocket> ;
            <https://w3id.org/won/core#targetSocket>
                    <https://w3id.org/won/core#OwnerSocket> ;
            <https://w3id.org/won/core#textMessage>
                    "Hello, debugbot!" .
}

```

### retractProposalTocancelBeforeAccept
**input**: one-agreement-proposaltocancel-retracted-b4-accept.trig
**output**: one-agreement-proposaltocancel-retracted-b4-accept.trig
**test name**:  retractProposalTocancelBeforeAccept

```
<https://localhost:8443/won/resource/event/6149800720990867000#content> {
    event:6149800720990867000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            won:textMessage  "Hello, debugbot!" .
}

<https://localhost:8443/won/resource/event/1107469913331435500#content> {
    event:1107469913331435500
            won:textMessage  "usage" ;
            agr:proposes  event:6149800720990867000 .
}

<https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
    event:cbcccoqqqbec6bxkl3y3
            won:textMessage  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." ;
            agr:accepts event:1107469913331435500 .
}

<https://localhost:8443/won/resource/event/8863100035920837000#content> {
    event:8863100035920837000
            won:textMessage  "validate" ;
            agr:proposesToCancel event:cbcccoqqqbec6bxkl3y3 .
}


<https://localhost:8443/won/resource/event/6834006177130613000#content> {
    event:6834006177130613000
            won:textMessage  "validate" ;
            mod:retracts event:8863100035920837000 .
}

<https://localhost:8443/won/resource/event/1tr3o22co1907d6b6n7s#content-nrkb> {
    event:1tr3o22co1907d6b6n7s
            won:textMessage  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts  event:8863100035920837000 .
}

```

**expected output**:  

```
<https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3> {
    <https://localhost:8443/won/resource/event/6149800720990867000>
            <https://w3id.org/won/core#socket>
                    <https://w3id.org/won/core#OwnerSocket> ;
            <https://w3id.org/won/core#targetSocket>
                    <https://w3id.org/won/core#OwnerSocket> ;
            <https://w3id.org/won/core#textMessage>
                    "Hello, debugbot!" .
}

```
