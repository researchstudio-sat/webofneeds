## WoN resource types and meanings
HTTP GET method is supported for all WoN data via the following resources:

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

  **connections container of a need** contains a list of all connections URIs of the particular need
  
    Example: https://localhost:8443/won/resource/need/6666347806036328000/connections

  **event** represents a communication event (message) between WoN resources that have identities 
  
    Example: https://localhost:8443/won/resource/event/hbaz7dxrlq2lmol2yzlt
    
  **event container of a need** holds all events that pertain to a need (and not one of its connections) - these events are CREATE, ACTIVATE, DEACTIVATE, and their respective responses from the node.
  
    Example: https://localhost:8443/won/page/need/8929366644928455000#events 
    
  **event container of a connection** represents all communication events that are part of a particular connection
  
    Example: https://localhost:8443/won/resource/connection/a4qyk5jl1twz34b4umjt/events/
    
  **need deep** represents an aggregated resource consisting of *need* resource, its *connections of a need container* resource, its *connection* resources, each of its connection *events of a connection container* resource, and all its connections *event* resources. The number of connections and events can be limited by an optional layer-size parameter (the latest of those are displayed then).
  
    Example: https://localhost:8443/won/resource/need/6666347806036328000/deep
             https://localhost:8443/won/resource/need/6666347806036328000/deep?layer-size=5
