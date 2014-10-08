/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/**
 * Created by LEIH-NB on 19.08.2014.
 */

(function(){
    // determine if in-browser or using node.js

    var _browser = (typeof window !== 'undefined' || typeof self !== 'undefined');
    if(_browser) {
        if(typeof global === 'undefined') {
            if(typeof window !== 'undefined') {
                global = window;
            } else if(typeof self !== 'undefined') {
                global = self;
            } else if(typeof $ !== 'undefined') {
                global = $;
            }
        }
    }

// attaches wonmessagebuilder API to the given object
    var wrapper = function(won) {

        /**
         *  Constants
         *
         */


        won.WON = {};
        won.WON.baseUri = "http://purl.org/webofneeds/model#";
        won.WON.prefix = "won";
        won.WON.hasWonNode = won.WON.baseUri+"hasWonNode";
        won.WON.hasWonNodeCompacted = won.WON.prefix+":hasWonNode";

        won.WON.isInState = won.WON.baseUri+"isInState";
        won.WON.isInStateCompacted = won.WON.prefix+":isInState";
        won.WON.hasFacet= won.WON.baseUri+"hasFacet";
        won.WON.hasFacetCompacted= won.WON.prefix+":hasFacet";
        won.WON.hasRemoteFacet= won.WON.baseUri+"hasRemoteFacet";
        won.WON.hasRemoteFacetCompacted= won.WON.prefix+":hasRemoteFacet";

        won.WON.hasRemoteNeed= won.WON.baseUri+"hasRemoteNeed";
        won.WON.hasRemoteNeedCompacted = won.WON.prefix+":hasRemoteNeed";
        won.WON.hasRemoteConnection = won.WON.baseUri+"hasRemoteConnection";
        won.WON.hasRemoteConnectionCompacted = won.WON.prefix+":hasRemoteConnection";

        //EVENT TYPES
        won.WON.OwnerClose = won.WON.baseUri + "OwnerClose";
        won.WON.OwnerCloseCompacted = won.WON.prefix + ":OwnerClose";
        won.WON.OwnerOpen = won.WON.baseUri + "OwnerOpen";
        won.WON.OwnerOpenCompacted = won.WON.prefix + ":OwnerOpen";
        won.WON.PartnerClose = won.WON.baseUri + "PartnerClose";
        won.WON.PartnerCloseCompacted = won.WON.prefix + ":PartnerClose";
        won.WON.PartnerOpen = won.WON.baseUri + "PartnerOpen";
        won.WON.PartnerOpenCompacted = won.WON.prefix + ":PartnerOpen";
        won.WON.PartnerMessage = won.WON.baseUri + "PartnerMessage";
        won.WON.PartnerMessageCompacted = won.WON.prefix + ":PartnerMessage";
        won.WON.OwnerMessage = won.WON.baseUri + "OwnerMessage";
        won.WON.OwnerMessageCompacted = won.WON.prefix + ":OwnerMessage";
        won.WON.Hint = won.WON.baseUri + "Hint";
        won.WON.HintCompacted = won.WON.prefix + ":Hint";


        won.WON.hasGraph = won.WON.baseUri + "hasGraph";
        won.WON.hasGraphCompacted = won.WON.prefix + ":hasGraph";

        won.WON.Connection = won.WON.baseUri + "Connection";
        won.WON.ConnectionCompacted = won.WON.prefix + ":Connection";

        won.WON.Event = won.WON.baseUri + "Event";
        won.WON.EventCompacted = won.WON.prefix + ":Event";

        won.WON.Need = won.WON.baseUri + "Need";
        won.WON.NeedCompacted = won.WON.prefix + ":Need";
        won.WON.BasicNeedTypeDemand = won.WON.baseUri + "Demand";
        won.WON.BasicNeedTypeDemandCompacted = won.WON.prefix + ":Demand";
        won.WON.BasicNeedTypeSupply = won.WON.baseUri + "Supply";
        won.WON.BasicNeedTypeSupplyCompacted = won.WON.prefix + ":Supply";
        won.WON.BasicNeedTypeDotogether = won.WON.baseUri + "DoTogether";
        won.WON.BasicNeedTypeDotogetherCompacted = won.WON.prefix + ":DoTogether";
        won.WON.BasicNeedTypeCritique = won.WON.baseUri + "Critique";
        won.WON.BasicNeedTypeCritiqueCompacted = won.WON.prefix + ":Critique";
        won.WON.OwnerFacet = won.WON.baseUri +"OwnerFacet";
        won.WON.OwnerFacetCompacted = won.WON.prefix +":OwnerFacet";
        won.WON.GroupFacet = won.WON.baseUri +"GroupFacet";
        won.WON.GroupFacetCompacted = won.WON.prefix +":GroupFacet";
        won.WON.ParticipantFacet = won.WON.baseUri +"ParticipantFacet";
        won.WON.ParticipantFacetCompacted = won.WON.prefix +":ParticipantFacet";
        won.WON.CommentFacet = won.WON.baseUri +"CommentFacet";
        won.WON.CommentFacetCompacted = won.WON.prefix +":CommentFacet";
        won.WON.CoordinatorFacet = won.WON.baseUri +"CoordinatorFacet";
        won.WON.CoordinatorFacetCompacted = won.WON.prefix +":CoordinatorFacet";
        won.WON.belongsToNeed = won.WON.baseUri + "belongsToNeed";
        won.WON.belongsToNeedCompacted = won.WON.prefix + ":belongsToNeed";
        won.WON.hasBasicNeedType = won.WON.baseUri + "hasBasicNeedType";
        won.WON.hasBasicNeedTypeCompacted = won.WON.prefix + ":hasBasicNeedType";
        won.WON.hasConnections = won.WON.baseUri + "hasConnections";
        won.WON.hasConnectionsCompacted = won.WON.prefix + ":hasConnections";
        won.WON.hasConnectionState = won.WON.baseUri + "hasConnectionState";
        won.WON.hasConnectionStateCompacted = won.WON.prefix + ":hasConnectionState";
        won.WON.hasContent = won.WON.baseUri + "hasContent";
        won.WON.hasContentCompacted = won.WON.prefix + ":hasContent";
        won.WON.hasEndTime = won.WON.baseUri + "hasEndTime";
        won.WON.hasEndTimeCompacted = won.WON.prefix + ":hasEndTime";
        won.WON.hasEventContainer = won.WON.baseUri + "hasEventContainer";
        won.WON.hasEventContainerCompacted = won.WON.prefix + ":hasEventContainer";
        won.WON.hasOriginator = won.WON.baseUri + "hasOriginator";
        won.WON.hasOriginatorCompacted = won.WON.prefix + ":hasOriginator";
        won.WON.hasRecurInfiniteTimes = won.WON.baseUri + "hasRecurInfiniteTimes";
        won.WON.hasRecurInfiniteTimesCompacted = won.WON.prefix + ":hasRecurInfiniteTimes";
        won.WON.hasRecursIn = won.WON.baseUri + "hasRecursIn";
        won.WON.hasRecursInCompacted = won.WON.prefix + ":hasRecurInfiniteTimes";
        won.WON.hasScore = won.WON.baseUri + "hasScore";
        won.WON.hasScoreCompacted = won.WON.prefix + ":hasScore";
        won.WON.hasStartTime = won.WON.baseUri + "hasStartTime";
        won.WON.hasStartTimeCompacted = won.WON.prefix + ":hasStartTime";
        won.WON.hasTag = won.WON.baseUri + "hasTag";
        won.WON.hasTagCompacted = won.WON.prefix + ":hasTag";
        won.WON.hasTextDescription = won.WON.baseUri + "hasTextDescription";
        won.WON.hasTextDescriptionCompacted = won.WON.prefix + ":hasTextDescription";

        won.WON.hasMatchScore = won.WON.baseURI + "hasMatchScore";
        won.WON.hasMatchScoreCompacted = won.WON.prefix + ":hasMatchScore";
        won.WON.hasMatchCounterpart = won.WON.baseURI + "hasMatchCounterpart";
        won.WON.hasMatchCounterpart = won.WON.prefix + ":hasMatchCounterpart";
        won.WON.hasTextMessage= won.WON.baseURI + "hasTextMessage";
        won.WON.hasTextMessageCompacted= won.WON.prefix + ":hasTextMessage";


        won.WONMSG = {};
        won.WONMSG.baseUri = "http://purl.org/webofneeds/message#";
        won.WONMSG.prefix = "msg";

        //sender/receiver etc.
        won.WONMSG.hasReceiverNeed = won.WONMSG.baseUri + "hasReceiverNeed";
        won.WONMSG.hasReceiverNeedCompacted = won.WONMSG.prefix + ":hasReceiverNeed";
        won.WONMSG.hasReceiver = won.WONMSG.baseUri + "hasReceiver";
        won.WONMSG.hasReceiverCompacted = won.WONMSG.prefix + ":hasReceiver";
        won.WONMSG.hasReceiverNode = won.WONMSG.baseUri + "hasReceiverNode";
        won.WONMSG.hasReceiverNodeCompacted = won.WONMSG.prefix + ":hasReceiverNode";
        won.WONMSG.hasSenderNeed = won.WONMSG.baseUri + "hasSenderNeed";
        won.WONMSG.hasSenderNeedCompacted = won.WONMSG.prefix + ":hasSenderNeed";
        won.WONMSG.hasSender = won.WONMSG.baseUri + "hasSender";
        won.WONMSG.hasSenderCompacted = won.WONMSG.prefix + ":hasSender";
        won.WONMSG.hasSenderNode = won.WONMSG.baseUri + "hasSenderNode";
        won.WONMSG.hasSenderNodeCompacted = won.WONMSG.prefix + ":hasSenderNode";
        won.WONMSG.hasMessageType = won.WONMSG.baseUri + ":hasMessageType";
        won.WONMSG.hasMessageTypeCompacted = won.WONMSG.prefix + ":hasMessageType";
        won.WONMSG.hasTimestamp = won.WONMSG.baseUri + "hasTimestamp"
        won.WONMSG.hasTimestampCompacted = won.WONMSG.prefix + ":hasTimestamp";
        won.WONMSG.refersTo = won.WONMSG.baseUri + "refersTo";
        won.WONMSG.refersToCompacted = won.WONMSG.prefix + ":refersTo";
        won.WONMSG.EnvelopeGraph = won.WONMSG.baseUri + "EnvelopeGraph";
        won.WONMSG.EnvelopeGraphCompacted = won.WONMSG.prefix+ ":EnvelopeGraph";
        won.WONMSG.hasContent = won.WONMSG.baseUri + "hasContent";
        won.WONMSG.hasContentCompacted = won.WONMSG.prefix+ ":hasContent";

        //message types
        won.WONMSG.createMessage = won.WONMSG.baseUri + "CreateMessage";
        won.WONMSG.createMessageCompacted = won.WONMSG.prefix + ":CreateMessage";
        won.WONMSG.hintMessage = won.WONMSG.baseUri + "HintMessage";
        won.WONMSG.hintMessageCompacted = won.WONMSG.prefix + ":HintMessage";
        won.WONMSG.connectMessage = won.WONMSG.baseUri + "ConnectMessage";
        won.WONMSG.connectMessageCompacted = won.WONMSG.prefix + ":ConnectMessage";
        won.WONMSG.needStateMessage = won.WONMSG.baseUri + "NeedStateMessage";
        won.WONMSG.needStateMessageCompacted = won.WONMSG.prefix + ":NeedStateMessage";
        won.WONMSG.closeMessage = won.WONMSG.baseUri + "CloseMessage";
        won.WONMSG.closeMessageCompacted = won.WONMSG.prefix + ":CloseMessage";
        won.WONMSG.openMessage = won.WONMSG.baseUri + "OpenMessage";
        won.WONMSG.openMessageCompacted = won.WONMSG.prefix + ":OpenMessage";
        won.WONMSG.connectionMessage = won.WONMSG.baseUri + "ConnectionMessage";
        won.WONMSG.connectionMessageCompacted = won.WONMSG.prefix + ":ConnectionMessage";

        //response types
        won.WONMSG.hasResponseStateProperty = won.WONMSG.baseUri + "hasResponseStateProperty";
        won.WONMSG.hasResponseStateCompacted = won.WONMSG.prefix + ":hasResponseStateProperty";
        won.WONMSG.createResponseMessage = won.WONMSG.baseUri + "CreateResponseMessage";
        won.WONMSG.createResponseMessageCompacted = won.WONMSG.prefix + ":CreateResponseMessage";
        won.WONMSG.connectResponseMessage = won.WONMSG.baseUri + "ConnectResponseMessage";
        won.WONMSG.connectResponseMessageCompacted = won.WONMSG.prefix + ":ConnectResponseMessage";
        won.WONMSG.needStateResponseMessage = won.WONMSG.baseUri + "NeedStateResponseMessage";
        won.WONMSG.needStateResponseMessageCompacted = won.WONMSG.prefix + ":NeedStateResponseMessage";
        won.WONMSG.closeResponseMessage = won.WONMSG.baseUri + "CloseResponseMessage";
        won.WONMSG.closeResponseMessageCompacted = won.WONMSG.prefix + ":CloseResponseMessage";
        won.WONMSG.openResponseMessage = won.WONMSG.baseUri + "OpenResponseMessage";
        won.WONMSG.openResponseMessageCompacted = won.WONMSG.prefix + ":OpenResponseMessage";
        won.WONMSG.connectionMessageResponseMessage = won.WONMSG.baseUri + "ConnectionMessageResponseMessage";
        won.WONMSG.connectionMessageResponseMessageCompacted = won.WONMSG.prefix + ":ConnectionMessageResponseMessage";

        //notification types
        won.WONMSG.hintNotificationMessage = won.WONMSG.baseUri + "HintNotificationMessage";
        won.WONMSG.hintNotificationMessageCompacted = won.WONMSG.prefix + ":HintNotificationMessage";


        won.EVENT = {};
        won.EVENT.WON_MESSAGE_RECEIVED = "WonMessageReceived";
        won.EVENT.NEED_CREATED = "NeedCreatedEvent";
        won.EVENT.HINT_RECEIVED = "HintReceivedEvent";
        won.EVENT.CONNECT.SENT ="ConnectSentEvent";
        won.EVENT.CONNECT_RECEIVED = "ConnectReceivedEvent";
        won.EVENT.OPEN_SENT = "OpenSentEvent";
        won.EVENT.OPEN_RECEIVED = "OpenReceivedEvent";
        won.EVENT.CLOSE_SENT = "CloseSentEvent";
        won.EVENT.CLOSE_RECEIVED = "CloseReceivedEvent";
        won.EVENT.CONNECTION_MESSAGE_RECEIVED = "ConnectionMessageReceivedEvent";
        won.EVENT.NEED_STATE_MESSAGE_RECEIVED = "NeedStateMessageReceivedEvent";

        won.EVENT.APPSTATE_CURRENT_NEED_CHANGED = "AppState.CurrentNeedChangedEvent";

        //keys for things that can be shown in the GUI as 'unread'
        won.UNREAD = {};
        won.UNREAD.TYPE = {};
        won.UNREAD.TYPE.CREATED = "created";
        won.UNREAD.TYPE.HINT = "hint";
        won.UNREAD.TYPE.MESSAGE = "message";
        won.UNREAD.TYPE.CONNECT = "connect";
        won.UNREAD.TYPE.CLOSE = "close";
        won.UNREAD.GROUP = {};
        won.UNREAD.GROUP.ALL="all";
        won.UNREAD.GROUP.BYNEED="byNeed";

        //UTILS
        var UNSET_URI= "no:uri";

        won.clone = function(obj){
            return JSON.parse(JSON.stringify(obj));
        }

        //get the URI from a jsonld resource (expects an object with an '@id' property)
        //or a value from a literal
        won.getSafeJsonLdValue = function(dataItem) {
            if (dataItem == null) return null;
            if (typeof dataItem === 'object') {
                if (dataItem['@id'] != null) return dataItem['@id'];
            } else {
                return dataItem;
            }
            return null;
        }

        won.getLocalName = function(uriOrQname) {
            if (uriOrQname == null || typeof uriOrQname !== 'string') return null;
            //first, try to get the URI hash fragment (without hash)
            var pos = uriOrQname.lastIndexOf('#')
            if ( pos > -1 && pos < uriOrQname.length) {
                return uriOrQname.substring(pos + 1);
            }
            //try portion after last trailing slash
            pos = uriOrQname.lastIndexOf('/')
            if ( pos > -1 && pos < uriOrQname.length) {
                return uriOrQname.substring(pos + 1);
            }
            //take portion after last ':'
            pos = uriOrQname.lastIndexOf(':')
            if ( pos > -1 && pos < uriOrQname.length) {
                return uriOrQname.substring(pos + 1);
            }
            return uriOrQname;
        }

        won.isJsonLdKeyword = function(propertyName) {
            if (propertyName == null || typeof propertyName !== 'string') return false;
            return propertyName.indexOf('@') == 0;
        }

        won.reportError = function(message) {
            if (arguments.length == 1) {
                return function(reason) {
                    console.log(message + " reason: " + reason);
                }
            }
            return function(reason) {
                console.log("Error! reason: " + reason);
            }
        }

        //helper function: is x an array?
        won.isArray = function(x){
            return Object.prototype.toString.call( x ) === '[object Array]';
        }

        won.replaceRegExp = function (string) {
            return string.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
        }

        /**
         * Deletes every element of the array for which the
         * test function returns true.
         * @param array
         * @param test
         */
        won.deleteWhere = function(array, test) {
            for (var i = 0; i< array.length; i++){
                if (test(array[i])){
                    array.splice(i,1);
                    i--;
                }
            }
            return array;
        }

        /**
         * Deletes all null entries in the specified array.
         * @param array
         */
        won.deleteWhereNull = function(array){
            return won.deleteWhere(array, function(x){ return x == null});
        }

        /**
         * Visits the specified data structure. For each element, the callback is called
         * as callback(element, key, container) where the key is the key of the element
         * in its container or callback (element, null, null) if there is no such container).
         */
        won.visitDepthFirst = function(data, callback, currentKey, currentContainer){
            if (data == null) return;
            if (won.isArray(data) && data.length > 0){
                for (key in data) {
                    won.visitDepthFirst(data[key], callback, key, data);
                }
                return;
            }
            if (typeof data === 'object'){
                for (key in data) {
                    won.visitDepthFirst(data[key], callback, key, data);
                }
                return;
            }
            //not a container: visit value.
            callback(data, currentKey, currentContainer);
        }



        won.minimalContext = {
            "msg": "http://purl.org/webofneeds/message#",
            "won": "http://purl.org/webofneeds/model#",
            "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        }

        won.defaultContext = {
                "webID": "http://www.example.com/webids/",
                "msg": "http://purl.org/webofneeds/message#",
                "dc": "http://purl.org/dc/elements/1.1/",
                "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
                "geo": "http://www.w3.org/2003/01/geo/wgs84_pos#",
                "xsd": "http://www.w3.org/2001/XMLSchema#",
                "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                "won": "http://purl.org/webofneeds/model#",
                "gr": "http://purl.org/goodrelations/v1#",
                "ldp": "http://www.w3.org/ns/ldp#",
                "won:hasContent":{
                    "@id":"http://purl.org/webofneeds/model#hasContent",
                    "@type":"@id"
                },
                "msg:hasMessageType":{
                    "@id":"http://purl.org/webofneeds/message#hasMessageType",
                    "@type":"@id"
                }


        }


        won.JsonLdHelper = {
            /**
             * Returns all graph URIs. If none are found, an empty array is returned.
             * @returns {Array}
             */
            getGraphNames: function(data){
                //collect graph URIs in the specified dataset
                var graphs = data["@graph"]
                var graphURIs = [];
                if (graphs == null) {
                    return graphURIs;
                }
                if (won.isArray(graphs) && graphs.length > 0) {
                    for (var i = 0; i < graphs.length; i++){
                        var graphURI = graphs[i]['@id'];
                        if (graphURI != null){
                            graphURIs.push(graphURI);
                        }
                    }
                } else if (typeof graphs === 'object' && graphs["@graph"] != null){
                    return won.JsonLdHelper.getGraphNames(graphs['@graph']);
                }
                return graphURIs;
            },

            getDefaultGraph: function(data){
                if (data['@graph'] != null) {
                  //graph keyword is present. It could represent the default graph
                  // (in which case it contains only nodes) or a collection of
                  //, which are nodes that contain th @graph keyword.
                  //our naive test is: if the first node contains an '@graph' keyword
                  //we assume the outermost @graph array to contain only named graphs.
                  // we search for the one with '@id'='@default' or without
                  // an '@id' keyword and return it (if we find it)
                  //if the first node doesn't contain an @graph keyword, we assume that there
                  //are no named graphs and all data is in the default graph.
                  outermostGraphContent = data['@graph'];
                  for (var i = 0; i < outermostGraphContent.length; i++) {
                    var curNode = outermostGraphContent[i];
                    if (curNode['@graph'] == null){
                        //we assume there are no named graphs, the outermost graph is the default graph
                        return outermostGraphContent;
                    }
                    if (curNode['@id'] == null || curNode['@id'] === '@default'){
                        //we've found the named graph without an @id attribute - that's the default graph
                        return curNode['@graph'];
                    }
                  }
                  return null; //no default graph found
                } else {
                    //there is no @graph keyword at top level:
                    return data;
                }
            },
            getNamedGraph: function(data, graphName) {
                if (data['@graph'] != null) {
                    if (data['@id'] != null) {
                        if (data['@id'] === graphName) {
                            return data['@graph'];
                        }
                    } else {
                        //outermost node has '@graph' but no '@id'
                        //--> @graph array contains named graphs. search for name.
                        outermostGraphContent = data['@graph'];
                        for (var i = 0; i < outermostGraphContent.length; i++) {
                            var curNode = outermostGraphContent[i];
                            if (curNode['@id'] == null || curNode['@id'] === graphName) {
                                //we've found the named graph without an @id attribute - that's the default graph
                                return curNode['@graph'];
                            }
                        }
                    }
                }
                return null;
            },
            getNodeInGraph: function(data, graphName, nodeId){
                var graph = this.getNamedGraph(data, graphName);
                for (key in graph['@graph']){
                    var curNode = graph['@graph'][key];
                    var curNodeId = node['@id'];
                    if (curNodeId === nodeId){
                        return curNode;
                    }
                }
                return null;
            },
            addDataToNode: function(data, graphName, nodeId, predicate, object){
                var node = getNodeInGraph(data, graphName, nodeId);
                if (node != null) {
                    node[predicate] = object;
                }
            },
            getContext :  function (data) {
                return data["@context"];
            }
        }

        /**
         *  Adds a msg:hasContent triple for each specified graphURI into the message graph
         * @param messageGraph
         * @param graphURIs
         */
        won.addContentGraphReferencesToMessageGraph = function(messageGraph, graphURIs){
            if (graphURIs != null) {
                if (won.isArray(graphURIs) && graphURIs.length > 0) {
                    //if the message graph already contains content references, fetch them:
                    var existingContentRefs = messageGraph["@graph"][0][won.WONMSG.hasContentCompacted];
                    var contentGraphURIs = (typeof existingContentRefs === 'undefined' || ! isArray(existingContentRefs))? []: existingContentRefs;
                    for (var i = 0; i < graphURIs.length; i++) {
                        contentGraphURIs.push({'@id': graphURIs[i]});
                    }
                    messageGraph["@graph"][0][won.WONMSG.hasContentCompacted] = contentGraphURIs;
                }
            }
        }

        /**
         * Adds the message graph to the json-ld structure 'builder.data' with
         * the specified messageType
         * and adds all specified graph URIs (which must be URIs of the
         * graphs to be added as content of the message) with triples
         * [message] wonmsg:hasContent [graphURI]
         * @param graphURIs
         * @returns {won.CreateMessageBuilder}
         */
        won.addMessageGraph = function (builder, graphURIs, messageType) {
            graphs = builder.data['@graph'];
            //add the default graph to the graphs of the builder
            var defaultGraph = won.JsonLdHelper.getDefaultGraph(builder.data);
            if (defaultGraph == null) {
                defaultGraph =
                    {
                        "@graph": [],
                        "@id": "@default"
                    };
                graphs.push(defaultGraph);
            }
            defaultGraph["@graph"].push({"@id": UNSET_URI + "#data", "@type": "msg:EnvelopeGraph" });

            //create the message graph, containing the message type
            var messageGraph = {
                "@graph": [
                    {
                        "@id":UNSET_URI,
                        "msg:hasMessageType": {'@id':messageType}
                    }
                ],
                "@id": UNSET_URI + "#data"
            };
            won.addContentGraphReferencesToMessageGraph(messageGraph, graphURIs);
            //add the message graph to the graphs of the builder
            graphs.push(messageGraph);
            //point to the messagegraph so we can later access it easily for modifications
            builder.messageGraph = messageGraph;
        };

        /*
         * Creates a JSON-LD representation of the need data provided through builder functions.
         * e.g.:
         * jsonLD = new NeedBuilder().title("taxi").description("need a taxi").build();
         */
        won.NeedBuilder = function NeedBuilder(data) {
            if (data != null && data != undefined) {
                this.data = won.clone(data);
            } else {
                this.data =
                {
                    "@graph": [
                        {
                            "@id": "no-id-yet",
                            "@graph": [
                                {
                                    "@type": "won:Need",
                                    "won:hasContent": "_:n01"
                                },
                                {
                                    "@id": "_:n01",
                                    "@type": "won:NeedContent"
                                }
                            ]
                        }
                    ]
                };
            }
        }

        won.NeedBuilder.prototype = {
            constructor: won.NeedBuilder,
            setContext: function(){
                this.data["@context"] = won.clone(won.defaultContext);

                return this;
            },
            getContext: function () {               //TODO inherit from base buiilder
                return this.data["@context"];
            },
            getNeedGraph: function(){
                return this.data["@graph"][0]["@graph"];
            },
            getContentNode: function () {
                return this.data["@graph"][0]["@graph"][1];
            },
            getMainNode: function () {
                return this.data["@graph"][0]["@graph"][0];
            },

            supply: function () {
                return this.basicNeedType("won:Supply");
            },
            demand: function () {
                return this.basicNeedType("won:Demand");
            },
            doTogether: function () {
                return this.basicNeedType("won:DoTogether");
            },
            critique: function () {
                return this.basicNeedType("won:Critique");
            },
            basicNeedType: function (type) {
                this.getContext()["won:hasBasicNeedType"]= {
                    "@id":"http://purl.org/webofneeds/model#hasBasicNeedType",
                        "@type":"@id"
                },
                this.getMainNode()["won:hasBasicNeedType"] = type;
                return this;
            },
            ownerFacet: function(){
                return this.hasFacet(won.WON.OwnerFacetCompacted);
            },
            groupFacet: function(){
                return this.hasFacet(won.WON.GroupFacetCompacted);
            },
            coordinatorFacet: function(){
                return this.hasFacet(won.WON.CoordinatorFacetCompacted);
            },
            participantFacet: function(){
                return this.hasFacet(won.WON.ParticipantFacetCompacted);
            },
            commentFacet: function(){
                return this.hasFacet(won.WON.CommentFacetCompacted);
            },
            commentModeratedFacet: function(){
                return this.hasFacet("won:CommentModeratedFacet");
            },
            commentUnrestrictedFacet: function(){
                return this.hasFacet("won:CommentUnrestrictedFacet");
            },
            controlFacet: function(){
                return this.hasFacet("won:ControlFacet");
            },
            BAPCCoordinatorFacet: function(){
                return this.hasFacet("won:BAPCCordinatorFacet");
            },
            BAPCParticipantFacet: function(){
                return this.hasFacet("won:BAPCParticipantFacet");
            },
            BACCCoordinatorFacet: function(){
                return this.hasFacet("won:BACCCoordinatorFacet");
            },
            BACCParticipantFacet: function(){
                return this.hasFacet("won:BACCParticipantFacet");
            },
            BAAtomicPCCoordinatorFacet: function(){
                return this.hasFacet("won:BAAtomicPCCoordinatorFacet");
            },
            BAAtomicCCCoordinatorFacet: function(){
                return this.hasFacet("won:BAAtomicCCCoordinatorFacet");
            },
            hasFacet: function(facetType){
                this.getContext()["won:hasFacet"]={
                    "@id":"http://purl.org/webofneeds/model#hasFacet",
                        "@type":"@id"
                },
                this.getMainNode()["won:hasFacet"]=facetType;
                return this;
            },
            active: function(){
                return this.inState("won:Active")
            },
            inactive: function(){
                return this.inState("won:Inactive")
            },
            inState: function(state){
                this.getContext()["won:isInState"]={
                    "@id":"http://purl.org/webofneeds/model#isInState",
                    "@type":"@id"
                },
                this.getMainNode()["won:isInState"] = state;
                return this;
            },
          /*  facets: function(facets){
                this.getMainNode()["won:hasFacet"]=
            }     */


            description: function (description) {
                this.getContentNode()["won:hasTextDescription"] = description;
                return this;
            },

            title: function (title) {
                this.getContentNode()["dc:title"] = title;
                return this;
            },
            /**
             * in order to add price, location, time description hasContentDescription() shall be called first. then use getContentDescriptionNode()
             */

            hasContentDescription: function(){
                this.getContext()["won:hasContentDescription"]={
                    "@id":"http://purl.org/webofneeds/model#hasContentDescription",
                    "@type":"@id"
                },
                this.getContentNode()["won:hasContentDescription"]="_:contentDescription";
                this.getNeedGraph()[2]={
                    "@id":this.getContentNode()["won:hasContentDescription"],
                    "@type":"won:NeedModality"
                }
                this.getContentDescriptionNode = function(){
                    return this.getNeedGraph()[2];
                }
                return this;
            },
            hasPriceSpecification: function(currency, lowerLimit, upperLimit){
                this.getContext()["won:hasCurrency"]={
                    "@type":"xsd:string"
                },
                    this.getContext()["won:hasLowerPriceLimit"]={
                    "@type":"xsd:float"
                },
                    this.getContext()["won:hasUpperPriceLimit"]={
                    "@type":"xsd:float"
                },
                this.getContentDescriptionNode()["won:hasPriceSpecification"]= {
                    "@id":"_:priceSpecification",
                    "@type":"won:PriceSpecification",
                    "won:hasCurrency":currency,
                    "won:hasLowerPriceLimit":lowerLimit,
                    "won:hasUpperPriceLimit":upperLimit
                }
                return this;
            },
            hasLocationSpecification: function(latitude, longitude){
                this.getContext()["geo:latitude"]={
                    "@type":"xsd:float"
                },
                this.getContext()["geo:latitude"]={
                    "@type":"xsd:float"
                },
                this.getContentDescriptionNode()["won:hasLocationSpecification"]={
                    "@id":"_:locationSpecification",
                    "@type":"geo:Point",
                    "geo:latitude":latitude.toFixed(6),
                    "geo:longitude":longitude.toFixed(6)
                }
                return this;
            },
            hasTimeSpecification: function(startTime, endTime, recurInfinite, recursIn, recurTimes){
                this.getContext()["won:hasStartTime"]= {
                    "@type":"xsd:dateTime"
                }
                this.getContext()["won:hasEndTime"]={
                    "@type":"xsd:dateTime"
                }
                this.getContentDescriptionNode()["won:hasTimespecification"]={
                    "@type":"won:TimeSpecification",
                    "won:hasRecurInfiniteTimes":recurInfinite,
                    "won:hasRecursIn":recursIn,
                    "won:hasStartTime":startTime,
                    "won:hasEndTime":endTime
                }
                return this;
            },
            hasTag: function(tags){
                this.getContentNode()["won:hasTag"] = tags;
                return this;
            },
            //TODO: add images
            build: function () {

                return this.data;
            }
        }

        /*
         * Creates a JSON-LD stucture containing a named graph with default 'unset' event URI
         * plus the specified hashFragment
         *
         */
        won.newGraph = function (hashFragement) {
            hashFragement = hashFragement || 'graph1';
            return {"@graph": [
                        {
                            "@id": UNSET_URI + "#" + hashFragement,
                            "@graph": []
                        }
                    ]
                };
        }


        /**
         * Builds a JSON-LD WoN Message or adds the relevant data to the specified
         * JSON-LD data structure.
         * @param messageType a fully qualified URI
         * @param content a JSON-LD structure or null
         * @constructor
         */
        won.MessageBuilder = function MessageBuilder(messageType, content) {
            if (messageType == null) {
                throw { message : "messageType must not be null!" };
            }
            var graphNames = null;
            if (content != null) {
                this.data = won.clone(content);
                graphNames = won.JsonLdHelper.getGraphNames(this.data);
            } else {
                this.data =
                {
                    "@graph": [

                    ],
                    "@context" : won.clone(won.minimalContext)
                }
            }
            this.messageGraph = null;
            this.eventUriValue = UNSET_URI;
            won.addMessageGraph(this, graphNames , messageType);
        };

        won.MessageBuilder.prototype = {
            constructor: won.MessageBuilder,

            eventURI: function (eventUri) {
                this.getContext()[won.WONMSG.EnvelopeGraphCompacted]= {
                    "@id": "http://purl.org/webofneeds/message#EnvelopeGraph",
                    "@type": "@id"
                };
                var regex = new RegExp(won.replaceRegExp(this.eventUriValue));
                won.visitDepthFirst(this.data, function(element, key, collection){
                    if (collection != null && key === '@id'){
                        if (element)
                        collection[key] = element.replace(regex, eventUri);
                    }
                });
                this.eventUriValue = eventUri;
                return this;
            },
            getContext :  function () {
                return this.data["@context"];
            },
            forEnvelopeData: function (envelopeData){
                var node = this.getMessageEventNode();
                for (key in envelopeData){
                    node[key] = {"@id":envelopeData[key]};
                }
                return this;
            },
            hasSenderNeed: function(senderNeedURI){
                this.getMessageEventNode()[won.WONMSG.hasSenderNeedCompacted]={"@id":senderNeedURI};
                return this;
            },
            hasSenderNode: function(senderNodeURI){
                this.getMessageEventNode()[won.WONMSG.hasSenderNodeCompacted]={"@id":senderNodeURI};
                return this;
            },
            hasSender: function(senderURI){
                this.getMessageEventNode()[won.WONMSG.hasSenderCompacted]={"@id":senderURI};
                return this;
            },
            hasReceiver: function(receiverURI){
                this.getMessageEventNode()[won.WONMSG.hasReceiverCompacted]={"@id":receiverURI};
                return this;
            },
            hasReceiverNeed: function(receiverNeedURI){
                this.getMessageEventNode()[won.WONMSG.hasReceiverNeedCompacted]={"@id":receiverNeedURI};
                return this;
            },
            hasReceiverNode: function(receiverURI){
                this.getMessageEventNode()[won.WONMSG.hasReceiverNodeCompacted]={"@id":receiverURI};
                return this;
            },
            hasTimestamp: function(){
                this.getMessageEventNode()[won.WONMSG.hasTimestampCompacted]=new Date().getTime();
                return this;
            },
            /**
             * Adds the specified facet as local facets. Only needed for connect and
             * openSuggested.
             * @param receiverURI
             * @returns {won.MessageBuilder}
             */
            hasFacet: function(facetURI){
                this.getContentGraphNode()[won.WON.hasFacetCompacted]={"@id":facetURI};
                return this;
            },
            /**
             * Adds the specified facet as local facets. Only needed for connect and
             * openSuggested.
             * @param receiverURI
             * @returns {won.MessageBuilder}
             */
            hasRemoteFacet: function(facetURI){
                this.getContentGraphNode()[won.WON.hasRemoteFacetCompacted]={"@id":facetURI};
                return this;
            },
            getMessageEventGraph: function (){
                return this.messageGraph;
            },
            getMessageEventNode: function () {
                return this.getMessageEventGraph()["@graph"][0];
            },
            /**
             * Fetches the content graph, creating it if it doesn't exist.
             */
            getContentGraph: function(){
                var graphs = this.data["@graph"];
                for (key in graphs){
                    var graph = graphs[key];
                    if (graph['@id'] === this.eventUriValue + "#content"){
                        return graph;
                    }
                }
                //none found: create it
                var contentGraph = {
                    "@id": this.eventUriValue + "#content",
                    "@graph" :[
                        {"@id": this.eventUriValue}
                    ]
                }
                graphs.push(contentGraph);
                return contentGraph;
            },
            getContentGraphNode: function(){
                return this.getContentGraph()["@graph"][0];
            },
            addContentGraphData: function(predicate, object){
                this.getContentGraphNode()[predicate] = object;
                return this;
            },
            build: function () {
                console.log("built this message:" + JSON.stringify(this.data));
                return this.data;
            }
        };
        
        
        return won;
    };
    var factory = function() {
        return wrapper(function() {
            return factory();
        });
    };
// the shared global wonmessagebuilder API instance
    wrapper(factory);
   if(_browser) {
        // export simple browser API
        if(typeof won === 'undefined') {
            won = wonjs = factory;
        } else {
            won = factory;
        }
    }
})();


