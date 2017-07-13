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
 * Created by LEIH-NB on 27.08.2014.
 */

angular.module('won.owner').factory('utilService', function ($http) {
    var utilService = {};
    utilService.removeAllProperties = function (obj){
        Object.keys(obj).forEach(function(element,index,array){
            delete obj[element];
        })
    }
    utilService.getKeySize = function(obj) {
        return Object.keys(obj).length;
    };
    utilService.getRandomPosInt = function() {
        return utilService.getRandomInt(1,9223372036854775807);
    }
    utilService.getRandomInt = function(min, max){
        return Math.floor(Math.random()*(max-min+1))+min;
    }

    // TODO angularjs seems to have analogous method, so usage of this one can be replaced by angular's
    utilService.isString = function(o) {
        return typeof o == "string" || (typeof o == "object" && o.constructor === String);
    }

    utilService.readAsDataURL  = function(file) {
        return new Promise((resolve, reject) => {
            var reader = new FileReader();
            reader.onload = () => resolve(reader.result);
            reader.onerror = () => reject(f);
            reader.readAsDataURL(file);
        });
    };

    utilService.concatTags = function(tags) {
        if(tags.length>0){
            var concTags ='';
            for(var i = 0;i<tags.length;i++){
                if(i==0){
                    concTags = tags[i].text;
                }else{
                    concTags = concTags + ','+ tags[i].text;
                }
            }
            return concTags;
        }
    }
    return utilService;
});