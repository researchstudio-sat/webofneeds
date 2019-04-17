## Goals

Atoms can declare goals that they want to achieve in collaboration with a counterpart atom. For example, one atom offers a taxi ride and the other atoms is looking for a ride. This situation may be described as two complementary goals by the two atoms which can result in a collaboration to fulfill actually a common goal of both - perform the ride togehter as driver and client. 

As described before atoms are matched based on the data in their `won:is` and `won:seeks` branches. A third type of top level branch is `won:goal`. A goal consists of data graph as input suggestion for the goal and a SHACL graph that defines how the data should look like after the goal is reached. Atoms try to fulfill their goals in a conversation with a counterpart atom after the matching happened and a connection is opened. Atoms would look on the counterpart for fitting complementary goals where the data graph would fulfill the SHACL shapes graph of its own goal(s) and where they can fulfill the SHACL shapes graph on the counterpart with their own data graph in reverse. To fullfill two goals on both both sides one atom would propose a data graph that satisfies both SHACL shape graph constraints. This data graph would usually be a combination of data graphs of goals of both sides. The other side could then accept the proposed data graph if it satisfies one of its goals and thereby form an agreement with its counterpart atom.  

This structure of goals can be used to agree on actions like service/API calls executed by bots that mange atoms. For instance, a bot could create an atom that describes the input data for a certain API call (e.g. organize a ride and a call taxi) in its goals SHACL shape graph (e.g. specifying that there must be at least be a pickup location and optionally a time provided). 

### Declaring Goals

Atoms can declare zero or more goals in top level branches using the property `won:goal`. Each goal has one property `won:shapesGraph` attached to it that defines the expected outcome data of the goal. Optionally each goal has another property `won:dataGraph` attached to it that defines the input data to support the satisfaction of the goal. 

The following example defines an atom that offers "Taxi in Vienna" with a goal declaration to find out the pickup location of potential clients. In the `:atomGraph` graph the atom is defined having a `won:is` top level branch that describes it for the matching. Also the atom defines one goal using another top level branch element `won:goal`. 

The data graph `:service-pickup-data-graph` describes a node that should be of type `taxi:Ride` and sets the driver role to its own atom uri as well as a default pickup time to 10 minutes from now. If requested the pickup time data can be overwritten by the client, but it describes the default case where a client usually orders a taxi and wants it immediately if no time is specified explicitly. 

The shapes graph `:service-pickup-shapes-graph` defines that there must be exactly one node of class `taxi:Ride`. The ride specifies exactly one driver which must be `atom:taxiOfferAtom` as well as exacly one client which must be the counterpart atom uri `atom:taxiDemandAtom`. Also the ride must have exactly one `taxi:hasPickUpLocation` property which describes the pickup either as location (e.g. geo coordinates) or address (e.g. name and number of street). Also there must be exactly one property `taxi:hasPickUpDateTime` attached to the `taxi:Ride` node that describes the pickup date and time. A taxi bot could use the default time specified in its own data graph (now + 10 min which has to be updated regularly) or the time from the client atom data graph if provided instead. 

Furthermore the shapes graph defines that the `taxi:Ride` is `sh:closed true`. That means the shapes graph will only validate successfully if the proposed data graph has excatly the from described above with no additional properties. This way the taxi service bot can make sure that the agreement doesn't contain unkown triples before accepting it and that the call to the taxi service API can be made with all necessary parameters. 

````
GRAPH :atomGraph {
  atom:taxiOfferAtom
  a won:Atom;
  won:is [
    dc:title "Taxi in Vienna";
    dc:description "Offering taxi services in Vienna and around";
    won:location  [
      a  s:Place ;
      s:geo [
        a s:GeoCoordinates ;
        s:latitude   "48.209269" ;
        s:longitude  "16.370831"
      ] ;
      s:name        "Vienna, Austria"
    ] ;
        
  won:goal :myGoal ;
  :myGoal won:dataGraph :service-pickup-data-graph .
  :myGoal won:shapesGraph :service-pickup-shapes-graph .
  ] .
}
  
GRAPH :service-pickup-data-graph {
  :myRide1 a taxi:Ride .
  :myRide1 taxi:hasDriver atom:taxiOfferAtom .
  :myRide1 taxi:hasPickupDateTime "2017-11-22T09:30:00Z"^^xsd:dateTime    # now + 10 min 
}

