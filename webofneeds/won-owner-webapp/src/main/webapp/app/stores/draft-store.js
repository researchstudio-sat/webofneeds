/**
 * Created by ksinger on 23.09.2015.
 */
;

import angular from 'angular'; window.angular = angular;
import 'flux-angular'; //monkeypatches the global angular object


function genStoreConf() {
    return {
        drafts: [],
        handlers: {
            'addDraft': 'addDraft'
        },
        addDraft: function (draft) {
            this.drafts.push(draft);
            this.emitChange();
        },
        exports: {
            getLatestDraft: function () {
                return this.drafts[this.drafts.length - 1];
            },
            get drafts() {
                return this.drafts;
            }
        }
    };
}

/*
    .factory('Stores', function (flux) {
        return {
            'StoreA': flux.createStore('StoreA', {}),
            'StoreB': flux.createStore('StoreB', {})
        }
    });
    */



export default angular.module('won.owner.stores.drafts', [
        'flux'
    ])
    //.directive('wonImageDropzone', genComponentConf)
    .store('DraftStore', genStoreConf)
    .name;
