/**
 * builds the 0th node, the content-node, in the create message graph
 *
 * Created by ksinger on 29.06.2015.
 */
//TODO switch to requirejs for dependency mngmt (so this lib isn't angular-bound)
//TODO replace calls to `won` object to `require('util')`


(function(){ // <need-builder-js> scope
    if(!won) won = {};

    function hasModalities(args){
        return hasPriceSpecification(args) || hasLocation(args) || hasTimeConstraint(args) || args.bounds;
    }
    function hasLocation(args) {
        return !isNaN(args.longitude) && !isNaN(args.latitude) && args.address;
    }
    function hasPriceSpecification(args){
        return false; //TODO price-specification not fully implemented yet
    }
    function hasTimeConstraint(args){
        return false; //TODO time-constraint-attachment not implemented yet
    }
    function hasAttachmentUrls(args) {
        return args.attachmentUris && Array.isArray(args.attachmentUris) && args.attachmentUris.length > 0
    }

    /**
     * Usage:  `won.buildNeedRdf(args)`
     * Where args consists of:
     *
     * **mandatory parameters:**
     *
     * * args.type: the type of the need (e.g. 'won:Supply')
     * * args.title: a string with the title (e.g. 'Couch to give away')
     * * args.description: a longer string describing the need in detail
     *
     * **optional parameters:**
     *    ```
     *    //comma-separated
     *    args.tags = 'Couch, furniture';
     *
     *    // this is the default
     *    args.facet = 'won:OwnerFacet';
     *    // the options here are:
     *    // * won.WON.OwnerFacetCompacted
     *    // * won.WON.GroupFacetCompacted
     *    // * won.WON.CoordinatorFacetCompacted
     *    // * won.WON.ParticipantFacetCompacted
     *    // * won.WON.CommentFacetCompacted
     *    // * 'won:CommentModeratedFacet'
     *    // * 'won:CommentUnrestrictedFacet'
     *    // * 'won:ControlFacet'
     *    // * 'won:BAPCCordinatorFacet'
     *    // * 'won:BAPCParticipantFacet'
     *    // * 'won:BACCCoordinatorFacet'
     *    // * 'won:BACCParticipantFacet'
     *    // * 'won:BAAtomicPCCoordinatorFacet'
     *    // * 'won:BAAtomicCCCoordinatorFacet'
     *
     *    args.latitude = 12.345678;
     *    args.longitude = 12.345678;
     *
     *    //no format assumed. this will just be attached as a string
     *    args.address = 'Yellowbrick Rd. 7, 12345 Oz';
     *
     *    // the URIs where attachments can be found / will be published
     *    args.attachmentUris = ['http://example.org/.../1234.png', 'http://example.org/.../1234.pdf']
     *
     *    args.recurInfinite =
     *    args.recursIn =
     *    args.startTime =
     *    args.endTime =
     *
     *    args.currency =
     *    args.lowerLimit =
     *    args.upperLimit =
     *    ```
     * @returns {{@id: string, @graph: {...}*}}
     *
     * NOTE: the function below makes heavy use of the fact that `undefined`
     * properties won't be included for stringification. This allows to
     * keep it readable (the alternative is a myriad of ternary statements or
     * splitting the graph-building in as many sub-functions)
     *
     *
     * //TODO mandatory but specify later:
     * contentId, e.g. 'http://localhost:8080/won/resource/event/1997814854983652400#content-need';
     */
    won.buildNeedRdf = function(args){

        console.log('need-builder.js:buildNeedRdf:attachmentUris', args.attachmentUris);

        if(hasAttachmentUrls(args)) {
            var attachmentUrisTyped = args.attachmentUris.map(function (uri) {
                return {'@id': uri}
            });
        }


        var graph = [
            {
                '@id': args.publishedContentUri,
                '@type': 'won:Need',
                'won:hasContent': '_:n01',
                'won:hasBasicNeedType': args.type,
                'won:hasFacet': args.facet? args.facet : 'won:OwnerFacet',
                'won:hasAttachment': (hasAttachmentUrls(args) ? attachmentUrisTyped : undefined),
                'won:hasFlag': !!won.debugmode ? 'won:UsedForTesting' : undefined
            },
            {
                '@id': '_:n01',
                '@type': 'won:NeedContent',
                'dc:title': args.title,
                'won:hasTextDescription': args.description,
                'won:hasTag': args.tags,
                'won:hasContentDescription': (hasModalities(args) ? '_:contentDescription' : undefined)
            }
            //, <if _hasModalities> {... (see directly below) } </if>
        ];

        //check if at least one of images, location, time or price has been specified
        if(hasModalities(args)) {
            graph.push({
                '@id': '_:contentDescription',
                '@type': 'won:NeedModality',
                'won:hasLocation': (!hasLocation(args)? undefined : {
                    '@type': 's:Place',
                    's:geo' : {
                        '@id': '_:location',
                        '@type': 's:GeoCoordinates',
                        's:latitude': args.latitude.toFixed(6),
                        's:longitude': args.longitude.toFixed(6),
                    },
                    's:name': args.address,
                    'won:hasBoundingBox':(!args.bounds ? undefined : {
                        'won:hasNorthWestCorner': {
                            '@id': '_:boundsNW',
                            '@type': 's:GeoCoordinates',
                            's:latitude': args.bounds[0][0].toFixed(6),
                            's:longitude': args.bounds[0][1].toFixed(6),
                        },
                        'won:hasSouthEastCorner': {
                            '@id': '_:boundsSE',
                            '@type': 's:GeoCoordinates',
                            's:latitude': args.bounds[1][0].toFixed(6),
                            's:longitude': args.bounds[1][1].toFixed(6),
                        },
                    }),
                }),
                'won:hasTimeSpecification': (!hasTimeConstraint(args)? undefined : {
                    '@id': '_:timeSpecification',
                    '@type': 'won:TimeSpecification',
                    'won:hasRecurInfiniteTimes': args.recurInfinite,
                    'won:hasRecursIn': args.recursIn,
                    'won:hasStartTime': args.startTime,
                    'won:hasEndTime': args.endTime
                }),

                'won:hasPriceSpecificationhas': (!hasPriceSpecification(args)? undefined : {
                    '@id': '_:priceSpecification',
                    '@type': 'won:PriceSpecification',
                    'won:hasCurrency': args.currency,
                    'won:hasLowerPriceLimit': args.lowerPriceLimit,
                    'won:hasUpperPriceLimit': args.upperPriceLimit
                })
                //TODO images, time, currency(?)
            });
        }
        return {
            '@graph': graph,
            '@context': {
                's': 'http://schema.org/',
                /*
                 TODO add following datatypes to context
                 TODO only add the ones that are required?
                 */

                'won:hasContentDescription': {
                    '@id': won.WON.hasContentDescription, //'http://purl.org/webofneeds/model#hasContentDescription',
                    '@type': '@id'
                },
                'won:hasBasicNeedType':{
                    '@id': won.WON.hasBasicNeedType,//'http://purl.org/webofneeds/model#hasBasicNeedType',
                    '@type':'@id'
                },

                //TODO probably an alias instead of an type declaration as it's intended here
                'won:hasCurrency': 'xsd:string',
                'won:hasLowerPriceLimit': 'xsd:float',
                'won:hasUpperPriceLimit': 'xsd:float',

                //'geo:latitude': 'xsd:float',
                //'geo:longitude':'xsd:float',
                //'won:hasAddress': 'xsd:string',

                'won:hasStartTime': 'xsd:dateTime',
                'won:hasEndTime': 'xsd:dateTime',

                'won:hasFacet': {
                    '@id': won.WON.hasFacet,
                    '@type': '@id'
                },
                'won:hasFlag': {
                    '@id': won.WON.hasFlag,
                    '@type': '@id'
                }
            }
        };
    }

})() // </need-builder-js>