GRAPH :service-pickup-shapes-graph {
  :pickup-shape
  a sh:NodeShape ;
  sh:label "Required pickup information" ;
  sh:message "The required pickup information could not be found" ;
  sh:targetClass taxi:Ride ; 
  sh:severity sh:Violation ;
  sh:closed true ;
  sh:property [
    sh:path ( taxi:hasPickUpLocation ) ;
    sh:maxCount 1 ;
    sh:minCount 1 ;
    sh:or (
      [ sh:node :locationShape ] # details of :locationShape not shown here
      [ sh:node :addressShape ]  # details of :addressShape not shown here
    )
  ] ;
  sh:property [
    sh:path ( taxi:hasDriver ) ;
    sh:maxCount 1 ;
    sh:minCount 1 ;
    sh:hasValue atom:taxiOfferAtom ;
  ] ;
  sh:property [
    sh:path ( taxi:hasClient ) ;
    sh:maxCount 1 ;
    sh:minCount 1 ;
    sh:hasValue atom:taxiDemandAtom ;
  ] ;
  sh:property [
    sh:path ( taxi:hasPickUpDateTime ) ;
    sh:maxCount 1 ;
    sh:minCount 1 ;
    sh:datatype xsd:dateTime ;
  ] .
}
````

If this taxi service atom is matched with a potential client atom with compatible goals they could start a collaboration.
The taxi service cannot fulfill its goal just for itself by proposing its `:service-pickup-data-graph` to a client since its shape graph `:service-pickup-shapes-graph` requires a pick up location property of type `taxi:hasPickUpLocation` as well as a client set by the property `taxi:hasClient`. These triples have to be provided by a client to satisfy the taxi services shapes graphs. 

A client atom that could have been matched to the above service could look like the following. The atom is again described in a `:atomGraph` graph but with a `won:seeks` top level branch to match the service atom. The atom has one goal with a data graph `:client-pickup-data-graph` and a shape graph `client-pickup-data-graph`. 

The data graph specifies a node of type `taxi:Ride` with client, pickup time and location properties. The pickup time is set to the next day and is meant to overwrite the default pickup time in the data graph of the taxi service. For the pickup location an address string is specified. The client is set to the own atom uri `atom:taxiDemandAtom`.

The shape graph specifies the conditions for the client to accept an agreement with a taxi service provider. As above the `sh:closed` property is used to have control of the triples in the final agreement. As above the client expects the ride to have a driver role that is played by the counterpart atom `atom:taxiOfferAtom` and a client role with its own atom uri `atom:taxiDemandAtom`. Also the client expects an `:addressShape` as pickup location with value "Thurngasse, KG Alsergrund, Alsergrund, Wien, 1090, Österreich" which matches exactly the location already specified in its data graph. That means the taxi service should just use this address without modifying anything here. Also the client specifies the constraints for the pickup time in its goals shape graph. The pickup time that the client recommends in its goals data graph must not match exactly the one that the taxi service might propose. The client grants a time window of 10 minutes by specifying `sh:minInclusive` and `sh:maxInclusive` constraints for the `taxi:hasPickUpDateTime`. 


````
GRAPH :atomGraph {
  atom:taxiDemandAtom
  a won:Atom;
  won:seeks [
    dc:title "Looking for a taxi in Vienna" ;
    won:location [
      a  s:Place ;
      s:name  "Thurngasse, KG Alsergrund, Alsergrund, Wien, 1090, Österreich"
    ] 
  ] ;
    
  won:goal :myGoal ;
  :myGoal won:dataGraph :client-pickup-data-graph .
  :myGoal won:shapesGraph :client-pickup-shapes-graph .
  ]
}
  
GRAPH :client-pickup-data-graph {
  :myRide2 a taxi:Ride .
  :myRide2 taxi:hasClient atom:taxiDemandAtom .
  :myRide2 taxi:hasPickupDateTime "2017-11-23T09:30:00Z"^^xsd:dateTime ;    # same time next day
  :myRide2 taxi:hasPickUpLocation [
    a  s:Place ;
    s:name  "Thurngasse, KG Alsergrund, Alsergrund, Wien, 1090, Österreich"
  ]
}

GRAPH :client-pickup-shapes-graph {
  :pickup-shape
  a sh:NodeShape ;
  sh:label "Required pickup information" ;
  sh:message "The required pickup information could not be found" ;
  sh:targetClass taxi:Ride ; 
  sh:severity sh:Violation ;
  sh:closed true ;
  sh:property [
    sh:path ( taxi:hasPickUpLocation ) ;
    sh:maxCount 1 ;
    sh:minCount 1 ;
    sh:node :addressShape
  ] ;
  sh:property [
    sh:path ( taxi:hasDriver ) ;
    sh:maxCount 1 ;
    sh:minCount 1 ;
    sh:hasValue atom:taxiOfferAtom ;
  ] ;
  sh:property [
    sh:path ( taxi:hasClient ) ;
    sh:maxCount 1 ;
    sh:minCount 1 ;
    sh:hasValue atom:taxiDemandAtom ;
  ] ;
  sh:property [
    sh:path ( taxi:hasPickUpDateTime ) ;
    sh:maxCount 1 ;
    sh:minCount 1 ;
    sh:datatype xsd:dateTime ;
    sh:minInclusive "2017-11-23T09:25:00Z"^^xsd:dateTime ;   
    sh:maxInclusive "2017-11-23T09:35:00Z"^^xsd:dateTime ;   
  ] .
  
  :addressShape
  a sh:NodeShape ;
  sh:targetClass s:Place ; 
  sh:closed true ;
  sh:property [
    sh:path ( s:name ) ;
    sh:maxCount 1 ;
    sh:minCount 1 ;
    sh:hasValue "Thurngasse, KG Alsergrund, Alsergrund, Wien, 1090, Österreich"
  ] .
}
````    
    
