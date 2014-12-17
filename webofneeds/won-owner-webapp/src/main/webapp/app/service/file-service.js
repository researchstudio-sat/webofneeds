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
 * Created by syim on 08.08.2014.
 */
angular.module('won.owner').factory('fileService', function ($http, $q,messageService,$log) {
    /*var reader = new FileReader();


    reader.onload = function(e){
        var text = reader.result;
        messageService.sendMessage(text);
    }

    reader.readAsText(file, encoding);
    */
    var fileService = {};
    var jsonLD = require('jsonld');

    fileService.readTextFile(file)
    {
        var rawFile = new XMLHttpRequest();
        rawFile.open("GET",file,false);
        rawFile.onreadystatechange = function()
        {
            if(rawFile.readyState === 4)
            {
                if(rawFile.status === 200 || rawFile.status == 0)
                {
                    var allText = rawFile.responseText;
                    jsonLD.fromRDF(allText,{format:'application/trig'},function(err,doc){
                        $log.debug(doc);
                    });

                }
            }
        }
    }


});