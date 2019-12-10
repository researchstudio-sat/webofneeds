Small HowTos:

## HowTo get information out of Atom:

First you need to cast your Event to a MatcherExtensionAtomCreatedEvent.
```MatcherExtensionAtomCreatedEvent atomCreatedEvent = (MatcherExtensionAtomCreatedEvent) event; ```
from this atom you need to get the data and parse it to the DefaultModelWrapper.
```DefaultAtomModelWrapper defaultAtomModelWrapper = new DefaultAtomModelWrapper(atomCreatedEvent.getAtomData());```
From here on you have basic information about the atom. For example ```defaultAtomModelWrapper.getAllTags()``` 
To get the information from the values in seek you need to get all seek nodes. If the resource is a string it can be retireved with the getContentPropertyStringValue Method from the DefaultAtomModelWrapper. It takes the resource containing the requested value and the value you want to get back.
If it is an RDFNode you can get it with getContentPropertyObjects. 
``` MatcherExtensionAtomCreatedEvent atomCreatedEvent = (MatcherExtensionAtomCreatedEvent) event;

        DefaultAtomModelWrapper defaultAtomModelWrapper = new DefaultAtomModelWrapper(atomCreatedEvent.getAtomData());
        System.out.println(defaultAtomModelWrapper.getAllTags());
        defaultAtomModelWrapper.getSeeksNodes().forEach(node -> {
            System.out.println(defaultAtomModelWrapper.getContentPropertyStringValue(node, DC.description));
            Coordinate locationCoordinate = defaultAtomModelWrapper.getLocationCoordinate(node);
            System.out.println(locationCoordinate.getLatitude()+", "+locationCoordinate.getLongitude());
            defaultAtomModelWrapper.getContentPropertyObjects(node,SCHEMA.LOCATION);
        }); ```