### Achieving Goals

Goals can be achieved in collaboration when two atoms have established a connection and having a conversation. Either of the two atoms can propose a solution for a goal of its counterpart (thereby usually fulfilling a common goal in case a complementary goal exists on its own side that is also fulfilled by the solution) by using the agreement protocol, described in [our DeSemWeb2017 publication](http://ceur-ws.org/Vol-1934/contribution-07.pdf). A solution for a goal is proposed by sending a message with a data graph to the conversation and afterwards another message that proposes the content of the referenced content message to the other atom. The following example of 2 messages `event:event1` and `event:event2` would be a solution for both goals of the two atoms (`taxiOfferAtom` and `taxiDemandAtom`) defined above and could have been sent by the taxi service bot to a client to agree on the specific conditions of the ride. 
    
Message with data graph:
````
event:event1 won:contentGraph :pickup-solution .

GRAPH :pickup-solution {
  :myRide3 a taxi:Ride .
  :myRide3 taxi:hasDriver atom:taxiOfferAtom .
  :myRide3 taxi:hasClient atom:taxiDemandAtom .
  :myRide3 taxi:hasPickupDateTime "2017-11-23T09:30:00Z"^^xsd:dateTime
  :myRide3 taxi:hasPickUpLocation [
    a  s:Place ;
    s:name  "Thurngasse, KG Alsergrund, Alsergrund, Wien, 1090, Österreich"
  ]
}
````

Message that proposes previous message to other atom:
````
event:event2 agr:propose event:event1
````
    
The `:pickup-solution` is meant to satisfy the shape graph of the client atom goal as well as its own goals shape graph. The proposed data graph is usually created by combining the data graphs of the goals of two atoms. It can however also be created on-the-fly without any data present in the atoms. For example by showing the user a form to enter some values, sending these values over the conversation to the other atom and then creating a data graph to propose to the user again (for an example see [information requests](draft-stating-information-requirements.md
)). 

The current solution to achieve goals extracts data not only from the goals data graphs of both atoms but also from all other content data (e.g. in the is/seeks branches) of the two atoms (except its other goals data or shapes graphs) and the whole conversation. This way so called goal instantiation data graphs can be created not only from goal data but all atom and conversation information. Missing information can be exchanged over a conversation to achieve goals. This goal instantiation data graphs can be extracted using the shacl graphs described in the goals since they specify in detail how the data that satisfies goals has to look like. 

The proposing atom has to make sure that its goals shape graph is satisfied by the proposed data graph. After a data graph has been proposed, it can be accepted (using `agr:accepts` property of the agreement protocol) by the other side. The accepting atom also has to make sure that its goals shape graph is satisfied by the proposed data graph before accepting the proposal. Once a proposal is accepted it cannot be canceled without the approval of the counterpart anymore. 

Message that accepts the proposal message:
````
event:event3 agr:accept event:event2
````

In our example the taxi service bot would combine the data graphs of both atoms goals (taxi service and client), check if the shape graphs of both atoms goals are satisfied and then propose the data to the client atom. The client atom would then check if the proposed data satisfies its own goals shape graph and then accept it. However the roles who is proposing and who is accepting could also be changed so that the client proposes data and the taxi service would accept it. Anyway the taxi service bot is able to call a taxi API and order a taxi after the proposal is accepted since its goals shape graph is satisfied which specified the input datan needed to make the API call. 

Each side can use `agr:proposeToCancel` and try to cancel an agreement and thereby try to roll back actions that may be started after the agreement was formed (e.g. canceling the taxi order by calling the driver). However as stated before the request has to be accepted by the counterpart if an agreement should be canceled. 


### Blending data graphs

As mentioned above a proposed data graph that is meant to fulfill goals is usually generated by one of the agents by combining the data graphs of one goal from each side of a connection. We call the process of combining two data graphs "blending". 

Blending is neccessary since both side describe their goals from their own perspective and usually use other node URIs to describe concepts that should be the same to talk about the same thing in a mutual agreement. For instance in the above examples the taxi service describes the ride it wants to organize as `:myRide1`, the client describe it as `:myRide2` and the proposed data graph may even use another URI `:myRide3` for the ride node. The point is to come up with a name of the ride that both participants can agree on. Since both participants have not used any restrictions on the ride node name/URI (only refering to the ride as a node of class `taxi:Ride`) in their goals shape graphs, the name of the ride can be changed to blend the data graphs together and satisfy both goals. 

Blending is described in [more detail here.](draft-graph-blending.md) 
