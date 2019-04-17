
# Paging of WoN linked data resources

Paging of WoN resources aims to implement paging as defined by https://www.w3.org/TR/ldp-paging/. Current version 
used is https://www.w3.org/TR/2015/NOTE-ldp-paging-20150630/.


## Supported resources
Paging is supported for the following container resources:

  **atoms container**
    
    Example: https://localhost:8443/won/resource/atom/

  **connections container**
    
    Example: https://localhost:8443/won/resource/connection/

  **connections of an atom container**
  
    Example: https://localhost:8443/won/resource/atom/6666347806036328000/connections

  **events of a connection container**
  
    Example: https://localhost:8443/won/resource/connection/a4qyk5jl1twz34b4umjt/events


## How client can trigger the paging of the WoN container resource
For the supported resources the GET request containing the following headers triggers paging:

    Accept: [application/trig|application/ld+json|application/n-quads]
    Prefer: return=representation; max-member-count="[preferred number of elements URIs per page]"


Example:

    Accept: application/trig
    Prefer: return=representation; max-member-count="10"


## Query parameters
Optional query parameters define the returned paged resource content. The following query parameters are supported:

  **paging parameters** define which page is returned:
  
    p: e.g., p=1 returned the 1st page
    resumebefore: e.g., resumebefore=abc returns a page with members that precede the member with id abc
    resumeafter: e.g., resumebefore=abc returns a page with members that follow the member with id abc
  
Only one of the paging parameter can be used per request.

  **filter parameters** define which resources are participating when selecting the page:
    
    type: e.g., type=OPEN (for events and connections container) takes into account only OPEN events
    state: e.g., state=Active (for atoms container) takes into account only atoms with Active state
    timeof: e.g., timeof=2016-02-23 (for connections container) considers only the events before that date

They are specific for each container. For events and connections container, the type parameter can be 
specified (supported types: HINT_MESSAGE, HINT_FEEDBACK_MESSAGE, CONNECT, OPEN, CONNECTION_MESSAGE, 
CLOSE). For atoms container, the atom state can be specified (Active/Inactive). For atom connections container, the 
timeof parameter can be specified, i.e. the time before which we should consider events activity can be specified in ISO 8601 format (UTC): yyyy-MM-dd'T'HH:mm:ss.SSS'Z'

  **member content parameter** defines if the content of the container member resources has to be returned:
  
    deep: e.g., deep=true in addition to members list returns a resource description for each member
    
This parameter is supported only for connections container.

**modification date parameter** specifies resources in relation to a certain modification date:
  
    modifiedafter: returns atoms or connections that have been modified after a specified date using ISO 8601 format (UTC).
    
This parameter currently does **NOT** support paging and cannot be used with the other paging parameters together (except with "deep"). 

## Some example request URIs

**atoms container**

    https://localhost:8443/won/resource/atom/
    https://localhost:8443/won/resource/atom/?p=2&state=Active
    https://localhost:8443/won/resource/atom/?resumebefore=yy7752x8bk79llazh8bj&state=Inactive

**connections container**

    https://localhost:8443/won/resource/connection
    https://localhost:8443/won/resource/connection?timeof=2016-02-23T14:45:20.273&deep=true
    https://localhost:8443/won/resource/connection?resumeafter=lr598mkvscfeh8tzkx6t&timeof=2016-02-23T14:45:20.273&deep=true

**atom connections container**

    https://localhost:8443/won/resource/atom/6666347806036328000/connections
    https://localhost:8443/won/resource/atom/6666347806036328000/connections?timeof=2016-02-23T14:45:20.273&deep=true
    https://localhost:8443/won/resource/atom/6666347806036328000/connections?resumebefore=mea3rcm1s0uj25h4r78q&timeof=2016-02-23T14:45:20.273&type=OPEN
    
**connection events container**

    https://localhost:8443/won/resource/connection/mea3rcm1s0uj25h4r78q/events/
    https://localhost:8443/won/resource/connection/mea3rcm1s0uj25h4r78q/events?type=HINT_MESSAGE
    https://localhost:8443/won/resource/connection/mea3rcm1s0uj25h4r78q/events?type=CONNECTION_MESSAGE&resumebefore=3728369276814360600


