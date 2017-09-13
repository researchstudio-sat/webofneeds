/**
 * Created by ksinger on 28.09.2015.
 */


//TODO switch to requirejs for dependency mngmt (so this lib isn't angular-bound)
//TODO replace calls to `won` object to `require('util')`
import won from './won.js';

(function(){ // <need-builder-js> scope

    won.buildDraftRdf = function(args){
        let createDraftObject = {
            draftURI: $scope.need.needURI,
            draft: {
                '@graph': [
                    { /* need graph */ },

                    { /* attachment graphs* */},

                    {   /* draft state graph (instead of msg-envelope)*/
                        '@id':'no-id-yet',
                        '@graph':[
                            {
                                '@type': 'won:DraftState',
                                'won:hasContent': '_:metaInformation'
                            },
                            {
                                '@id': '_:metaInformation',
                                '@type': 'won:MetaInformation',
                                'won:hasLastEditedFocus': '', //TODO as id-selector or tab-index?
                                //"won:isInStep":"2",
                                //"won:hasMenuposition":"0",
                                //"won:hasDraftObject":{ },
                                "won:lastSavedTimestamp":1444127025209 //TODO unix-time?
                            }
                        ]
                    }
                ],
                '@context': {}
            }
        };

    }

})() // </need-builder-js>
