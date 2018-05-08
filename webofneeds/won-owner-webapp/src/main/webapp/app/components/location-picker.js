/**
 * Created by ksinger on 15.07.2016.
 */

import angular from 'angular';
import Immutable from 'immutable'; // also exports itself as (window).L
import L from '../leaflet-bundleable.js';
import {
    attach,
    searchNominatim,
    reverseSearchNominatim,
    nominatim2draftLocation,
    leafletBounds,
    delay,
} from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import {
    doneTypingBufferNg,
    DomCache,
} from '../cstm-ng-utils.js'

import {
    initLeaflet,
} from '../won-utils.js';

const serviceDependencies = ['$scope', '$element', '$sce'];
function genComponentConf() {
    let template = `
        <!-- LOCATION SEARCH BOX -->
        <input type="text" class="lp__searchbox" placeholder="Search for location"/>

        <!-- SELECTED LOCATION -->
        <div class="lp__selected" ng-if="self.locationIsSaved">
            <svg class="lp__selected__icon clickable" 
                 style="--local-primary:var(--won-primary-color);"
                 ng-click="self.resetLocation()">
                    <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
            <span> {{self.pickedLocation.name}} </span>
        </div>

        <!-- LIST OF SUGGESTED LOCATIONS -->
        <ol>
            <li ng-if="
                !self.locationIsSaved && self.currentLocation &&
                (
                    !self.lastSearchedFor ||
                    ([self.currentLocation] | filter:self.lastSearchedFor).length > 0
                )
            ">
                <a href=""
                    ng-click="self.selectedLocation(self.currentLocation)"
                    ng-bind-html="self.highlight(self.currentLocation.name, self.lastSearchedFor)">
                </a>
                (current)
            </li>
            <!--li ng-if="!self.locationIsSaved"
                ng-repeat="previousLocation in self.previousLocations | filter:self.lastSearchedFor">
                    <a href=""
                        ng-click="self.selectedLocation(previousLocation)"
                        ng-bind-html="self.highlight(previousLocation.name, self.lastSearchedFor)">
                    </a>
                    (previous)
            </li-->
            <li ng-repeat="result in self.searchResults">
                <a href=""
                    ng-click="self.selectedLocation(result)"
                    ng-bind-html="self.highlight(result.name, self.lastSearchedFor)">
                </a>
            </li>
        </ol>
        <div class="lp__mapmount" id="lp__mapmount"></div>
            `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.domCache = new DomCache(this.$element);

            this.map = initLeaflet(this.mapMount());
            this.map.on('click', e => onMapClick(e, this));

            this.locationIsSaved = !!this.initialLocation;
            this.pickedLocation = this.initialLocation;
            
            window.lp4dbg = this;

            // needs to happen after constructor finishes, otherwise
            // the component's callbacks won't be registered.
           	delay(0).then(() => this.determineCurrentLocation());
            

            doneTypingBufferNg(
                e => this.doneTyping(e),
                this.textfieldNg(), 1000
            );
        }

        /**
         * Taken from <http://stackoverflow.com/questions/15519713/highlighting-a-filtered-result-in-angularjs>
         * @param text
         * @param search
         * @return {*}
         */
        highlight(text, search) {
            if(!text) {
                text = "";
            }
            if(!search) {
                return this.$sce.trustAsHtml(text);
            }
            return this.$sce.trustAsHtml(
                text.replace(
                    new RegExp(search, 'gi'),
                    '<span class="highlightedText">$&</span>'
                )
            );
        }

        placeMarkers(locations) {
            
            this.removeMarkers();

            this.markers = locations.map(location =>
                L.marker([location.lat, location.lng])
                .bindPopup(location.name)
            );

            for(let m of this.markers) {
                this.map.addLayer(m);
            }
        }

        removeMarkers() {
            if(this.markers) {
                //remove previously placed markers
                for(let m of this.markers) {
                    this.map.removeLayer(m);
                }
            }
        }

        resetSearchResults() {
            this.searchResults = undefined;
            this.lastSearchedFor = undefined;
            this.placeMarkers([]);
        }

        resetLocation() {
            this.locationIsSaved = false;
            this.pickedLocation = undefined;
            this.removeMarkers();
            
            this.onLocationPicked({location: undefined});
        }

        selectedLocation(location) {

            // callback to update location in isseeks
            this.onLocationPicked({location: location});
            this.locationIsSaved = true;
            this.pickedLocation = location;

            this.resetSearchResults(); // picked one, can hide the rest if they were there

            this.placeMarkers([location]);
            this.map.fitBounds(leafletBounds(location), { animate: true });
            this.markers[0].openPopup();
        }

        doneTyping() {
            const text = this.textfield().value;

            if(!text) {
                this.$scope.$apply(() => { this.resetSearchResults(); });
            } else {
                searchNominatim(text).then( searchResults => {
                    const parsedResults = scrubSearchResults(searchResults, text);
                    this.$scope.$apply(() => {
                        this.searchResults = parsedResults;
                        //this.lastSearchedFor = { name: text };
                        this.lastSearchedFor = text;
                    });
                    this.placeMarkers(Object.values(parsedResults));
                });
            }
        }

        determineCurrentLocation() {
            // if a location is saved, zoom in on saved location
            if(this.initialLocation) {

                // constructor may not be done in time, so set values here again.
                this.locationIsSaved = true;
                this.pickedLocation = this.initialLocation;

                const lat = this.pickedLocation.lat;
                const lng = this.pickedLocation.lng;
                const zoom = 13;

                this.map.setZoom(zoom);
                this.map.panTo([lat, lng]);

                //this.textfield().value = this.pickedLocation.name;
                this.placeMarkers([this.pickedLocation]);
            }
            // else, try to zoom in on current location
            else if ("geolocation" in navigator) {
                navigator.geolocation.getCurrentPosition(
                    currentLocation => {

                        const lat = currentLocation.coords.latitude;
                        const lng = currentLocation.coords.longitude;
                        const zoom = 13; // TODO use `currentLocation.coords.accuracy` to control coarseness of query / zoom-level

                        // center map around current location
                        this.map.setZoom(zoom);
                        this.map.panTo([lat, lng]);

                        reverseSearchNominatim(lat, lng, zoom)
                            .then(searchResult => {
                                const location = nominatim2draftLocation(searchResult);
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

            this.$scope.$apply();

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
            onLocationPicked: "&",
            initialLocation: "=",
        },
        template: template
    }
}


function scrubSearchResults(searchResults) {

    return Immutable.fromJS(
            searchResults.map(nominatim2draftLocation)
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

function jsonLd2draftLocation(location) {
    // payload uses the json-ld format
    const nw = location.getIn(['won:hasBoundingBox', 'won:hasNorthWestCorner']);
    const se = location.getIn(['won:hasBoundingBox', 'won:hasSouthEastCorner']);
    return  {
        name: location.get('s:name'),
        lon: Number.parseFloat(location.getIn(['s:geo', 's:longitude'])),
        lat: Number.parseFloat(location.getIn(['s:geo', 's:latitude'])),
        //importance: searchResult.importance,
        nwCorner: {
            lat: Number.parseFloat(nw.get('s:latitude')),
            lng: Number.parseFloat(nw.get('s:longitude')),
        },
        seCorner: {
            lat: Number.parseFloat(se.get('s:latitude')),
            lng: Number.parseFloat(se.get('s:longitude')),
        },
        //bounds: [
        //    [
        //        Number.parseFloat(nw.get('s:latitude')),
        //        Number.parseFloat(nw.get('s:longitude')),
        //    ],
        //    [
        //        Number.parseFloat(se.get('s:latitude')),
        //        Number.parseFloat(se.get('s:longitude')),
        //    ]
        //]
    }
}

function onMapClick(e, ctrl) {
    //`this` is the mapcontainer here as leaflet
    // apparently binds itself to the function.
    // This code was moved out of the controller
    // here to avoid confusion resulting from
    // this binding.
    reverseSearchNominatim(
        e.latlng.lat,
        e.latlng.lng,
        ctrl.map.getZoom()// - 1
    ).then(searchResult => {
        const location = nominatim2draftLocation(searchResult);

        //use coords of original click though (to allow more detailed control)
        location.lat = e.latlng.lat;
        location.lng = e.latlng.lng;
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
window.nominatim2wonLocation4dbg = nominatim2draftLocation;