## Default paging
If no page related query parameter is provided, but client signals that it supports paging (by using the above Prefer
header in hist requests), the node returns the 1st page of the paged resource based on the sorting logic outlined below.
Therefore, for atoms container, the latest created atoms are returned. For events container, the latest created 
events are returned. For connections container, the connections are ordered by their first occured event creation date (latest created connections first).


## Sorting of members for paging
Container members are sorted in Descendant order based on their direct or linked date property. Atoms container uses 
the atom creation date as sorting criteria. Events container uses event creation date as sorting criteria. 
Connections container uses the creation date of each connection latest event as sorting criteria.


## Paging of connections container explained

When paging for connections container is used, connections are sorted based on their latest events. Because each
connection can have many events and can receive them any time, the result of connection ordering can be completely
different at each different point of time. This is quite different from atoms and events ordering that is based on their
direct property that never change - their creation date.

Therefore, the additional parameter that defines the point of time for ordering for connections can be specified. The
parameter is a query parameter with the name _timeof_ and the value is time in format _yyyy-MM-dd'T'HH:mm:ss.SSS_. Any
events that where created after this time are not taken into account for ordering.

The following connections container paging requests are supported:
  1. Display the page with the last N connections
  2. Resume displaying connection given their events state at the given time Z - display N connections before 
  (earlier in time than) the one having id X.
  3. Resume displaying connection given their events state at the given time Z - display N connections after (later 
  in time than) the one having id X. 

Optionally, one can specify the events of which type should be taken into account (by default all events are taken into 
account) when getting the latest events for each connection.


#### An example of interacting with paged connections container resource

##### _interaction_1:_

- User opens Owner Application, logs-in, selects his atom, and the user's 10 connections of that atom having latest 
Conversations going on have to be displayed:

**request:**

    GET: /won/resource/atom/A/connections?type=CONNECTION_MESSAGE&deep=true
    Accept: application/trig
    Prefer: return=representation; max-member-count="10"

**response:**

    HTTP/1.1 200 OK
    Content-Type: application/trig
    Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
    Link: <../connections?resumebefore=c41&timeof=Z&type=CONNECTION_MESSAGE&deep=true>; rel="prev"
    Link: <../connections?resumeafter=c50&timeof=Z&type=CONNECTION_MESSAGE&deep=true>; rel="next"

**response body:**
    (not shown here) contains 10 connection uris, one of them is c50 having its latest event of type 
    CONNECTION_MESSAGE happen later than all other events of that type across all the atom connections. 
    Another one is c41 that has its own latest event of type CONNECTION_MESSAGE happen earlier than the
    latest CONNECTION_MESSAGE events of the other returned connections. Additionally (because deep was set to true), 
    the content of each connection member is returned.


##### _interaction 2:_
- User scrolls down to see earlier connections (the connections with earlier Conversation activities). The _prev_ 
link from the response HEADER of _(interaction 1)_ should be used:

**request:**

    GET: /won/resource/atom/A/connections?resumebefore=c41&timeof=Z&type=CONNECTION_MESSAGE&deep=true
    Accept: application/trig
    Prefer: return=representation; max-member-count="20"

**response:**

    HTTP/1.1 200 OK
    Content-Type: application/trig
    Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
    Link: <../connections?resumebefore=c21&timeof=Z&type=CONNECTION_MESSAGE&deep=true>; rel="prev"
    Link: <../connections?resumeafter=c40&timeof=Z&type=CONNECTION_MESSAGE&deep=true>; rel="next"

**response body:**
    (not shown here) contains 20 connection uris, one of them is c40 having its latest event of type 
    CONNECTION_MESSAGE happen later than all other events of that type across the returned atom connections, but 
    before those of connection c41. Another one is c21 that has its own latest event of type CONNECTION_MESSAGE 
    happen earlier than the latest CONNECTION_MESSAGE events of the other returned connections. Additionally, only 
    events that happen before time Z are taken into account. Additionally, the content of each connection member is 
    returned.
    

**NOTE**: If there are new messages/connections that happen between user does request in (1) and scrolls in (2), they 
are not influencing the returned connections and their order. 
This is intended because the GUI doesn't want to change the order of connections during user scrolling - that would 
be very confusing. Instead, the user could see from some notifications that there are updates and can explicitly 
choose to reload the connections view. In the latter case we end up at case (1) and the process can repeat with 
different max-member-count values.

**NOTE**: The query parameter "modifiedafter" makes the requests return modified atoms or connections. However for now it doesnt support paging!


