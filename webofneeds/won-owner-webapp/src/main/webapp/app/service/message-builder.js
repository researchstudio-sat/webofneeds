/**
 * Created by ksinger on 29.06.2015.
 */


//TODO switch to requirejs for dependency mngmt (so this lib isn't angular-bound)
//TODO replace calls to `won` object to `require('util')`
angular.module('won.owner').factory('MessageBuilder', function () {//$q,$log, $rootScope) {



    var MessageBuilder = function(receiverNode, msgType){
        this.receiverNode = receiverNode;
        this.msgType = msgType;
    }
    MessageBuilder.prototype = {
        constructor: MessageBuilder,
        /**
         * @param contentBuilder can construct the graph to be contained in this message.
         * All ContentBuilders (like e.g. a NeedBuilder) should specify the following
         * functions to be usable with MessageBuilder
         *
         *    ```
         *    build: function(publishedContentUri, msgContentURI) => {...}
         *    getTypesForContext: function() => {...}
         *    ```
         *
         * @param publishedContentUri (e.g. 'http://localhost:8080/won/resource/need/2440117048691087400')
         * @param msgContentUri (e.g. 'http://localhost:8080/won/resource/event/1997814854983652400#content-need')
         * @param attachmentUris
         * @returns {{@graph: *[], @context}}
         */
        build: function (contentBuilder, publishedContentUri, msgUri, attachmentUris){
            var msgContentUri = msgUri + '#content-need';
            var msgDataUri = msgUri + '#data';
            return {
                '@graph': [

                    {
                        '@id': msgContentUri,
                        '@graph': contentBuilder.build(publishedContentUri, msgContentUri)
                    },
                    /*
                    {
                        //attachments go here
                    }
                     */
                    {
                        //TODO the graph below contains dummy data
                        '@id': msgDataUri,
                        '@graph': [
                            {
                                '@id': msgUri,
                                'msg:hasMessageType': {
                                    '@id': this.msgType
                                },
                                'msg:hasContent': [
                                    {
                                        '@id': msgContentUri
                                    }
                                ],
                                'msg:hasReceiverNode': {
                                    '@id': this.receiverNode
                                },
                                'msg:hasSenderNeed': {
                                    '@id': publishedContentUri
                                }
                            },
                            {
                                '@id': msgDataUri,
                                '@type': 'msg:EnvelopeGraph',
                                'rdfg:subGraphOf': {
                                    '@id': msgUri
                                }
                            }
                        ]
                    }
                 ],

                '@context': won.merge(
                    won.defaultContext,
                    contentBuilder.getTypesForContext(),
                    this.getTypesForContext()
                )
            }
        },
        /*
         TODO missing parameters for equivalence with old builder:

         see won-service:548f

         forEnvelopeData(?)

         hasSender
         hasSenderNode
         hasSenderNeed (used)

         hasReceiver
         hasReceiverNeed
         hasReceiverNode (used)

         hasFacet (used, in needbuilder)
         hasRemoteFacet

         hasTextMessage
         addContentGraphData(?) //who thought this was a good idea?
         */

        getTypesForContext: function(){
            var o = {};
            o[won.WONMSG.EnvelopeGraphCompacted] = {
                '@id': 'http://purl.org/webofneeds/message#EnvelopeGraph',
                '@type': '@id'
            };
            return o;
        }
    }

    return MessageBuilder;
});
