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
 * Created by LEIH-NB on 20.08.2014.
 */
/*
 {
 "@context": {
 "@base": "http://www.example.com/resource/need/randomNeedID_1",
 "webID": "http://www.example.com/webids/",
 "msg": "http://purl.org/webofneeds/message#",
 "dc":	"http://purl.org/dc/elements/1.1/",
 "rdfs":  "http://www.w3.org/2000/01/rdf-schema#",
 "geo":   "http://www.w3.org/2003/01/geo/wgs84_pos#",
 "xsd":   "http://www.w3.org/2001/XMLSchema#",
 "rdf":   "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
 "won":   "http://purl.org/webofneeds/model#",
 "gr":    "http://purl.org/goodrelations/v1#",
 "ldp":   "http://www.w3.org/ns/ldp#",
 "won:Demand": {
 "@type": "won:Demand"
 }
 },
 "@id":  "urn:x-arq:DefaultGraphNode",
 "@graph":[
 {
 "@id" : "_:b0",
 "containsMessage" : ":/event/0#data"
 }
 ],
 "@id": ":/event/0#data",
 "@graph":[
 {
 "@id" : ":/event/0",
 "@type" : "msg:CreateMessage",
 "msg:hasContent" :
 [
 {
 "@id" : ":/core"
 },
 {
 "@id" : ":/transient"
 }
 ]
 }
 ],
 "@id": ":/core#data",
 "@graph":[
 {
 "@id": "http://localhost:8080/won/resource/need/1",
 "@type": "won:Need",
 "won:hasBasicNeedType": {
 "@id": "won:Demand"
 },
 "won:hasContent": {
 "@id": "_:n01"
 }
 },
 {
 "@id": "_:n01",
 "@type": "won:NeedContent",
 "dc:title": "a"
 }
 ]
 }    */