/**
 * Created by ksinger on 15.07.2016.
 */

import won from '../won-es6';
import angular from 'angular';
import Immutable from 'immutable'; // also exports itself as (window).L
import L from '../leaflet-bundleable';
import 'ng-redux';
//import { labels } from '../won-label-utils';
import {
    attach,
    searchNominatim,
    reverseSearchNominatim,
    clone,
} from '../utils.js';
import { actionCreators }  from '../actions/actions';
import { selectOpenDraft, selectRouterParams } from '../selectors';
import {
    doneTypingBufferNg,
    DomCache,
} from '../cstm-ng-utils'

import {
    initLeaflet,
} from '../won-utils';

const serviceDependencies = ['$scope', '$ngRedux', '$element'];
function genComponentConf() {
    let template = `
        <input type="text" class="lp__searchbox" placeholder="Search for location"/>
        <span class="lp__verifiedLocation" ng-show="self.locationIsSaved()">[CHECK]</span>
        <ol>
            <li ng-show="self.currentLocation && !self.searchResults && !self.locationIsSaved()">
                <a href="" ng-click="self.selectedLocation(self.currentLocation)">
                    {{ self.currentLocation.name }}
                </a>
                (current)
            </li>
            <li ng-repeat="result in self.searchResults">
                <a href="" ng-click="self.selectedLocation(result)">
                    {{ result.name }}
                </a>
            </li>
        </ol>
        <div class="lp__mapmount" id="lp__mapmount" style="height:500px"></div>
            `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.domCache = new DomCache(this.$element);

            this.map = initLeaflet(this.mapMount());
            this.map.on('click', e => onMapClick(e, this));
            this.determineCurrentLocation();

            window.lp4dbg = this;
            const selectFromState = state => {
                const openDraft = selectOpenDraft(state);
                const routerParams = selectRouterParams(state);
                return {
                    savedName: openDraft && openDraft.getIn(['location', 'name']),
                    draftId: routerParams.get('draftId'),

                    openDraft4dbg: openDraft,
                }
            };


            doneTypingBufferNg(
                e => this.doneTyping(e),
                this.textfieldNg(), 1000
            );

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

        }
        locationIsSaved() {
            return this.savedName &&
                this.textfield().value === this.savedName;
        }
        placeMarkers(locations) {
            if(this.markers) {
                //remove previously placed markers
                for(let m of this.markers) {
                    this.map.removeLayer(m);
                }
            }

            this.markers = locations.map(location =>
                L.marker([location.lat, location.lon])
                .bindPopup(location.name)
            );

            for(let m of this.markers) {
                this.map.addLayer(m);
            }
        }
        resetSearchResults() {
            this.searchResults = undefined;
            this.placeMarkers([]);
        }
        selectedLocation(location) {
            this.resetSearchResults(); // picked one, can hide the rest if they were there

            //TODO ? let locationWithOrigName = clone(location);
            //TODO ? locationWithOrigName.name = this.textfield().value;

            this.drafts__change__location({
                draftId: this.draftId,
                location
            });

            this.textfield().value = location.name; // use textfield to display result

            this.placeMarkers([location]);
            this.map.fitBounds(location.bounds, { animate: true });
            this.markers[0].openPopup();
        }
        doneTyping() {
            const text = this.textfield().value;
            console.log('starting type-ahead search for: ' + text);

            if(!text) {
                this.$scope.$apply(() => { this.resetSearchResults(); });
            } else {
                searchNominatim(text).then( searchResults => {
                    console.log('location search results: ', searchResults);
                    this.$scope.$apply(() => {
                        this.searchResults = scrubSearchResults(searchResults);
                    });
                    this.placeMarkers(searchResults);
                });
            }
        }
        determineCurrentLocation() {
            if ("geolocation" in navigator) {
                navigator.geolocation.getCurrentPosition(
                    currentLocation => {

                        console.log(currentLocation)
                        const lat = currentLocation.coords.latitude;
                        const lon = currentLocation.coords.longitude;
                        const zoom = 13; // TODO use `currentLocation.coords.accuracy` to control coarseness of query / zoom-level

                        // center map around current location
                        this.map.setZoom(zoom);
                        this.map.panTo([lat, lon]);

                        reverseSearchNominatim(lat, lon, zoom)
                            .then(searchResult => {
                                const location = nominatim2wonLocation(searchResult);
                                console.log('current location: ', location);
                                this.$scope.$apply(() => { this.currentLocation = location });
                            });
                    },
                    err => { //error handler
                        if(err.code ===2 ) {
                            alert("Position is unavailable!"); //TODO toaster
                        }
                    },
                    { //options
                        enableHighAccuracy: true,
                        timeout: 5000,
                        maximumAge: 0
                    });

            }

        }

        textfieldNg() { return this.domCache.ng('.lp__searchbox'); }

        textfield() { return this.domCache.dom('.lp__searchbox'); }

        mapMountNg() { return this.domCache.ng('.lp__mapmount'); }

        mapMount() { return this.domCache.dom('.lp__mapmount'); }
    }
    Controller.$inject = serviceDependencies;


    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
        },
        template: template
    }
}


function scrubSearchResults(searchResults) {

    return Immutable.fromJS(
            searchResults.map(nominatim2wonLocation)
        )
        /*
         * filter "duplicate" results (e.g. "Wien"
         *  -> 1x waterway, 1x boundary, 1x place)
         */
        .groupBy(r => r.get('name'))
        .map(sameNamedResults => sameNamedResults.first())
        .toList()
        .toJS()
}

/**
 * drop info not stored in rdf, thus info that we
 * couldn't restore for previously used locations
 */
function nominatim2wonLocation(searchResult) {
    const b = searchResult.boundingbox;
    return {
        name: searchResult.display_name,
        lon: Number.parseFloat(searchResult.lon),
        lat: Number.parseFloat(searchResult.lat),
        //importance: searchResult.importance,
        bounds: [
            [ Number.parseFloat(b[0]), Number.parseFloat(b[2]) ], //north-western point
            [ Number.parseFloat(b[1]), Number.parseFloat(b[3]) ] //south-eastern point
        ],
    }
}

function onMapClick(e, ctrl) {
    //`this` is the mapcontainer here as leaflet
    // apparently binds itself to the function.
    // This code was moved out of the controller
    // here to avoid confusion resulting from
    // this binding.
    console.log('clicked map ', e);
    reverseSearchNominatim(
        e.latlng.lat,
        e.latlng.lng,
        ctrl.map.getZoom()// - 1
    ).then(searchResult => {
        console.log('nearest address: ', searchResult);
        const location = nominatim2wonLocation(searchResult);

        //use coords of original click though (to allow more detailed control)
        location.lat = e.latlng.lat;
        location.lon = e.latlng.lng;
        ctrl.$scope.$apply(() => {
            ctrl.selectedLocation(location);
        })
    });
}

export default angular.module('won.owner.components.locationPicker', [
    ])
    .directive('wonLocationPicker', genComponentConf)
    .name;




window.searchNominatim4dbg = searchNominatim;
window.reverseSearchNominatim4dbg = reverseSearchNominatim;
window.nominatim2wonLocation4dbg = nominatim2wonLocation;
