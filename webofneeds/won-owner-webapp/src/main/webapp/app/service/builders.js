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
        won.WON.BasicNeedTypeDemand = won.WON.baseUri + "Critique";
        won.WON.BasicNeedTypeCritiqueCompacted = won.WON.prefix + ":Critique";
        won.WON.belongsToNeed = won.WON.baseUri + ":belongsToNeed";
        won.WON.belongsToNeedCompacted = won.WON.prefix + ":belongsToNeed";
        won.WON.hasBasicNeedType = won.WON.baseUri + "hasBasicNeedType";
        won.WON.hasBasicNeedTypeCompacted = won.WON.prefix + ":hasBasicNeedType";
        won.WON.hasConnections = won.WON.baseUri + "hasConnections";
        won.WON.hasConnectionsCompacted = won.WON.prefix + ":hasConnections";
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
        won.WON.hasTimeStamp = won.WON.baseUri + "hasTimeStamp"
        won.WON.hasTimeStampCompacted = won.WON.prefix + ":hasTimeStamp";



        won.WONMSG = {};
        won.WONMSG.baseUri = "http://purl.org/webofneeds/message#";
        won.WONMSG.prefix = "wonmsg";
        won.WONMSG.hasReceiverNeed = won.WONMSG.baseUri + "hasReceiverNeed";
        won.WONMSG.hasReceiverNeedCompacted = won.WONMSG.prefix + ":hasReceiverNeed";
        won.WONMSG.hasReceiver = won.WONMSG.baseUri + "hasReceiver";
        won.WONMSG.hasReceiverCompacted = won.WONMSG.prefix + ":hasReceiver";
        won.WONMSG.hasReceiverNode = won.WONMSG.baseUri + "hasReceiverNode";
        won.WONMSG.hasReceiverNodeCompacted = won.WONMSG.prefix + ":hasReceiverNode";
        won.WONMSG.hasResponseStateProperty = won.WONMSG.baseUri + "hasResponseStateProperty";
        won.WONMSG.hasResponseStatePropertyCompacted = won.WONMSG.prefix + ":hasResponseStateProperty";
        won.WONMSG.createResponseMessage = won.WONMSG.baseUri + "CreateResponseMessage";
        won.WONMSG.createResponseMessageCompacted = won.WONMSG.prefix + ":CreateResponseMessage";
        won.WONMSG.hintMessage = won.WONMSG.baseUri + "HintMessage";
        won.WONMSG.hintMessageCompacted = won.WONMSG.prefix + ":HintMessage";
        won.WONMSG.hasSenderNeed = won.WONMSG.baseUri + "hasSenderNeed";
        won.WONMSG.hasSenderNeedCompacted = won.WONMSG.prefix + ":hasSenderNeed";
        won.WONMSG.hasSender = won.WONMSG.baseUri + "hasSender";
        won.WONMSG.hasSenderCompacted = won.WONMSG.prefix + ":hasSender";
        won.WONMSG.hasSenderNode = won.WONMSG.baseUri + "hasSenderNode";
        won.WONMSG.hasSenderNodeCompacted = won.WONMSG.prefix + ":hasSenderNode";
        won.WONMSG.hasMessageTypeProperty = won.WONMSG.baseUri + ":hasMessageType";
        won.WONMSG.hasMessageTypePropertyCompacted = won.WONMSG.prefix + ":hasMessageType";
        won.WONMSG.refersTo = won.WONMSG.baseUri + "refersTo";
        won.WONMSG.refersToCompacted = won.WONMSG.prefix + ":refersTo";

        won.WONMSG.MessageEnvelope = won.WONMSG.baseUri + "EnvelopeGraph";
        won.WONMSG.MessageEnvelopeCompacted = won.WONMSG.prefix+ ":EnvelopeGraph";

        won.EVENT = {};
        won.EVENT.WON_MESSAGE_RECEIVED = "WonMessageReceived";
        won.EVENT.NEED_CREATED = "NeedCreatedEvent";

        won.clone = function(obj){
            return JSON.parse(JSON.stringify(obj));
        }
        won.defaultContext = {
                "@base": "http://www.example.com/resource/need/randomNeedID_1",
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
                this.data["@context"] = won.defaultContext;

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
                return this.hasFacet("won:OwnerFacet");
            },
            groupFacet: function(){
                return this.hasFacet("won:GroupFacet");
            },
            coordinatorFacet: function(){
                return this.hasFacet("won:coordinatorFacet");
            },
            participantFacet: function(){
                return this.hasFacet("won:ParticipantFacet");
            },
            commentFacet: function(){
                return this.hasFacet("won:CommentFacet");
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
            needURI: function (needURI) {
                this.getContext()["@base"] = needURI;
                this.getMainNode()["@id"] = needURI;
                this.data["@graph"][0]["@id"] = needURI + "/core#data";
                this.getNeedURI = function (){
                    return this.getContext()["@base"];
                }
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
                    "geo:latitude":latitude,
                    "geo:longitude":longitude
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
       // window.NeedBuilder = NeedBuilder;

        won.CreateMessageBuilder = function CreateMessageBuilder(dataset) {
            this.data = won.clone(dataset);
            this.socket;
        };

        won.CreateMessageBuilder.prototype = {
            constructor: won.CreateMessageBuilder,

            addMessageGraph: function () {
                this.data['@graph'][1] =
                {
                    "@id": "urn:x-arq:DefaultGraphNode",
                    "@graph": []
                };
                this.data['@graph'][2] = {
                    "@graph": [
                        {
                            "msg:hasMessageType": "msg:CreateMessage",
                            "msg:hasContent": [
                                {
                                    "@id": "core#data"
                                },
                                {
                                    "@id": "transient#data"
                                }
                            ]

                        }
                    ]
                }
                return this;
            },
            eventURI: function (eventId) {
                this.getContext()["msg:EnvelopeGraph"]= {
                    "@id": "http://purl.org/webofneeds/message#EnvelopeGraph",
                    "@type": "@id"
                },
                this.getDefaultGraphNode().push({"@id":this.data["@context"]["@base"] + "/event/" + eventId + "#data", "@type": "msg:EnvelopeGraph" });
                this.getMessageEventNode()['@id'] =this.data["@context"]["@base"] +"/event/"+eventId;
                this.getMessageEventGraph()['@id'] = this.data["@context"]["@base"] +"/event/"+eventId+"#data";
                return this;
            },
            getContext :  function () {               //TODO inherit from base buiilder
                return this.data["@context"];
            },
            hasSenderNeed: function(needURI){
                this.getMessageEventNode()["msg:hasSenderNeed"]={"@id":this.data["@context"]["@base"]};
                return this;
            },

            hasReceiverNode: function(receiverNodeURI){
                this.getMessageEventNode()["msg:hasReceiverNode"]={"@id":receiverNodeURI};
                return this;
            },
            getDefaultGraphNode: function () {
                return this.data["@graph"][1]["@graph"];
            },
            getMessageEventGraph: function (){
                return this.data["@graph"][2];
            },
            getMessageEventNode: function () {
                return this.data["@graph"][2]["@graph"][0]
            },
            build: function () {
                return this.data;
            }
        }

        won.ConnectionBuilder = function ConnectionBuilder(data){
            if (data != null && data != undefined) {
                this.data = won.clone(data);
            } else {
                this.data =
                {
                    "@graph": [
                        {
                            "@graph": [
                                {
                                    "@type": "won:Connection"
                                }
                            ]
                        }
                    ]
                };
            }
        }

        won.ConnectionBuilder.prototype = {
            constructor: won.ConnectionBuilder,
            setContext: function(){
                this.data["@context"] = won.defaultContext;

                return this;
            },
            getContext: function () {               //TODO inherit from base buiilder
                return this.data["@context"];
            },
            getConnectionGraph: function(){
                return this.data["@graph"][0]["@graph"];
            },
            getMainNode: function () {
                return this.data["@graph"][0]["@graph"][0];
            },
            connectionURI: function (connectionURI) {
                this.getContext()["@base"] = connectionURI;
                this.getMainNode()["@id"] = connectionURI;
                this.data["@graph"][0]["@id"] = connectionURI + "/core#data";
                return this;
            },
            build: function () {
                console.log("built this data:" + JSON.stringify(this.data));
                return this.data;
            }
        }

        won.ConnectMessageBuilder = function ConnectMessageBuilder(dataset){
            this.data = won.clone(dataset);
            this.socket;

        };

        won.ConnectMessageBuilder.prototype = {
            constructor: won.ConnectMessageBuilder,

                addMessageGraph: function () {
                this.data['@graph'][1] =
                {
                    "@id": "urn:x-arq:DefaultGraphNode",
                    "@graph": []
                };
                this.data['@graph'][2] = {
                    "@graph": [
                        {
                            "msg:hasMessageType": "msg:ConnectMessage"
                        }
                    ]
                }
                return this;
            },
            eventURI: function (eventId) {      //TODO: inherit from base class
                this.getContext()["msg:EnvelopeGraph"]= {
                    "@id": "http://purl.org/webofneeds/message#EnvelopeGraph",
                    "@type": "@id"
                },
                    this.getDefaultGraphNode().push({"@id":this.data["@context"]["@base"] + "/event/" + eventId + "#data", "@type": "msg:EnvelopeGraph" });
                this.getMessageEventNode()['@id'] =this.data["@context"]["@base"] +"/event/"+eventId;
                this.getMessageEventGraph()['@id'] = this.data["@context"]["@base"] +"/event/"+eventId+"#data";
                return this;
            },
            getContext :  function () {               //TODO inherit from base buiilder
                return this.data["@context"];
            },
            hasSenderNeed: function(senderNeedURI){
                this.getMessageEventNode()["msg:hasSenderNeed"]={"@id":senderNeedURI};
                return this;
            },
            hasSenderNode: function(senderNodeURI){
                this.getMessageEventNode()["msg:hasSenderNode"]={"@id":senderNodeURI};
                return this;
            },
            sender: function(){
                this.getMessageEventNode()["msg:sender"]={"@id":this.getContext()["@base"]};
                return this;
            },
            receiver: function(){
                this.getMessageEventNode()["msg:receiver"]={"@id":this.getMessageEventNode()["msg:hasReceiverNeed"]["@id"]+"/facets#owner"};
                return this;
            },
            hasReceiverNeed: function(receiverNeedURI){
                this.getMessageEventNode()["msg:hasReceiverNeed"]={"@id":receiverNeedURI};
                return this;
            },
            hasReceiverNode: function(receiverURI){
                this.getMessageEventNode()["msg:hasReceiverNode"]={"@id":receiverURI};
                return this;
            },
            getDefaultGraphNode: function () {
                return this.data["@graph"][1]["@graph"];
            },
            getMessageEventGraph: function (){
                return this.data["@graph"][2];
            },
            getMessageEventNode: function () {
                return this.data["@graph"][2]["@graph"][0]
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


