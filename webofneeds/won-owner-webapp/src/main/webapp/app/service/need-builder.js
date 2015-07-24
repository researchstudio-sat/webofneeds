/**
 * builds the 0th node, the content-node, in the create message graph
 *
 * Created by ksinger on 29.06.2015.
 */
//TODO switch to requirejs for dependency mngmt (so this lib isn't angular-bound)
//TODO replace calls to `won` object to `require('util')`
angular.module('won.owner').factory('NeedBuilder', function (){//$q,$log, $rootScope) {

    /**
     * Usage:
     * 1. Construct via `var builder = new NeedBuilder(type, title, description)`
     * 2. Set optional parameters:
     *    ```
     *    //comma-separated
     *    builder.tags = 'Couch, furniture';
     *
     *    // this is the default
     *    builder.facet = 'won:OwnerFacet';
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
     *    builder.latitude = 12.345678;
     *    builder.longitude = 12.345678;
     *
     *    //no format assumed. this will just be attached as a string
     *    builder.address = 'Yellowbrick Rd. 7, 12345 Oz';
     *
     *    // the URIs where attachments can be found / will be published
     *    builder.attachmentUris = ['http://example.org/.../1234.png', 'http://example.org/.../1234.pdf']
     *
     *    builder.recurInfinite =
     *    builder.recursIn =
     *    builder.startTime =
     *    builder.endTime =
     *
     *    this.currency =
     *    this.lowerLimit =
     *    this.upperLimit =
     *    ```
     *
     *
     *    NeedBuilder is a ContentBuilder.

     //TODO mandatory but specify later:
     contentId, e.g. 'http://localhost:8080/won/resource/event/1997814854983652400#content-need';
     */

    /**
     * This function initialises the NeedBuilder. Note
     * thtat type, title and description are mandatory
     * properties and thus their specification is
     * already required here.
     * @param type the type of the need (e.g. 'won:Supply')
     * @param title a string with the title (e.g. 'Couch to give away')
     * @param description a longer string describing the need in detail
     * @constructor
     *
     */
    var NeedBuilder = function(type, title, description){
        this.needType = type; // e.g. 'won:Supply';
        this.title = title;
        this.description = description;

        // particular defaults:
        //   (all others are `undefined` and thus nonexistant)
        this.facet = 'won:OwnerFacet';
    };



    NeedBuilder.prototype = {
        constructor: NeedBuilder,
        /**
         * NOTE: the function below makes heavy use of the fact that `undefined`
         * properties won't be included for stringification. This allows to
         * keep it readable (the alternative is a myriad of ternary statements or
         * splitting the graph-building in as many sub-functions)
         * @param publishedContentUri The URI where the need will be published
         * @param msgContentUri The URI to reference the need-graph (the create-message's content)
         *        and distinguish it from the msg-context.
         * @returns {{@id: string, @graph: {...}*}}
         */
        build: function(publishedContentUri){
            this.needUri = publishedContentUri;

            var graph = [
                {
                    '@id': this.needUri,
                    '@type': 'won:Need',
                    'won:hasContent': '_:n01',
                    'won:hasBasicNeedType': this.needType,
                    'won:hasFacet': this.facet,
                    'won:hasAttachments': won.clone(this.attachmentUris)
                },
                {
                    '@id': '_:n01',
                    '@type': 'won:NeedContent',
                    'dc:title': this.title,
                    'won:hasTextDescription': this.description,
                    'won:hasTag': this.tags,
                    'won:hasContentDescription': (this._hasModalities() ? '_:contentDescription' : undefined)
                }
                //, <if _hasModalities> {... (see directly below) } </if>
            ];

            //check if at least one of images, location, time or price has been specified
            if(this._hasModalities()) {
                graph.push({
                    '@id': '_:contentDescription',
                    '@type': 'won:NeedModality',
                    'won:hasLocationSpecification': (!this._hasLocation()? undefined : {
                        '@id': '_:locationSpecification',
                        '@type': 'geo:Point',
                        'geo:latitude': this.latitude.toFixed(6),
                        'geo:longitude': this.longitude.toFixed(6),
                        'won:hasAddress': this.address //TODO add to onto
                    }),
                    'won:hasTimeSpecification': (!this._hasTimeConstraint()? undefined : {
                        '@id': '_:timeSpecification',
                        '@type': 'won:TimeSpecification',
                        'won:hasRecurInfiniteTimes': this.recurInfinite,
                        'won:hasRecursIn': this.recursIn,
                        'won:hasStartTime': this.startTime,
                        'won:hasEndTime': this.endTime
                    }),

                    'won:hasPriceSpecificationhas': (!this._hasPriceSpecification()? undefined : {
                        '@id': '_:priceSpecification',
                        '@type': 'won:PriceSpecification',
                        'won:hasCurrency': this.currency,
                        'won:hasLowerPriceLimit': this.lowerLimit,
                        'won:hasUpperPriceLimit': this.upperLimit
                    })
                    //TODO images, time, currency(?)
                });
            }
            return graph;
        },
        getTypesForContext: function(){
            return {
                /*
                 TODO add following datatypes to context
                 TODO only add the ones that are required?
                 */

                'won:hasContentDescription': {
                    '@id': 'http://purl.org/webofneeds/model#hasContentDescription',
                    '@type': '@id'
                },

                'won:hasCurrency': 'xsd:string',
                'won:hasLowerPriceLimit': 'xsd:float',
                'won:hasUpperPriceLimit': 'xsd:float',

                'geo:latitude': 'xsd:float',
                'geo:longitude':'xsd:float',
                'won:hasAddress': 'xsd:string',

                'won:hasStartTime': 'xsd:dateTime',
                'won:hasEndTime': 'xsd:dateTime',

                'won:hasFacet': {
                    '@id': 'http://purl.org/webofneeds/model#hasFacet',
                    '@type': '@id'
                }
            }
        },

        addAttachment: function(uri, content) {
            //TODO stopped here


        },
        _hasModalities: function(){
            return this._hasImage() || this._hasLocation() || this._hasTimeConstraint();
        },
        _hasLocation: function(){
            return (!isNaN(this.longitude) && !isNaN(this.latitude)) || this.address
        },
        _hasImage: function() {
            return false; //TODO image-attachment not implemented yet
        },
        _hasPriceSpecification: function(){
            return false; //TODO price-specification not fully implemented yet
        },
        _hasTimeConstraint: function(){
            return false; //TODO time-constraint-attachment not implemented yet
        }
    }


    return NeedBuilder;
});
