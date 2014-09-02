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
 *    See the License for the specific laAnguage governing permissions and
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
            },
            build: function () {
                console.log("built this data:" + JSON.stringify(this.data));
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
                            "@type": "msg:CreateMessage",
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
                console.log("built this message:" + JSON.stringify(this.data));
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
                            "@type": "msg:ConnectMessage"
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
            hasSenderNeed: function(){
                this.getMessageEventNode()["msg:hasSenderNeed"]={"@id":this.data["@context"]["@base"]};
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
                this.getMessageEventNode()["msg:receiver"]={"@id":this.getMessageEventNode()["msg:hasReceiverNeed"]+"/facets#owner"};
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


