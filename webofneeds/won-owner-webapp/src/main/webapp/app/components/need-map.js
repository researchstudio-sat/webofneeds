/**
 * Created by fsuda on 21.08.2017.
 */
import angular from 'angular';
import inviewModule from 'angular-inview';

import { attach } from '../utils.js';
import won from '../won-es6.js';
import { actionCreators }  from '../actions/actions.js';
import L from '../leaflet-bundleable.js';
import {
    initLeaflet,
    connect2Redux,
} from '../won-utils.js';

import {
    DomCache,
} from '../cstm-ng-utils.js';

const serviceDependencies = ['$ngRedux', '$scope', '$element'];
function genComponentConf() {
    let template = `
        <div class="need-map__mapmount"
             in-view="$inview && self.mapInView($inviewInfo)">
        </div>
    `;


    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.domCache = new DomCache(this.$element);

            this.map = initLeaflet(this.mapMount());

            this.$scope.$watch(
                'self.location',
                (newLocation, oldLocation) => {
                    if(newLocation) {
                        this.updateMap(newLocation);
                        this._mapHasBeenAutoCentered = true;
                    }
                }
            );

            const selectFromState = (state) => {
                const post = this.uri && state.getIn(["needs", this.uri]);
                const location = post && post.get('location');
                return {
                    post: post,
                    location: location,
                }
            };
            connect2Redux(selectFromState, actionCreators, ['self.uri'], this);
        }

        mapInView(inviewInfo) {
            if(inviewInfo.changed) {
                this.map.invalidateSize();
            }
        }

        updateMap(location) {
            if(!location) {
                console.log("no marker set for location: ",location);
                return;
            }

            this.map.fitBounds([
                [
                    location.getIn(["nwCorner", "lat"]),
                    location.getIn(["nwCorner", "lng"])
                ],
                [
                    location.getIn(["seCorner", "lat"]),
                    location.getIn(["seCorner", "lng"])
                ]
            ]);

            if(this.marker) {
                this.map.removeLayer(this.marker);
            }
            this.marker = L.marker([location.get("lat"), location.get("lng")]).bindPopup(location.get("address"));
            this.map.addLayer(this.marker);

            this.mapAlreadyInitialized = true;
        }

        mapMountNg() {
            return this.domCache.ng('.need-map__mapmount');
        }
        mapMount() {
            return this.domCache.dom('.need-map__mapmount');
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        template: template,
        scope: {
            uri: "=",
        }
    }
}

export default angular.module('won.owner.components.needMapModule', [ inviewModule.name ])
    .directive('wonNeedMap', genComponentConf)
    .name;