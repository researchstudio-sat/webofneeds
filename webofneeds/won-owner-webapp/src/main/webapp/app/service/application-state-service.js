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
 * Created by LEIH-NB on 09.09.2014.
 */

angular.module('won.owner').factory('applicationStateService', function (linkedDataService) {
    var currentNeedURI = null;
    var allNeeds = [];
    var applicationStateService = {}

    applicationStateService.setCurrentNeedURI = function(needURI){
        currentNeedURI = needURI;
    }

    applicationStateService.getCurrentNeedURI = function(){

        return currentNeedURI;
    }

    applicationStateService.getAllNeeds = function(){
        return allNeeds;
    }

    applicationStateService.addNeed = function(need){
        allNeeds.push(need);
    }
    return applicationStateService;
});