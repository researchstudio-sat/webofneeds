### This query functions like modifcation (or Modifcation.java), but it utilizes a construct query that builds
### triples instead of building named graphs with a select query
### oneValidRetract
**input**: correct-remote-retract.trig
**output**: correct-remote-retract.ttl
**test name**: oneValidRetract
*// This is the case where there is one valid retraction*

**input**:

```
<https://localhost:8443/won/resource/event/4055709708568209400#content> {
    event:4055709708568209400
            won:hasTextMessage  "two" ;
            mod:retracts event:6149800720990867000 .
}

```

 
 **output**: 
 
 ```
 event:4055709708568209400  mod:retracts event:6149800720990867000 .
 ```
 
### TODO ... add more tests from modification protocol