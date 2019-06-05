 This query functions like proposal (or ProposalFunction.java), but it utilizes a construct query that builds
 triples instead of building named graphs with a select query
 
### noOpenProposal
**input**: 2proposal-bothaccepted.trig
**output**: 2proposal-bothaccepted.ttl
**test name**: noOpenProposal
*// This is the case where there is no open proposal...(each exist in their own envelope, both are accepted in an agreement)*
 
 **output**: none
 
### oneOpenProposal
**input**: 2proposal-one-accepted.trig
**output**: 2proposal-one-accepted.ttl
**test name**:  oneOpenProposal
*// This is the case where there is one open proposal...(each exist in their own envelope, only one is accepted in an agreement)*

**input**:

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

<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:proposes event:5669098069340991000 .
}

<https://localhost:8443/won/resource/event/557600936467257340#content> {
    event:557600936467257340
            con:text  "two" ;
            agr:accepts event:usi9yhill1lo2xi70sjx .
}
```

**output**:  

 ```
 event:00d5wzbbf8hzzt2eewcc agr:proposes event:5669098069340991000 .
 ```


### twoOpenProposals
**input**: 2proposal-noaccepted.trig
**output**: 2proposal-noaccepted.ttl
**test name**: twoOpenProposals   
*// This is the case where there are two open proposals ...(each exist in their own envelope)*

**input**:

```
<https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            con:text  "one" .
}

<https://localhost:8443/won/resource/event/6671551888677331000#content> {
    event:6671551888677331000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "hi" .
}


<https://localhost:8443/won/resource/event/00d5wzbbf8hzzt2eewcc#content-tbn3> {
    event:00d5wzbbf8hzzt2eewcc
            con:text  "I'm not sure I understand you fully." ;
            agr:proposes event:5669098069340991000 .
}

<https://localhost:8443/won/resource/event/usi9yhill1lo2xi70sjx#content-klsc> {
    event:usi9yhill1lo2xi70sjx
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." ;
            agr:proposes event:6671551888677331000 .
}
```

**output**: 

```
 event:00d5wzbbf8hzzt2eewcc agr:proposes event:5669098069340991000 .
 event:usi9yhill1lo2xi70sjx agr:proposes event:6671551888677331000 .
```