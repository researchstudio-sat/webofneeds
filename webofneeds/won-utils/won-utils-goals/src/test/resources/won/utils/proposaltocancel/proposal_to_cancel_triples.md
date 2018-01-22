 ### oneOpenCancellationPropsoal
 **input**: one-agreement-one-unacceptedcancellation.trig
 **output**: one-agreement-one-unacceptedcancellation.trig
 **test name**: oneOpenCancellationPropsoal
 
 **input**:
 
 ```
 <https://localhost:8443/won/resource/event/5669098069340991000#content> {
    event:5669098069340991000
            won:hasTextMessage  "one" .
}

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
```

**output**:
```
<https://localhost:8443/won/resource/event/1435888415723958200> {
    <https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0>
            <http://purl.org/webofneeds/model#hasTextMessage>
                    "Please go on." .
        <https://localhost:8443/won/resource/event/ow321nn1va6clfidc4s0>
                <http://purl.org/webofneeds/agreement#accepts>
                    <https://localhost:8443/won/resource/event/557600936467257340> .
}
```