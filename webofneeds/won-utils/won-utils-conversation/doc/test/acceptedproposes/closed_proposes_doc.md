### This query functions like the agreement function (or AgreementFunction.java), but it utilizes a construct query that builds
### triples instead of building named graphs with a select query
### oneValidProposal  
**input**: one-agreement.trig
**output**: one-agreement.ttl
**test name**: oneValidProposal
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


```

 
 **output**: 
 
 ```
event:557600936467257340 agr:proposes event:5669098069340991000  .
 ```
 
### oneValidProposalwithOneAgreementOneCancellationTest
**input**: one-agreement-one-cancellation.trig
**output**: one-agreement-one-cancellation.ttl
**test name**: oneValidProposalwithOneAgreementOneCancellationTest

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

<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            con:text  "Ok, I'm going to validate the data in our connection. This may take a while." ;
            agr:accepts event:1435888415723958200 .
}
```

**output**: NOTHING

### oneValidProposalWithOneAgreementOneCancellationTestProposalError
**input**: one-agreement-one-cancellation-proposal-error.trig
**output**: one-agreement-one-cancellation-proposal-error.ttl
**test name**:  oneValidProposalWithOneAgreementOneCancellationTestProposalError

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

**output**: 

```
event:557600936467257340  agr:proposes event:5669098069340991000 .
```

### twoValidProposalWithoneAgreementTwoProposalClauses
**input**: one-agreement-two-proposal-clauses.trig
**output**: one-agreement-two-proposal-clauses.ttl
**test name**: twoValidProposalWithoneAgreementTwoProposalClauses

**input**:
**output**: 

```
event:557600936467257340
            agr:proposes event:5669098069340991000 ;
            agr:proposes event:00d5wzbbf8hzzt2eewcc 
```

### noValidProposaloneAgreementMissingProposal
**input**: one-agreement-missing-proposal.trig
**output**: one-agreement-missing-proposal.ttl
**test name**: noValidProposaloneAgreementMissingProposal

**input**:

```
<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            con:text  "Please go on." ;
            agr:accepts event:557600936467257340 .
}
```

**output**: NOTHING

### noValidProposaloneAgreementMissingClause
**input**: one-agreement-missing-clause.trig
**output**: one-agreement-missing-clause.ttl
**test name**: noValidProposaloneAgreementMissingClause

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
```

**output**: NOTHING

### validProposalNoAcceptanceNoAgreementOneCancellationError
**input**: no-agreement-one-cancellation-error.trig
**output**: no-agreement-one-cancellation-error.ttl
**test name**: validProposalNoAcceptanceNoAgreementOneCancellationError

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
**output**: NOTHING