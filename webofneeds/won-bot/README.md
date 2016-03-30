##Testing and debugging with bots

###Debug Bot
[DebugBotApp](src/main/java/won/bot/app/DebugBotApp.java) can be used to test if connections 
can be established with the needs you are creating and if messages can be sent via those connections. For each 
created by you need the Bot will generate a connection request and a hint messages. Additionally, some actions can be
 triggered by sending text messages on those connections. Check supported
[actions](src/main/java/won/bot/framework/events/action/impl/DebugBotIncomingMessageToEventMappingAction.java). 


Run [DebugBotApp](src/main/java/won/bot/app/DebugBotApp.java) with the argument specifying the configuration 
location, e.g:

    -DWON_CONFIG_DIR=C:/webofneeds/conf.local
    
Make sure this location contains the relevant property files, and you have specified the values of the properties 
relevant for the system being tested, i.e.:
 * in [node-uri-source.properties](../conf/node-uri-source.properties) 
    * won.node.uris - specify values of nodes being tested - the bot will react to needs published on those nodes 
 * in [owner.properties](../conf/owner.properties) 
    * specify default node data (node.default.host/scheme/port) - the bot will create its own needs on that node
    * make sure a path to keystore and truststore is specified (keystore/truststore.location) and their password 
    (keystore/truststore.password)  
    
    NOTE: Don't use at Bot the same keystore (and key pair) that you use for Owner Application, especially if you are 
    running owner locally - the node won't deliver messages correctly to the Bot and Owner because the queues used 
    for delivery are defined based on Owner and Bot certificates, and if they are the same there will be errors. 
    
    NOTE: For the same reason as above, do not run several Bot applications at the same time, - stop one before 
    running another.
    
    NOTE: Keystore and trustore pathes have to be specified but the files themselves do not have to exist, they will 
    be created automatically. In fact, in case the Bot's default Node was previously having a different certificate  
    this file must be deleted - otherwise the Bot won't trust the Node and won't be able to register and create needs
    there.
