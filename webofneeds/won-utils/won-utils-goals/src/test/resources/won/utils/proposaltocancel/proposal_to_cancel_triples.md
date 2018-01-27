### This returns all open proposalsToCancel

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

 ### name to be determined
 **input**: twoproposetocancel-one-unaccepted.trig
 **output**: 
 **test name**: name to be determined
 
 **input**:
 
 ```
 <https://localhost:8443/won/resource/event/6149800720990867000#content> {
    event:6149800720990867000
            won:hasFacet        won:OwnerFacet ;
            won:hasRemoteFacet  won:OwnerFacet ;
            won:hasTextMessage  "Hello, debugbot!" .
}


<https://localhost:8443/won/resource/event/gv6zk2yqk6o8bl574n36#content-paqe> {
    event:gv6zk2yqk6o8bl574n36
            won:hasTextMessage  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}

<https://localhost:8443/won/resource/event/1107469913331435500#content> {
    event:1107469913331435500
            won:hasTextMessage  "usage" ;
            agr:proposes event:6149800720990867000 .
}

<https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
    event:cbcccoqqqbec6bxkl3y3
            won:hasTextMessage  "You are connected to the debug bot. You can issue commands that will cause interactions with your need." ;
            agr:proposes event:gv6zk2yqk6o8bl574n36 .
}


<https://localhost:8443/won/resource/event/4846251213444807000#content> {
    event:4846251213444807000
            won:hasTextMessage  "one" ;
            agr:accepts event:cbcccoqqqbec6bxkl3y3 .
}

<https://localhost:8443/won/resource/event/152dum7y56zn95qyernf#content-19tl> {
    event:152dum7y56zn95qyernf
            won:hasTextMessage  "I'm not sure I understand you fully." ;
            agr:accepts event:1107469913331435500 .
}

<https://localhost:8443/won/resource/event/4055709708568209400#content> {
    event:4055709708568209400
            won:hasTextMessage  "two" .
}

<https://localhost:8443/won/resource/event/cgqt5h004iql2003me2n#content-lqq7> {
    event:cgqt5h004iql2003me2n
            won:hasTextMessage  "Please go on." ;
            agr:proposesToCancel event:152dum7y56zn95qyernf ;
            agr:proposesToCancel event:4055709708568209400 .
}

<https://localhost:8443/won/resource/event/8863100035920837000#content> {
    event:8863100035920837000
            won:hasTextMessage  "validate" ;
            agr:accepts event:cgqt5h004iql2003me2n .
}
```

**output**:

```
<https://localhost:8443/won/resource/event/cgqt5h004iql2003me2n> {
    event:4846251213444807000
            won:hasTextMessage  "one" ;
            agr:accepts event:cbcccoqqqbec6bxkl3y3 .
}
```