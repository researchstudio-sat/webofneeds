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

<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            con:text  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:1435888415723958200 .
}

```

 
 **output**: 
 
 ```
event:1435888415723958200 agr:proposesToCancel  event:ow321nn1va6clfidc4s0 .  
 ```
 
### twoProposalOneAgreementOneCancellation
**input**: 2proposal-one-agreement-one-cancellation.trig
**output**: 2proposal-one-agreement-one-cancellation.ttl
**test name**: twoProposalOneAgreementOneCancellation
*// This is the case where there is one valid proposal*

**input**:

```
<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "hi" .
}
<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." ;
agr:proposes event:6671551888677331000 .
}

<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:proposes event:5669098069340991000 .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:accepts event:usi9yhill1lo2xi70sjx ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc .
}
<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            con:text  "validate" ;
            agr:proposesToCancel event:557600936467257340.
}
<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            con:text  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:1435888415723958200 .
}
```

**output**: 
 
 ```
event:1435888415723958200  agr:proposesToCancel event:557600936467257340 .  
 ```

### oneAgreementTwoCancellationSameAgreement
**input**: one-agreement-two-cancellation-same-agreement.trig
**output**: one-agreement-two-cancellation-same-agreement.ttl
**test name**: oneAgreementTwoCancellationSameAgreement
*// This is the case where there is one valid proposal*

**input**:

```
<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "hi" .
}

<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." ;
            agr:proposes  event:6671551888677331000 .
}
<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" ;
            agr:accepts event:usi9yhill1lo2xi70sjx .
}
<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:proposesToCancel event:5669098069340991000 .
            
}
<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc .
}
<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            con:text  "Please go on." ;
            agr:proposesToCancel event:5669098069340991000 .
}
<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            con:text  "validate" ;
            agr:accepts event:ow321nn1va6clfidc4s0 .
}
```

**output**:

```
 event:00d5wzbbf8hzzt2eewcc  agr:proposesToCancel event:5669098069340991000 .
 event:ow321nn1va6clfidc4s0 agr:proposesToCancel event:5669098069340991000 . 
 ```

### oneAgreementOneCancellationTestProposalError
**input**: one-agreement-one-cancellation-proposal-error.trig
**output**: one-agreement-one-cancellation-proposal-error.ttl
**test name**: oneAgreementOneCancellationTestProposalError
*// This is the case where there is one valid proposal*

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
            agr:proposesToCancel event:557600936467257340 .
}
<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            con:text  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:1435888415723958200 .
}
```

**output**: NOTHING


### cancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes
**input**: cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig
**output**: cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.ttl
**test name**: cancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes
*// This is the case where there is one valid proposal*

**input**:

```
<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "hi" .
}

<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}


<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" ;
            agr:proposes event:6671551888677331000 .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:accepts event:5669098069340991000 ;
            agr:proposes event:usi9yhill1lo2xi70sjx .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc ;
            agr:proposesToCancel event:00d5wzbbf8hzzt2eewcc .
}

<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            con:text  "Please go on." ;
            agr:accepts event:557600936467257340 .
}
```

**output**:

```
event:557600936467257340 agr:proposesToCancel event:00d5wzbbf8hzzt2eewcc .
```

### twoProposalOneAgreementOneCancellationError 
**input**: 2proposal-one-agreement-errormsg-one-cancellation.trig
**output**: 2proposal-one-agreement-errormsg-one-cancellation.ttl
**test name**: twoProposalOneAgreementOneCancellationError
*// This is the case where there is one valid proposal*

**input**:

```
<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "hi" .
}
<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." ;
agr:proposes event:6671551888677331000 .
}

<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:proposes event:5669098069340991000 .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:accepts event:usi9yhill1lo2xi70sjx ;
            agr:accepts event:5669098069340991000 .
}
<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            con:text  "validate" ;
            agr:proposesToCancel event:557600936467257340.
}
<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            con:text  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:1435888415723958200 .
}
```

**output**:

```
event:1435888415723958200
        agr:proposesToCancel  event:557600936467257340 .
```

### twoProposalOneAgreementOneCancellationmsgError
**input**: 2proposal-one-agreement-one-cancellation-msgerror.trig
**output**: 2proposal-one-agreement-one-cancellation-msgerror.ttl
**test name**: twoProposalOneAgreementOneCancellationError
*// This is the case where there is one valid proposal*

**input**:

```
<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}

<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:proposes event:5669098069340991000 ;
            agr:proposes event:usi9yhill1lo2xi70sjx .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc .
}
<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            con:text  "validate" ;
            agr:proposesToCancel event:usi9yhill1lo2xi70sjx .
}

<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            con:text  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:1435888415723958200 .
}
```

**output**: NOTHING

