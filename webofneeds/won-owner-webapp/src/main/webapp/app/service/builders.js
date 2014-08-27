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
  /*  var url = "http://localhost:8080/owner/msg";

    var options = {debug: true};

   socket =  new SockJS(url, null, options);

    socket.onmessage = function (event) {
        console.log("Received data: "+event.data);

        writeOutput('Received data: ' + event.data);
    };

    socket.onclose = function () {
        console.log("Lost connection")
        writeOutput('Lost connection!');
    };        */
// attaches wonmessagebuilder API to the given object
    var wrapper = function(wonmessagebuilder) {

        wonmessagebuilder.clone = function(obj){
            return JSON.parse(JSON.stringify(obj));
        }

    /*    wonmessagebuilder.sendMessage = function(dataset){
            socket.onopen = function () {
                console.log("connection has been established!")
                writeOutput("connection has been established!");
                socket.send(JSON.stringify(dataset));
            }


        }         */
        /*
         * Creates a JSON-LD representation of the need data provided through builder functions.
         * e.g.:
         * jsonLD = new NeedBuilder().title("taxi").description("need a taxi").build();
         */
        wonmessagebuilder.NeedBuilder = function NeedBuilder(data) {
            if (data != null && data != undefined) {
                this.data = wonmessagebuilder.clone(data);
            } else {
                this.data =
                {
                    "@context": {
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
                        "msg:containsMessage": {
                            "@id": "http://purl.org/webofneeds/message#containsMessage",
                            "@type": "@id"
                        },
                        "won:hasBasicNeedType":{
                            "@id":"http://purl.org/webofneeds/model#hasBasicNeedType",
                            "@type":"@id"
                        },
                        "won:hasContent":{
                            "@id":"http://purl.org/webofneeds/model#hasContent",
                            "@type":"@id"
                        },
                        "won:isInState":{
                            "@id":"http://purl.org/webofneeds/model#isInState",
                            "@type":"@id"
                        },
                        "won:hasContentDescription":{
                            "@id":"http://purl.org/webofneeds/model#hasContentDescription",
                            "@type":"@id"
                        },
                        "won:hasCurrency":{
                            "@type":"xsd:string"
                        },
                        "won:hasLowerPriceLimit":{
                            "@type":"xsd:float"
                        },
                        "won:hasUpperPriceLimit":{
                            "@type":"xsd:float"
                        },
                        "geo:latitude":{
                            "@type":"xsd:float"
                        },
                        "geo:longitude":{
                            "@type":"xsd:float"
                        },
                        "won:hasEndTime":{
                            "@type":"xsd:dateTime"
                        },
                        "won:hasStartTime":{
                            "@type":"xsd:dateTime"
                        }
                    },
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

        wonmessagebuilder.NeedBuilder.prototype = {
            constructor: wonmessagebuilder.NeedBuilder,
            getNeedGraph: function(){
                return this.data["@graph"][0]["@graph"];
            },
            getContentNode: function () {
                return this.data["@graph"][0]["@graph"][1];
            },
            getMainNode: function () {
                return this.data["@graph"][0]["@graph"][0];
            },
            getContext: function () {
                return this.data["@context"];
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
                this.getMainNode()["won:hasBasicNeedType"] = type;
                return this;
            },
            active: function(){
                return this.inState("won:Active")
            },
            inactive: function(){
                return this.inState("won:Inactive")
            },
            inState: function(state){
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
                return this;
            },
            title: function (title) {
                this.getContentNode()["dc:title"] = title;
                return this;
            },
            hasContentDescription: function(){
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
                this.getContentDescriptionNode()["won:hasLocationSpecification"]={
                    "@id":"_:locationSpecification",
                    "@type":"geo:Point",
                    "geo:latitude":latitude,
                    "geo:longitude":longitude
                }
                return this;
            },
            hasTimeSpecification: function(startTime, endTime, recurInfinite, recursIn, recurTimes){
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

        wonmessagebuilder.CreateMessageBuilder = function CreateMessageBuilder(dataset) {
            this.data = wonmessagebuilder.clone(dataset);
            this.socket;
        };

        wonmessagebuilder.CreateMessageBuilder.prototype = {
            constructor: wonmessagebuilder.CreateMessageBuilder,

            addMessageGraph: function () {
                this.data['@graph'][1] =
                {
                    "@id": "urn:x-arq:DefaultGraphNode",
                    "@graph": [
                        {
                            "@id": "_:b0"
                        }
                    ]
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
                this.getDefaultGraphNode()['msg:containsMessage'] = { "@id": this.data["@context"]["@base"] + "/event/" + eventId + "#data" };  //TODO: abstract class
                this.getMessageEventNode()['@id'] =this.data["@context"]["@base"] +"/event/"+eventId;
                this.getMessageEventGraph()['@id'] = this.data["@context"]["@base"] +"/event/"+eventId+"#data";
                return this;
            },
            sender: function(){
                this.getMessageEventNode()["msg:sender"]={"@id":this.data["@context"]["@base"]};
                return this;
            },
            receiver: function(receiverURI){
                this.getMessageEventNode()["msg:receiver"]={"@id":receiverURI};
                return this;
            },
            getDefaultGraphNode: function () {
                return this.data["@graph"][1]["@graph"][0];
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
      //  window.CreateMessageBuilder = CreateMessageBuilder;

//}
        return wonmessagebuilder;
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
        if(typeof wonmessagebuilder === 'undefined') {
            wonmessagebuilder = wonmessagebuilderjs = factory;
        } else {
            wonmessagebuilder = factory;
        }
    }
})();


needJson = new window.wonmessagebuilder.NeedBuilder()
    .title("testneed")
    .demand()
    .needURI("http://localhost:8080/won/resource/need/lk234njkhsdjfgb4l25rtj34")
    .description("just a test")
    .active()
    .supply()
    .build();

messageJson = new window.wonmessagebuilder.CreateMessageBuilder(needJson)
    .addMessageGraph()
    .eventURI("34543242134")
    .sender()
    .receiver("http://localhost:8080/won")
    .build();


