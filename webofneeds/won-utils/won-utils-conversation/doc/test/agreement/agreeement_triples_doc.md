### noAgreementsTest
*no-agreements.trig (there are no agreement triples, expect output to be blank)*

### oneAgreementTest 
*one-agreement.trig (there is one agreement .. expect agreement in output)*

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
```

### oneAgreementOneCancellationTest
*one-agreement-one-cancellation.trig (valid cancellation. This should produce a blank file)*

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

<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            con:text  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:1435888415723958200 .
}
```

### oneAgreementOneCancellationTestProposalError
*one-agreement-one-cancellation-proposal-error.trig 
( propose to cancel an a proposal instead of an agreement .. this will result in intact agreement )*

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

### oneAgreementTwoProposalClauses
*one-agreement-two-proposal-clauses.trig*

```
<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." .
}
<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:proposes event:5669098069340991000 ;
            agr:proposes event:00d5wzbbf8hzzt2eewcc .
}
<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            con:text  "Please go on." ;
            agr:accepts event:557600936467257340 .
}
```

### oneAgreementMissingProposal
*one-agreement-missing-proposal.trig*

```
<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            con:text  "Please go on." ;
            agr:accepts event:557600936467257340 .
}
```

### oneAgreementMissingClause
*one-agreement-missing-clause.trig (the content graph: event:5669098069340991000#content is missing.)*

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
```

### exposeJenaBug
*expose-jena-bug.trig*

### noAgreementOneCancellationError 
*no-agreement-one-cancellation-error.trig
(there is no acceptance of  the original proposal. The acceptance graph was deleted event:ow321nn1va6clfidc4s0 )*

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

### twoProposalOneAgreement
*2proposal-one-agreement.trig (this should  create a proposal with two clauses)*

```
<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "hi" .
}
<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" .
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
```

### twoProposalOneAgreementOneCancellationmsgError
*2proposal-one-agreement-one-cancellation-msgerror.trig*

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

### twoProposalOneAgreementOneCancellation
*2proposal-one-agreement-one-cancellation.trig  (this creates and agreement with two clauses then cancels it …this should produce an empty file)*

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
### twoProposalOneAgreementError
*2proposal-one-agreement-errormsg.trig (a text message instead of a proposal is accepted in an agreement, making one of the proposals in the agreement invalid  ... produces a file an agreement with only the valid clause content

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
```

**output**:

```
<https://localhost:8443/won/resource/event/557600936467257340> {
    <https://localhost:8443/won/resource/event/6671551888677331000>
            <https://w3id.org/won/core#socket>
                    <https://w3id.org/won/core#OwnerSocket> ;
            <https://w3id.org/won/core#targetSocket>
                    <https://w3id.org/won/core#OwnerSocket> ;
            <https://w3id.org/won/content#text>
                    "hi" .
}
```



### twoProposalOneAgreementOneCancellationError
*2proposal-one-agreement-errormsg-one-cancellation.trig (a text message instead of a proposal is accepted in an agreement, making one of the proposals in the agreement invalid  … produces a blank file)*

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

### oneAgreementTwoCancellationSameAgreement
*one-agreement-two-cancellation-same-agreement.trig (two cancellations of the same agreement, the second cancellation should be the same  … produce an empty file)*

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

### twoProposalTwoAgreementsOneCancellation
*2proposal-2agreements-onecancellation.trig (two agreements formed. One is cancelled.)*

**But there are not enough parts of the conversation to make this a valid test…see the bold letters below**

```
<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "hi" .
}

<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" .
}

<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." ;
            agr:proposes event:6671551888677331000 .
}
<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:accepts event:usi9yhill1lo2xi70sjx .
}
<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:proposes event:5669098069340991000 .
}
# this is an error. I am accepting a proposal from myself…
<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            con:text  "Please go on." ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc .
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

### oneProposalTwoAccepted
*one-proposal-two-accepted-corrected.trig (accept a clause instead of a proposal for one agreement, accept a proposal (forming a valid agreement) for the second)*

```
<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" .
}

<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:proposes event:usi9yhill1lo2xi70sjx .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc .
}

<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            con:text  "Please go on." ;
            agr:accepts event:5669098069340991000 .
}
```

### oneAgreementTwoAccepts
*oneproposal-twoaccepts.trig (accepting a proposal twice .. this should produce a normal agreement like one-agreement.trig ? … which one forms the agreement? …ask Florian .. when this code runs the last accept is what is saved.. Florian says we get two agreements, and we do.)*

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
<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            con:text  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:557600936467257340 .
}
```

### oneSelfAcceptedAgreement
*one-self-accepted-agreement.trig*

```
<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:proposes event:5669098069340991000 .
}
<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            con:text  "validate" ;
            agr:accepts event:557600936467257340 .
}
```

### oneAcceptBeforeProposeAgreement 
*one-accept-before-propose-agreement.trig    (fix this …. )*

```
<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" .
}
<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:accepts   event: ow321nn1va6clfidc4s0 .
 .}
<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            con:text  "Please go on." ;
           agr:proposes  event:5669098069340991000 .
 .}
```

