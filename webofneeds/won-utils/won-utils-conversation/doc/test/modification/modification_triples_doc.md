## d13NoModificationTest
**input**: correct-no-retraction.trig
**expected**: correct-no-retraction.trig
**test name**: d13NoModificationTest

No triples were removed.

## d13CorrectOneRemoteRetractionTest
**input**: correct-remote-retract.trig
**expected**: correct-remote-retract.trig
**test name**: d13CorrectOneRemoteRetractionTest

**diff** : the following triples are removed in the **expected**:

```
< <https://localhost:8443/won/resource/event/4055709708568209400#content> {
<     event:4055709708568209400
<             con:text  "two" ;
< 			mod:retracts event:6149800720990867000 .
< }
7411,7417d7405
< }
< 
< <https://localhost:8443/won/resource/event/6149800720990867000#content> {
<     event:6149800720990867000
<             won:socket        won:OwnerSocket ;
<             won:targetSocket  won:OwnerSocket ;
<             con:text  "Hello, debugbot!" .
```

## d13CorrectOneLocalRetractionOfDirectlyPreviousMessageTest
**input**: correct-local-retract-two-previous.trig
**expected**: correct-local-retract-two-previous.trig
**test name**: d13CorrectOneLocalRetractionOfDirectlyPreviousMessageTest

**diff**: the following triples are removed in the **expected**:

```
581,585d580
< <https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
<     event:cbcccoqqqbec6bxkl3y3
<             con:text  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." .
< }
< 
4679,4685d4673
< }
< 
< <https://localhost:8443/won/resource/event/m8b6jvgclclzy48p7wqd#content-9icc> {
<     event:m8b6jvgclclzy48p7wqd
<             con:text  "    'hint':        create a new atom and send hint to it" ;
< 			mod:retracts event:cbcccoqqqbec6bxkl3y3 .
< 	
```

## d13CorrectOneLocalRetractionOfLastButOneMessageTest
**input**: correct-local-retract-directly-previous.trig
**expected**:  correct-local-retract-directly-previous.trig
***test name***: d13CorrectOneLocalRetractionOfLastButOneMessageTest

**diff**: the following triples are removed in the **expected**:

```
< <https://localhost:8443/won/resource/event/8h7v5ml1aflqmoyem61a#content-7rw4> {
<     event:8h7v5ml1aflqmoyem61a
<             con:text  "Usage:" .
< }
< 
4681,4686d4675
< <https://localhost:8443/won/resource/event/m8b6jvgclclzy48p7wqd#content-9icc> {
<     event:m8b6jvgclclzy48p7wqd
<             con:text  "    'hint':        create a new atom and send hint to it" ;
< 			mod:retracts event:8h7v5ml1aflqmoyem61a .
< 			
< }
```

## d13CorrectRetractRetractOfLastButOneMessageTest
**input**: correct-retractRetract-two-previous.trig
**expected**: correct-retractRetract-two-previous.trig
**test name**: d13CorrectRetractRetractOfLastButOneMessageTest

**diff**: the following triples are removed in the **expected**:

```
< <https://localhost:8443/won/resource/event/cbcccoqqqbec6bxkl3y3#content-3j4j> {
<     event:cbcccoqqqbec6bxkl3y3
<             con:text  "You are connected to the debug bot. You can issue commands that will cause interactions with your atom." .
< }
< 
1201,1206d1195
< <https://localhost:8443/won/resource/event/orj8iruy8pcer6zzxlra#content-wi31> {
<     event:orj8iruy8pcer6zzxlra
<             con:text  "    'close':       close the current connection" ;
<             mod:retracts event:m8b6jvgclclzy48p7wqd .
< }
< 
4680,4686d4668
< }
< 
< <https://localhost:8443/won/resource/event/m8b6jvgclclzy48p7wqd#content-9icc> {
<     event:m8b6jvgclclzy48p7wqd
<             con:text  "    'hint':        create a new atom and send hint to it" ;
< 			mod:retracts event:cbcccoqqqbec6bxkl3y3 .
< 	
```

## d13WrongLocalCopyRemoteRetractionTest
**input**: wrong-local-copyOfRemote-retract-local.trig
**expected**: wrong-local-copyOfRemote-retract-local.trig
**test name**: d13WrongLocalCopyRemoteRetractionTest 

Comments:

```
# This example contains no valid retractions. 
# There is one invalid retraction: the correspondingRemoteMessage of event:4846251213444807000 has an 
# additional content graph that contains the retraction.
# It should not be possible to create this example with correctly operating WoN nodes
# For checking the validity of the retraction, checking the happen-before relationship is not sufficient
```

Retract triples:

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

------------------
## d13WrongLocalRetractRemoteRetractionTest
**input**:  wrong-local-retract-remote.trig
**expected**:  wrong-local-retract-remote.trig
**test name**: d13WrongLocalRetractRemoteRetractionTest

Retract triples:

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

----------------------
## d13WrongLocalRetractSubsequentRetractionTest
**input**: wrong-local-retract-subsequent.trig
**expected**: wrong-local-retract-subsequent.trig
**test name**: d13WrongLocalRetractSubsequentRetractionTest

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

--------------
## d13WrongLocalSelfRetractionTest
**input**: wrong-local-selfretract.trig
**expected**: wrong-local-selfretract.trig
**test name**: d13WrongLocalSelfRetractionTest

Retract triples:

```
<https://localhost:8443/won/resource/event/8h7v5ml1aflqmoyem61a#content-7rw4> {
    event:8h7v5ml1aflqmoyem61a
            con:text  "Usage:" ;
			mod:retracts event:8h7v5ml1aflqmoyem61a .
}
```

----------------------
## d13WrongRemoteRetractLocalRetractionTest
**input**: wrong-remote-retract-local.trig
**expected**: wrong-remote-retract-local.trig
**test name**: d13WrongRemoteRetractLocalRetractionTest 

Retract triples:

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

------------------------
## d13WrongRemoteRetractSubsequentRetractionTest
**input**: wrong-remote-retract-subsequent.trig
**expected**: wrong-remote-retract-subsequent.trig
**test name**: d13WrongRemoteRetractSubsequentRetractionTest

Retract triples:

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

--------------------------
## d13WrongRemoteSelfRetractRetractionTest
**input**: wrong-remote-selfretract.trig
**expected**: wrong-remote-selfretract.trig
**test name**: d13WrongRemoteSelfRetractRetractionTest

*This test tries a retracting triple that retracts its own content graph. This should not be possible*

Retract triples:

```
<https://localhost:8443/won/resource/event/4055709708568209400#content> {
    event:4055709708568209400
            con:text  "two" ;
			mod:retracts event:4055709708568209400 .
}
```

------------------









