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
    selectOpenPost,
    selectLastUpdateTime,
} from '../selectors';
import { actionCreators }  from '../actions/actions';
import L from '../leaflet-bundleable';
import {
    initLeaflet,
    initLeafletBaseMaps,
    seeksOrIs,
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
                    ng-show="self.postContent.get('dc:description')">
                    Description
                </h2>
                <p class="post-info__details"
                    ng-show="self.postContent.get('dc:description')">
                    {{ self.postContent.get('dc:description')}}
                </p>

                <h2 class="post-info__heading"
                    ng-show="self.postContent.get('won:hasTag')">
                    Tags
                </h2>
                <div class="post-info__details post-info__tags"
                    ng-show="self.postContent.get('won:hasTag')">
                        <span class="post-info__tags__tag" ng-repeat="tag in self.postContent.get('won:hasTag').toJS()">{{tag}}</span>
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
                    <a class="debuglink" target="_blank" href="{{self.post.get('@id')}}">[DATA]</a>
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
                const post = selectOpenPost(state);
                const postContent = post && seeksOrIs(post);
                const location = postContent && postContent.get('won:hasLocation');
                return {
                    post,
                    postContent,
                    location: location,
                    address: location && location.get('s:name'),
                    friendlyTimestamp: relativeTime(
                        selectLastUpdateTime(state),
                        post && post.get('dct:created')
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

            const nw = location.getIn(['won:hasBoundingBox', 'won:hasNorthWestCorner']);
            const se = location.getIn(['won:hasBoundingBox', 'won:hasSouthEastCorner']);
            const lat = Number.parseFloat(location.getIn(['s:geo', 's:latitude']));
            const lon = Number.parseFloat(location.getIn(['s:geo', 's:longitude']));
            const name = location.get('s:name');

            if(!nw || !se || !lat || !lon || !name ) {
                return;
            }

            this.map.fitBounds([
                [
                    Number.parseFloat(nw.get('s:latitude')),
                    Number.parseFloat(nw.get('s:longitude')),
                ],
                [
                    Number.parseFloat(se.get('s:latitude')),
                    Number.parseFloat(se.get('s:longitude')),
                ]
            ]);

            if(this.marker) {
               this.map.removeLayer(this.marker);
            }
            this.marker = L.marker([lat, lon]).bindPopup(name);
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
