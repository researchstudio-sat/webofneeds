/**
 * Created by ksinger on 10.05.2016.
 */


;

import angular from 'angular';
import inviewModule from 'angular-inview';

import { attach, } from '../utils';
import won from '../won-es6';
import {
    relativeTime,
} from '../won-label-utils';
import {
    selectOpenPostUri,
    selectLastUpdateTime,
} from '../selectors';
import { actionCreators }  from '../actions/actions';
import L from '../leaflet-bundleable';
import {
    initLeaflet,
} from '../won-utils';

import {
    DomCache,
} from '../cstm-ng-utils';

const serviceDependencies = ['$ngRedux', '$scope', '$element'];
function genComponentConf() {
    let template = `
        <div class="post-info__inner">
            <won-gallery
                class="post-info__inner__left"
                ng-show="self.post.get('hasImages')">
            </won-gallery>

            <div class="post-info__inner__right">
                <h2 class="post-info__heading" ng-show="self.friendlyTimestamp">
                    Created
                </h2>
                <p class="post-info__details" ng-show="self.friendlyTimestamp">
                    {{ self.friendlyTimestamp }}
                </p>

                <h2 class="post-info__heading"
                    ng-show="self.post.get('description')">
                    Description
                </h2>
                <p class="post-info__details"
                    ng-show="self.post.get('description')">
                    {{ self.post.get('description')}}
                </p>

                <h2 class="post-info__heading"
                    ng-show="self.post.get('tags')">
                    Tags
                </h2>
                <div class="post-info__details post-info__tags"
                    ng-show="self.post.get('tags')">
                        <span class="post-info__tags__tag" ng-repeat="tag in self.post.get('tags').toJS()">{{tag}}</span>
                </div>

                <h2 class="post-info__heading"
                    ng-show="self.location">
                    Location
                </h2>
                <p class="post-info__details"
                    ng-show="self.address">
                    {{ self.address }}
                    <a href="" ng-click="self.updateMap(self.location)">
                        (show)
                    </a>
                </p>
                <p ng-show="self.debugmode">
                    <a class="debuglink" target="_blank" href="{{self.post.get('uri')}}">[DATA]</a>
                </p>
                <div class="post-info__mapmount"
                     id="post-info__mapmount"
                     in-view="$inview && self.mapInView($inviewInfo)"
                     ng-show="self.location">
                 </div>
            </div>
        </div>
    `;


    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.domCache = new DomCache(this.$element);

            window.pi4dbg = this;

            this.map = initLeaflet(this.mapMount());
            // this.determineCurrentLocation(); show in reference


            //TODO custom icons
            //TODO for the bounding box: make sure location-bb as well as own location-bb fit into the view.
            //TODO use different marker for own location
            //TODO need to resize page for map to render correctly

            this.$scope.$watch(
                'self.location',
                (location, previousLocationValue) => {
                    console.log('in location watch: ', location, previousLocationValue);
                    if(location && !this._mapHasBeenAutoCentered) {
                        this.updateMap(location);
                        this._mapHasBeenAutoCentered = true;
                    }
                }
            );


            const selectFromState = (state) => {
                const postUri = selectOpenPostUri(state);
                const post = state.getIn(["needs", postUri]);
                const location = post && post.get('location');
                return {
                    post,
                    location: location,
                    address: location && location.get('address'),
                    friendlyTimestamp: post && relativeTime(
                        selectLastUpdateTime(state),
                        post.get('creationDate')
                    ),
                    debugmode: won.debugmode
                }
            };
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        mapInView(inviewInfo) {
            if(inviewInfo.changed) {
                this.map.invalidateSize();
            }
        }

        updateMap(location) {
            if(!location) {
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

        mapMountNg() { return this.domCache.ng('.post-info__mapmount'); }
        mapMount() { return this.domCache.dom('.post-info__mapmount'); }
}
Controller.$inject = serviceDependencies;
return {
    restrict: 'E',
    controller: Controller,
    controllerAs: 'self',
    bindToController: true, //scope-bindings -> ctrl
    template: template,
    scope: { }
}
}

export default angular.module('won.owner.components.postInfo', [ inviewModule.name ])
    .directive('wonPostInfo', genComponentConf)
    .name;
