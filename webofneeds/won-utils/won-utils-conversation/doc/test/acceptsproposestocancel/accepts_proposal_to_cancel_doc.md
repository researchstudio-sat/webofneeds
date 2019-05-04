### This query functions like the agreement function (or AgreementFunction.java), but it utilizes a construct query that builds
### triples instead of building named graphs with a select query
### oneValidAcceptedProposalToCancel 
**input**: one-agreement-one-cancellation.trig
**output**: one-agreement-one-cancellation.ttl
**test name**: oneValidAcceptedProposalToCancel
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
event:mt1f8i41bogt7kc91fi6 agr:accepts event:1435888415723958200 .  
 ```
 
### TODO ... add more tests from agreement protocol