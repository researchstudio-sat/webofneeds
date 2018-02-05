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
            won:hasTextMessage  "two" ;
            agr:proposes event:5669098069340991000 .
}

<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            won:hasTextMessage  "Please go on." ;
            agr:accepts event:557600936467257340 .
}

<https://localhost:8443/won/resource/event/1435888415723958200#content> {
    event:1435888415723958200
            won:hasTextMessage  "validate" ;
            agr:proposesToCancel event:ow321nn1va6clfidc4s0 .
}

<https://localhost:8443/won/resource/event/mt1f8i41bogt7kc91fi6#content-lqq1> {
    event:mt1f8i41bogt7kc91fi6
            won:hasTextMessage  "Ok, I'm going to validate the data in our connection. This may take a while." ;
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

### oneAgreementTwoCancellationSameAgreement
**input**: one-agreement-two-cancellation-same-agreement.trig
**output**: one-agreement-two-cancellation-same-agreement.ttl
**test name**: oneAgreementTwoCancellationSameAgreement
*// This is the case where there is one valid proposal*

### oneAgreementOneCancellationTestProposalError
**input**: one-agreement-one-cancellation-proposal-error.trig
**output**: one-agreement-one-cancellation-proposal-error.ttl
**test name**: oneAgreementOneCancellationTestProposalError
*// This is the case where there is one valid proposal*


### cancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes
**input**: cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig
**output**: cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.ttl
**test name**: cancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes
*// This is the case where there is one valid proposal*

### twoProposalOneAgreementOneCancellationError 
**input**: 2proposal-one-agreement-errormsg-one-cancellation.trig
**output**: 2proposal-one-agreement-errormsg-one-cancellation.ttl
**test name**: twoProposalOneAgreementOneCancellationError
*// This is the case where there is one valid proposal*

### twoProposalOneAgreementOneCancellationmsgError
**input**: 2proposal-one-agreement-one-cancellation-msgerror.trig
**output**: 2proposal-one-agreement-one-cancellation-msgerror.ttl
**test name**: twoProposalOneAgreementOneCancellationError
*// This is the case where there is one valid proposal*