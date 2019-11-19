# Delivery Chains

The messages involved in the delivery and acknowledgment of a message logically form a chain. (If you are strict about it, the structure is a DAG.)
This structure is called a *delivery chain*.

## Structure

These structures are different depending on the type of message.
* *atom-specific* messages are only exchanged between an atom's owner and the WoN node (CREATE, DELETE, REPLACE, DEACTIVATE, ACTIVATE). They are stored in the atom's message container.
* *connection-specific* messages are exchanged between two atoms. Each atom maintains a *connection* entity for this communication channel. The messages are stored in each connection's message container.

In the following depictions, the arrows are labeled with their RDF property names.

In the case of an *atom-specific* message, it consists of the message `m` and the WoN node's success response `s`:
```
m <--msg:respondingTo-- s
```

In the case of a *connection-specific* message, it consists of the message `m` and the WoN node's success response `s1` as well as 
the recipient node's success response `s2`:
```
m <--msg:respondingTo-- s1
m <--msg:respondingTo-- s2
s1 <--msg:previousMessage-- s2
```

## Message Delivery
The following depictions show which messages are exchanged. `a => b : [structure]` means `a` sends `[structure]` to `b`, realized as one dataset. Messages that are set in parentheses, such as `(m)` indicate that they are referenced but not contained in the dataset.

Atom-specific delivery: Simple in/out behavior:
```
owner -> node : m
owner <- node : m <--msg:respondingTo-- s
```
Connection-specific delivery: Message `m` travels from `owner1` to `node1`, to `node2`, to `owner2`. The responses `s1` and `s2` are delivered to both `owner1` and `owner2`.

```
owner => node1 : m 
owner <= node2 : m <--msg:respondingTo-- s1
node1 => node2 : m <--msg:respondingTo-- s1
node1 <= node2 :  (m) <--msg:respondingTo-- s2
                 (s1) <--msg:previousMessage-- s2
node2 => owner2:  m <--msg:respondingTo-- s2
                 s1 <--msg:previousMessage-- s2                
```                 
After these exchanges, both owners end up with `m`, `s1`, and `s2`.