### oneSelfAcceptedAgreementSameGraph
*one-self-accepted-agreement-same-graph.trig (returns nothing)*

```
<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:proposes event:5669098069340991000 ;
            agr:accepts event:557600936467257340 .
}..
```

### falseAgreementUsingSuccessResponses
*false-agreement-using-success-responses.trig*

```
<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "hi" .
}

<https://localhost:8443/won/resource/event/t1h3u3s5q6is5sewv843#envelope-i81z> {
    <https://localhost:8443/won/resource/event/5669098069340991000#envelope-ma7y-sig>
            a                               sig:Signature ;
            sig:signatureValue           "MGUCMQD0H3I+Y3Y27WJgS6B3lLpDPhtI1odks1YXXd61zmYE0/DVLLWiMQoyDn6H8JQKY/ACMGz1OKAt2e+rg9xCOm53tP56gctuzUS6Qi6Es9Q6fZep8j20iihafRyE41uYcxvAtQ==" ;
            sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
            msg:hash                     "C8nYCZS1IgqOdjLk3f73I9dSekaGxFlNEPKwAAfBFuA/P3aoLEE8ypNLylRearbSS+xx4CuRwulCIfdRxDkZlydctRvVPOFr48u1lS04nPNlL43EMI4r21myMUbjIlo1VEjopgSJoUc/M1IlXcBcN1ir/p4W5zVc5GMy+2Jh5eo=" ;
            msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
            msg:signedGraph              <https://localhost:8443/won/resource/event/5669098069340991000#envelope-ma7y> .
    
    event:t1h3u3s5q6is5sewv843
            a                            msg:FromSystem ;
            msg:messageType           msg:SuccessResponse ;
            msg:previousMessage       event:5669098069340991000 ;
            msg:receivedTimestamp     1513345783608 ;
            msg:recipient              conn:xq6jofxo2w8o05io6dre ;
            msg:recipientAtom          atom:7820503869697675000 ;
            msg:recipientNode          <https://localhost:8443/won/resource> ;
            msg:sender                conn:xq6jofxo2w8o05io6dre ;
            msg:senderAtom            atom:7820503869697675000 ;
            msg:senderNode            <https://localhost:8443/won/resource> ;
            msg:isResponseTo             event:5669098069340991000 ;
            msg:isResponseToMessageType  msg:ConnectionMessage ;
            msg:protocolVersion          "1.0" ;
            agr:proposes                 event:6671551888677331000 .
    
    <https://localhost:8443/won/resource/event/t1h3u3s5q6is5sewv843#envelope-i81z>
            a                      msg:EnvelopeGraph ;
            msg:containsSignature  <https://localhost:8443/won/resource/event/5669098069340991000#envelope-ma7y-sig> ;
            <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
                    event:t1h3u3s5q6is5sewv843 .
}


<https://localhost:8443/won/resource/event/omh6mnwfqczgjimg3sch#envelope-b7qv> {
    event:omh6mnwfqczgjimg3sch
            a                            msg:FromSystem ;
            msg:correspondingRemoteMessage
                    event:dt0zlr5lc7zreq741bqn ;
            msg:messageType           msg:SuccessResponse ;
            msg:previousMessage       event:krcmdz8802701wpzlzl8 ;
            msg:receivedTimestamp     1513345783929 ;
            msg:recipient              conn:4t9deo4t6bqx83jxk5ex ;
            msg:recipientAtom          atom:nte803gz4pbiw6equ3qa ;
            msg:recipientNode          <https://localhost:8443/won/resource> ;
            msg:sender                conn:xq6jofxo2w8o05io6dre ;
            msg:senderAtom            atom:7820503869697675000 ;
            msg:senderNode            <https://localhost:8443/won/resource> ;
            msg:isRemoteResponseTo       event:00d5wzbbf8hzzt2eewcc ;
            msg:isResponseTo             event:krcmdz8802701wpzlzl8 ;
            msg:isResponseToMessageType  msg:ConnectionMessage ;
            msg:protocolVersion          "1.0" ;
            agr:accepts                  event:t1h3u3s5q6is5sewv843 .
    
    <https://localhost:8443/won/resource/event/omh6mnwfqczgjimg3sch#envelope-b7qv>
            a                      msg:EnvelopeGraph ;
            msg:containsSignature  <https://localhost:8443/won/resource/event/krcmdz8802701wpzlzl8#envelope-qdj9-sig> ;
            <http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf>
                    event:omh6mnwfqczgjimg3sch .
    
    <https://localhost:8443/won/resource/event/krcmdz8802701wpzlzl8#envelope-qdj9-sig>
            a                               sig:Signature ;
            sig:signatureValue           "MGUCMQDp4EW/TRcVjt36HYwuMqtupgfsFkedLfEw0AyMWociyv0Cd+sAir+FYbb97y88b8ACMDT19mFthP4FOu2hUqYH7f+GKDMGqRiCsg7mN96vwNy8wOcnLL7xMtoOY4CxBvEYwg==" ;
            sig:hasVerificationCertificate  <https://localhost:8443/won/resource> ;
            msg:hash                     "AJn3h4w+jd7mvcbsZBb700m26pJu+FZMWgXuSawF1zSbRAPpLqbURsL/FSea42XmDU94Fe1Ztuoj6dr4dDNsx4BucPanY3GrFKyvGM/zorObmvVHSE1uyrDja8ssZJ0tmCwzbNRW94n1boeygM/CaWa7V84qz+p/F7549APX+roC" ;
            msg:publicKeyFingerprint     "Sk7FwsKgm/SxPIjRVipweRj/6PYMlJlh9ubLMqz3myY=" ;
            msg:signedGraph              <https://localhost:8443/won/resource/event/krcmdz8802701wpzlzl8#envelope-qdj9> .
}

==================================
Fixed tests. 
```

