# Viewing RDF

In order to see the RDF representations of atoms and messages, you can enable the **debug mode** in the WoN owner application. This will make HTML links to an RDF display appear in the web appliction. Moreover, it will allow you to create atoms which will receive contact requests from the [DebugBot](webofneeds/won-bot#debug-bot) and chat with it. 

Follow these steps:
1. Log in to your WoN owner application, or [matchat.org](https://matchat.org)
2. Open your browser's javascript console (Alt+F12 in Firefox or Chrome)
3. Type `won.debugmode=1`
4. Create an atom

Wait for being contacted by the DebugBot (if it runs in your environment) and start a conversation.
You'll notice HTML links below each message as well as below the atom descriptions. These will take you to the RDF view of that resource.
Note that messages are protected resources, so you have to use the right client certificate when requesting them. The owner application does that for you, but if you try to access a Message via a link in the RDF, you will be denied access.
