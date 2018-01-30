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
            won:hasTextMessage  "two" ;
            agr:proposes event:5669098069340991000 .
}

<https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0#content-1o90> {
    event:ow321nn1va6clfidc4s0
            won:hasTextMessage  "Please go on." ;
            agr:accepts event:557600936467257340 .
}


```

 
 **output**: 
 
 ```
event:557600936467257340 agr:proposes event:5669098069340991000  .
 ```
 
### TODO ... add more tests from agreement protocol