In this example we have two WON nodes:
<http://www.example.com> (WN1) and <http://www.remote-example.com> (WN2)

The examples show only a fraction of the statements
- enough to point out the overall structure of the messages and models.

 In directory "01_create_atom" an owner creates an atom (N1) at WN1.
 In directory "02_connect" the owner sends a connect from N1 to another atom (N2) at WN2.
 In directory "03_receive_connect" the owner of N2 sends a connect to WN1.
 In directory "04_deactivate_(by_owner)" the owner of N1 sends a message to WN1 to deactivate N1.
 In directory "05_deactivate_(by_won_node)" WN1 deactivates N1.