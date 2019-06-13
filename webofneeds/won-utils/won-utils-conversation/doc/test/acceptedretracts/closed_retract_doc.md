 This query functions like modifcation (or Modifcation.java), but it utilizes a construct query that builds
 triples instead of building named graphs with a select query
 
### correctOneRemoteRetractionTest
**input**: correct-remote-retract.trig
**output**: correct-remote-retract.ttl
**test name**: correctOneRemoteRetractionTest
*// This is the case where there is one valid retraction*

**input**:

```
<https://localhost:8443/won/resource/event/4055709708568209400#content> {
    event:4055709708568209400
            con:text  "two" ;
            mod:retracts event:6149800720990867000 .
}

```

 
 **output**: 
 
 ```
 event:4055709708568209400  mod:retracts event:6149800720990867000 .
 ```
 
### noModificationTest
**input**: correct-no-retraction.trig
**output**: correct-no-retraction.ttl
**test name**: oneValidRetract
*// This is the case where there is no mod:retract triple*

**input**: Nothing except conversation without mod:retracts

**output**:  NOTHING

### correctOneLocalRetractionOfDirectlyPreviousMessageTest
**input**: correct-local-retract-two-previous.trig
**expected**: correct-local-retract-two-previous.ttl
**test name**: correctOneLocalRetractionOfDirectlyPreviousMessageTest

**input**:

```

<https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
     event:cbcccoqqqbec6bxkl3y3
            con:text  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." .
 }

<https://localhost:8443/won/resource/event/m8b6jvgclclzy48p7wqd#content-9icc> {
     event:m8b6jvgclclzy48p7wqd
            con:text  "    'hint':        create a new atom and send hint to it" ;
            mod:retracts event:cbcccoqqqbec6bxkl3y3 .  
```

**output**: 

```
event:m8b6jvgclclzy48p7wqd  mod:retracts event:cbcccoqqqbec6bxkl3y3 .
```

### correctOneLocalRetractionOfLastButOneMessageTest
**input**: correct-local-retract-directly-previous.trig
**expected**:  correct-local-retract-directly-previous.ttl
***test name***: correctOneLocalRetractionOfLastButOneMessageTest

**input**: 

```
 <https://localhost:8443/won/resource/event/8h7v5ml1aflqmoyem61a#content-7rw4> {
     event:8h7v5ml1aflqmoyem61a
            con:text  "Usage:" .
 }

 <https://localhost:8443/won/resource/event/m8b6jvgclclzy48p7wqd#content-9icc> {
     event:m8b6jvgclclzy48p7wqd
             con:text  "    'hint':        create a new atom and send hint to it" ;
           mod:retracts event:8h7v5ml1aflqmoyem61a .
           
 }
```

**output**: 

```
event:m8b6jvgclclzy48p7wqd   mod:retracts event:8h7v5ml1aflqmoyem61a .
```

### correctRetractRetractOfLastButOneMessageTest
**input**: correct-retractRetract-two-previous.trig
**expected**:  correct-retractRetract-two-previous.ttl
***test name***: correctRetractRetractOfLastButOneMessageTest

**input**:

```
 <https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
    event:cbcccoqqqbec6bxkl3y3
             con:text  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." .
 }

 <https://localhost:8443/won/resource/event/orj8iruy8pcer6zzxlra#content-wi31> {
     event:orj8iruy8pcer6zzxlra
             con:text  "    'close':       close the current connection" ;
             mod:retracts event:m8b6jvgclclzy48p7wqd .
 }

 <https://localhost:8443/won/resource/event/m8b6jvgclclzy48p7wqd#content-9icc> {
     event:m8b6jvgclclzy48p7wqd
             con:text  "    'hint':        create a new atom and send hint to it" ;
           mod:retracts event:cbcccoqqqbec6bxkl3y3 .
 }   
```

**output**: 

```
event:orj8iruy8pcer6zzxlra mod:retracts event:m8b6jvgclclzy48p7wqd .
event:m8b6jvgclclzy48p7wqd mod:retracts event:cbcccoqqqbec6bxkl3y3 .
```

### wrongLocalCopyRemoteRetractionTest
**input**: wrong-local-copyOfRemote-retract-local.trig
**expected**: wrong-local-copyOfRemote-retract-local.ttl
**test name**: wrongLocalCopyRemoteRetractionTest

**input**:

`` # extra message content used to try to break the retraction code ``

