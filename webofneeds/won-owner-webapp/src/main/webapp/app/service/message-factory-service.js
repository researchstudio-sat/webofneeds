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
angular.module('won.owner').factory('messageBuilderService', function ($http, $q) {
    var messageGeneratorService = {};
    var wonMessage = {};
    wonMessage.messageEvent = {};
    wonMessage.messageContent = {};
    wonMessage.messageHeader = {};

    //needEnum = {SUPPLY : "Supply", DEMAND: "Demand", DO_TOGETHER: "Do Together", CRITIQUE: "Critique"};

    messageGeneratorService.generateCreateNeedMessage = function(need){
        wonMessage.messageEvent = {messageURI:"",messageType:"CreateMessage",hasContent:"",signatures:""};
        wonMessage.messageContent = {type:"Need",hasBasicType:need.hasBasicType,textDescription:need.textDescription}
    }

    return messageGeneratorService;
});