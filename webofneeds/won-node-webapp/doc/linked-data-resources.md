## WoN resource types and meanings

  **node** represents information of node as publishing service, - protocols, endpoints, web identity.
    
    Example: https://localhost:8443/won/resource/

  **need** represents a user intent published on the node, has web identity.
    
    Example: https://localhost:8443/won/resource/need/6666347806036328000
    
  **needs container** contains a list of need URIs published on the node
    
    Example: https://localhost:8443/won/resource/need/

  **connection** represents a relation between two communicating needs
    
    Example: https://localhost:8443/won/resource/connection/a4qyk5jl1twz34b4umjt
    
  **connections container** contains a list of all connection URIs on the node
    
    Example: https://localhost:8443/won/resource/connection/

  **connections of a need container** contains a list of all connections URIs of the particular need
  
    Example: https://localhost:8443/won/resource/need/6666347806036328000/connections/

  **event** represents a communication event (message) between WoN resources that have identities 
  
    Example: https://localhost:8443/won/resource/events/hbaz7dxrlq2lmol2yzlt
    
  **events of a connection container** represents all communication events that are part of a particular connection
  
    Example: https://localhost:8443/won/resource/connection/a4qyk5jl1twz34b4umjt/events/