```
<https://localhost:8443/won/resource/event/5s66o8cqv4rxv74xfepg#added-content> {
  event:5s66o8cqv4rxv74xfepg mod:retracts event:gv6zk2yqk6o8bl574n36 .
}

<https://localhost:8443/won/resource/event/gv6zk2yqk6o8bl574n36#content-paqe> {
    event:gv6zk2yqk6o8bl574n36
            con:text  "Greetings! \nI am the DebugBot. I can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, type \n\n'usage'\n\n (without the quotes)." .
}

<https://localhost:8443/won/resource/event/4846251213444807000#envelope-tsng> {
    event:4846251213444807000
            a                         msg:FromOwner ;
            msg:correspondingRemoteMessage
                    event:5s66o8cqv4rxv74xfepg ;
```

**output**:  NOTHING


### wrongLocalRetractRemoteRetractionTest
**input**: wrong-local-retract-remote.trig
**expected**: wrong-local-retract-remote.ttl
**test name**: wrongLocalRetractRemoteRetractionTest 

**input**:  

```
<https://localhost:8443/won/resource/event/8h7v5ml1aflqmoyem61a#content-7rw4> {
    event:8h7v5ml1aflqmoyem61a
            con:text  "Usage:" ;
            mod:retracts event:6149800720990867000 .
}

<https://localhost:8443/won/resource/event/yrizizmtaxehctdi1m1n#envelope-8gf6> {
    event:yrizizmtaxehctdi1m1n
            a                     msg:FromExternal ;
            msg:correspondingRemoteMessage
                    event:8h7v5ml1aflqmoyem61a ;
            msg:sentTimestamp  1513170817983 ;
            msg:protocolVersion   "1.0" .

<https://localhost:8443/won/resource/event/6149800720990867000#content> {
    event:6149800720990867000
            won:socket        won:OwnerSocket ;
            won:targetSocket  won:OwnerSocket ;
            con:text  "Hello, debugbot!" .
}
```

**output**:  NOTHING

### wrongLocalRetractSubsequentRetractionTest
**input**: wrong-local-retract-subsequent.trig
**expected**: wrong-local-retract-subsequent.ttl
**test name**: wrongLocalRetractSubsequentRetractionTest

**input**:

```
<https://localhost:8443/won/resource/event/8h7v5ml1aflqmoyem61a#content-7rw4> {
    event:8h7v5ml1aflqmoyem61a
            con:text  "Usage:" ;
            mod:retracts event:m8b6jvgclclzy48p7wqd .
}

<https://localhost:8443/won/resource/event/m8b6jvgclclzy48p7wqd#content-9icc> {
    event:m8b6jvgclclzy48p7wqd
            con:text  "    'hint':        create a new atom and send hint to it" .
}
```

**output**:  NOTHING

### wrongLocalSelfRetractionTest
**input**: wrong-local-selfretract.trig
**expected**: wrong-local-selfretract.ttl
**test name**: wrongLocalSelfRetractionTest

**input**:

```
<https://localhost:8443/won/resource/event/8h7v5ml1aflqmoyem61a#content-7rw4> {
    event:8h7v5ml1aflqmoyem61a
            con:text  "Usage:" ;
            mod:retracts event:8h7v5ml1aflqmoyem61a .
}
```

**output**:  NOTHING

### wrongRemoteRetractLocalRetractionTest
**input**: wrong-remote-retract-local.trig
**expected**: wrong-remote-retract-local.ttl
**test name**: wrongRemoteRetractLocalRetractionTest 

**input**:

```
<https://localhost:8443/won/resource/event/4846251213444807000#content> {
    event:4846251213444807000
            con:text  "one" ;
            mod:retracts event:8h7v5ml1aflqmoyem61a .
}

<https://localhost:8443/won/resource/event/8h7v5ml1aflqmoyem61a#content-7rw4> {
    event:8h7v5ml1aflqmoyem61a
            con:text  "Usage:" .
}
```

**output**:  NOTHING

### wrongRemoteRetractSubsequentRetractionTest
**input**: wrong-remote-retract-subsequent.trig
**expected**: wrong-remote-retract-subsequent.ttl
**test name**: d13WrongRemoteRetractSubsequentRetractionTest

**input**:

```
<https://localhost:8443/won/resource/event/4846251213444807000#content> {
    event:4846251213444807000
            con:text  "one" ;
            mod:retracts event:4055709708568209400 .
}

<https://localhost:8443/won/resource/event/4055709708568209400#content> {
    event:4055709708568209400
            con:text  "two" .
}
```

**output**:  NOTHING

### wrongRemoteSelfRetractRetractionTest
**input**: wrong-remote-selfretract.trig
**expected**: wrong-remote-selfretract.ttl
**test name**: wrongRemoteSelfRetractRetractionTest

**input**:

```
<https://localhost:8443/won/resource/event/4055709708568209400#content> {
    event:4055709708568209400
            con:text  "two" ;
            mod:retracts event:4055709708568209400 .
}
```

**output**:  NOTHING

