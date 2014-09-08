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
 * Created by fkleedorfer on 05.09.2014.
 */

angular.module('won.owner').factory('linkedDataService', function ($q) {
    linkedDataService = {};

    privateData = {};

    //create an rdfstore-js based store as a cache for rdf data.
    privateData.store =  rdfstore.create();

    /**
     * Fetches the linked data for the specified URI and saves it in the local triplestore.
     * @param uri
     * @return a promise to a boolean which indicates success
     */
    linkedDataService.fetch = function(uri) {
        var deferred = $q.defer();
        try {
            privateData.store.load('remote', uri, function (success, results) {
                deferred.resolve(success);
            });
        } catch (e) {
            deferred.reject(e);
        }
        return deferred.promise;
    }

    /**
     * Retrieves the RDF data by dereferencing the specified URI.
     * @param uri
     * @param forceFetch if true, data will be fetched via http and updated in the cache before being returned.
     * @return a promise to the data, which is represented as JSON-LD.
     */
    linkedDataService.get = function(uri, forceFetch) {
        if (typeof forceFetch === 'undefined'){
            forceFetch = false;
        }
        var deferred = $q.defer();
        try {
            var done = false;
            //load the data from the local rdf store if forceFetch is false
            if (! forceFetch) {
                privateData.store.graph(uri, function (success, mygraph) {
                    if (success) {
                        deferred.resolve(mygraph);
                        done = true;
                    }
                })
            }
            if (done) {
                //if we found the data, we're done!
                return deferred.promise;
            }
            //we're not done yet - we have to fetch the data remotely
            linkedDataService.fetch(uri).then(
                function(successValue) {
                    //ignore successValue 'true'
                    deferred.notify("fetched data for " + uri);
                    //now get the data from the store and return
                    privateData.store.graph(uri, function(success, graph) {
                        deferred.resolve(graph);
                    })
                },
                function(reason) {
                    //handle error when fetching the data
                    deferred.reject("cannot get " + uri + ", reason:" + reason);
                },
                //don't handle updates
                null
            );
        } catch (e){
            deferred.reject(e);
        }
        return deferred.promise;
    }

    return linkedDataService;

});