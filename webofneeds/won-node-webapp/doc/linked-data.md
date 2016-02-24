# Paging of a linked data resource

## Supported resources
Paging is supported only for following container resources:

  needs container
  **Example**: https://localhost:8443/won/resource/need/

  connections container
  **Example**: https://localhost:8443/won/resource/connection/

  connections of a need container
  **Example**: https://localhost:8443/won/resource/need/6666347806036328000/connections/

  events of a connection container.
  **Example**: https://localhost:8443/won/resource/connection/a4qyk5jl1twz34b4umjt/events


## How client can trigger the paging of the WoN container resource
For the supported resources the GET request containing the following headers triggers paging:

  Accept: \[application/trig|application/ld+json|application/n-quads\]
  Prefer: return=representation; max-member-count="\[preferred number of elements URIs per page\]"

  **Example**:
  Accept: application/trig
  Prefer: return=representation; max-member-count="10"

Optional query parameters define which page is returned. The following query parameters are supported:

TODO supported parameters

If no page related query parameter is provided, but client signals that it supports paging (by using the above Prefer
header in hist requests), the node returns the 1st page of the paged resource based on the following sorting logic:
the latest created needs for need container, the latest messages for events container, and
connections where the latest activity was recorded (based on their events).


### Paging of connections container explained

When paging for connections container is used, connections are sorted based on their latest events. Because each
connection can have many events and can receive them any time, the result of connection ordering can be completely
different at each different point of time. This is not like in case of needs and events ordering based on their
direct property that never change - their creation date.

Therefore, the additional parameter that defines the point of time for ordering for connections can be specified. The
parameter is a query parameter with the name 'timeof' and the value is time in format yyyy-MM-dd'T'HH:mm:ss.SSS. Any
events that where created after this time are not taken into account for ordering.

TODO examples


