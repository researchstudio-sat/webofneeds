## WoN Linked Data resources access

See also [genral description](../../won-core/doc/access-control.md) of Linked Data access control in WoN.

### Public resources

By default Node, Atom, Connection and container resources are public in WoN - can be accessed by anyone.

### Resources with restricted access

By default Event resources can only be accessed by the party that has identity of one of the participants of this 
event: receiver atom, receiver node, sender atom or sender node. WebID-TLS is used to verify the identity. The 
node web application configuration in web.xml points to the 
[configuration](../src/main/resources/spring/node-context.xml) that specifies the WebID filter and access control rules 
to use. 

##### Debugging resources with restricted access
For development process it can be useful to be able to access Event resources from the browser, but since these 
resources have restricted access, one cannot see them without proving the rightful identity. For debugging purpose, 
a developer can use separate client node keystore for generating atoms and events, import the identity of the node 
from that keystore into his computer's certificate manager, and provide that certificate when browsing the 
resources on that node - when browser asks for an identity. Here are the steps how it can be done when using Windows 
and Chrome:

1. Before running the node application, change type of the keystore in **KeyStoreService** to be in **PKCS12**. It is 
considered less safe than the one we are using but it is supported by most key management tools.

2. After running the node application the keystore and the new node certificate will be created.
 
3. In **Windows**, click **Start**, enter **certmgr.msc** in Search field, press Enter. In Own certificates select 
**import**, 
specify keystore location of your node's client keystore (the location you have specified in the project's 
configuration, i.e. in **node.properties** file in **keystore.location**) and provide the password (specified in 
**node-properties** in **keystore.password**).

4. Open Chrome Window, browse on that node the event resources, the window should pop up asking to provide the 
certificate, select the certificate of the node, after that you should be able to view the event resources.

5. If after a while the browser 'forgets' the certificate and doesn't ask for it again, or you want to force it to 
forget it and provide a new one, close and reopen window in private mode - after closing it the browser should forget 
the certificate and ask for it again.