### agreementProposesHintFeedback
*agreement-proposes-in-hint-feedback-clause.trig  (I expected an empty file when proposing a Hint Feedback Message, but I got the contents of the feedback message. Can I propose anything? Am I restricted by envelope type for agr:proposes and agr:accepts triples?)*

```
<https://localhost:8443/won/resource/event/4672813835273634000#content> {
    event:4672813835273634000
            con:feedback  [ con:feedbackTarget      conn:xq6jofxo2w8o05io6dre ;
                               con:binaryRating  con:Good
                             ] .
}

<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" ;
            agr:proposes  event:4672813835273634000 .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:accepts event:5669098069340991000 .
}

Test output:

diff - only in expected:
diff - only in actual:
<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc> {
    <https://localhost:8443/won/resource/event/4672813835273634000>
            <https://w3id.org/won/content#feedback>
                    [ <https://w3id.org/won/content#feedbackTarget>
                              <https://localhost:8443/won/resource/connection/xq6jofxo2w8o05io6dre> ;
                      <https://w3id.org/won/content#binaryRating>
                              <https://w3id.org/won/content#Good>
                    ] .
}
```

### agreementProposesConnectMessage
*agreement-proposes-connect-message.trig (I expected an empty file when proposing a Connect Message, but I got the contents of the feedback message. Can I propose anything? Am I restricted by envelope type for agr:proposes and agr:accepts triples?)*

```
<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "hi" .
}

<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" ;
            agr:proposes event:6671551888677331000 .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:accepts event:5669098069340991000 .
}

Test output:

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc> {
    <https://localhost:8443/won/resource/event/6671551888677331000>
            <https://w3id.org/won/core#socket>
                    <https://w3id.org/won/core#OwnerSocket> ;
            <https://w3id.org/won/core#targetSocket>
                    <https://w3id.org/won/core#OwnerSocket> ;
            <https://w3id.org/won/content#text>
                    "hi" .
}
```

### agreementConnectMessageProposesHintFeedbackMessage
*agreementConnectMessageProposesHintFeedbackMessage.trig 

(a connect message proposes a hint feedback message, and the agreement works, should it??)
*

```
<https://localhost:8443/won/resource/event/4672813835273634000#content> {
    event:4672813835273634000
            con:feedback  [ con:feedbackTarget      conn:xq6jofxo2w8o05io6dre ;
                               con:binaryRating  con:Good
                             ] .
}

<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "hi" ;
            agr:proposes event:4672813835273634000 .
}


<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." ;
            agr:accepts event:6671551888677331000 .
}

test (output):

<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx> {
    <https://localhost:8443/won/resource/event/4672813835273634000>
            <https://w3id.org/won/content#feedback>
                    [ <https://w3id.org/won/content#feedbackTarget>
                              <https://localhost:8443/won/resource/connection/xq6jofxo2w8o05io6dre> ;
                      <https://w3id.org/won/content#binaryRating>
                              <https://w3id.org/won/content#Good>
                    ] .
}
```

### twoAgreementsSharingEnvelopeforAcceptsPurposes   two-
*two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig*

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
            agr:accepts event:00d5wzbbf8hzzt2eewcc .
}
```

### cancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes
*cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig*

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

### oneAgreementProposedProposaldAgent
*one-agreement-proposed-proposal-dagent.trig   (In the test, expect this to produce a regular agreement)*

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
            con:text  "one" ;
            agr:proposes event:usi9yhill1lo2xi70sjx .
}

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:accept event:5669098069340991000 .
}
```

### oneAgreementProposedProposalsAgent
*one-agreement-proposed-proposal-sagent.trig   (In the test, expect this to produce an empty agreement)*

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

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:proposes event:usi9yhill1lo2xi70sjx .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc .
}
```

### selfCancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes
*(stopped here on the 8th /won-utils-goals/src/test/resources/won/utils/agreement/expected/self-cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig    self-cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig*

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
            agr:proposesToCancel event:557600936467257340 .
}

<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            con:text  "Please go on." ;
            agr:accepts event:557600936467257340 .
}

compare with: 
```
### cancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes 
*cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig*

```
<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc ;
            agr:proposesToCancel event:00d5wzbbf8hzzt2eewcc .
}

Is replaced with:

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:accepts event:00d5wzbbf8hzzt2eewcc ;
            agr:proposesToCancel event:557600936467257340 .
}



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