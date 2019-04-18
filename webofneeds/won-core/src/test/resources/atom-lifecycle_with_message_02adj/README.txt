This is a copy of final messaging format examples, adjusted for syntax.
See original at feat-security-trip-demo branch from won-cryptography
/src/test/resources/atom-lifecycle_with_messages_02/

Other changes from that original include:
1. base prefix (or other prefix) should map to URI part up to # or last path part,
   otherwise the graph names cannot be read. Therefore, I put the full URI as names
   of the graphs. Additionally, from what I saw from the protocols etc. implementations
   in the current code, it is always assumed there, that the base URI is the current atom URI;
2. [randomAtomID_1] (and similar) as part of the URI are replaced by randomAtomID_1,
   (i.e. square brackets removed), in order to be able to use the examples for
   testing the reading/writing of messages, so that the URIs are well formed.

############################################################################

In this example we have two WON nodes:
<http://www.example.com> (WN1) and <http://www.remote-example.com> (WN2)
The examples show only a fraction of the statements
- enough to point out the overall structure of the messages and models.
In directory "01_create_atom" an owner creates an atom (N1) at WN1.
In directory "02_connect" the owner sends a connect from N1 to another atom (N2) at WN2.
In directory "03_receive_connect" the owner of N2 sends a connect to WN1.
In directory "04_deactivate_(by_owner)" the owner of N1 sends a message to WN1 to deactivate N1.
In directory "05_deactivate_(by_won_node)" WN1 deactivates N1.