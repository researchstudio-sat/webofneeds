/**
 * Created by ksinger on 15.07.2016.
 */

import won from '../won-es6';
import angular from 'angular';
import Immutable from 'immutable'; // also exports itself as (window).L
import 'leaflet';
import 'ng-redux';
//import { labels } from '../won-label-utils';
import { attach, searchNominatim } from '../utils.js';
import { actionCreators }  from '../actions/actions';
import { } from '../selectors';
import { doneTypingBufferNg } from '../cstm-ng-utils'

const serviceDependencies = ['$scope', '$ngRedux', '$element'];
function genComponentConf() {
    //TODO input as text-input or contenteditable? need to overl
    let template = `


        <input type="text" class="lp__searchbox" placeholder="Search for location"/>
        <ol>
            <li ng-repeat="result in self.searchResults">
                <a href="" ng-click="self.clickedSearchResult(result)">
                    {{ result.name }}
                </a>
            </li>
        </ol>
        <div class="lp__mapmount" id="lp__mapmount" style="height:500px"></div>
        <!--<img class="lp__mapmount" src="images/some_map_screenshot.png"alt=""/>-->
            `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            this.initMap();

            window.lp4dbg = this;
            const selectFromState = (state)=>{
                return {
                };
            }

            doneTypingBufferNg(
                e => this.doneTyping(e),
                this.textfieldNg(), 1000
            )

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

        }
        initMap() {
            // Leaflet + JS-Bundling fix:
            L.Icon.Default.imagePath = 'images/map-images/';
            //TODO replace with own icons

            const secureOsmSource = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png' // secure osm.org
            const secureOsm = L.tileLayer(secureOsmSource, {
                attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
            });

            const transportSource = 'http://{s}.tile2.opencyclemap.org/transport/{z}/{x}/{y}.png';
            const transport = L.tileLayer(transportSource, {
                attribution: 'Maps &copy; <a href="http://www.thunderforest.com">Thunderforest</a>, Data &copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
            });

            this.map = L.map(this.mapMount(),{
                center: [37.44, -42.89], //centered on north-west africa
                zoom: 1, //world-map
                layers: [secureOsm], //initially visible layers

            }); //.setView([51.505, -0.09], 13);

            //this.map.fitWorld() // shows every continent twice :|
            this.map.fitBounds([[-80, -190],[80, 190]]); // fitWorld without repetition

            const baseMaps = {
                "Detailed default map": secureOsm,
                "Transport (Insecure!)": transport,
            }

            L.control.layers(baseMaps).addTo(this.map);

            L.marker([51.5, -0.09]).addTo(this.map)
                .bindPopup('A pretty CSS3 popup.<br> Easily customizable.')
                .openPopup();

            // Force it to adapt to actual size
            // for some reason this doesn't happen by default
            // when the map is within a tag.
            // this.map.invalidateSize();
            // ^ doesn't work (needs to be done manually atm);

        }
        doneTyping() {
            console.log('starting type-ahead search for: ' + this.textfield().value);
            //buffer for 1s before starting the search
            searchNominatim(this.textfield().value)
            .then( searchResults => {
                console.log('location search results: ', searchResults);
                this.$scope.$apply(() =>
                    //this.searchResults = searchResults.map(nominatim2wonLocation)

                    this.searchResults = scrubSearchResults(searchResults)
                )
            })

        }
        clickedSearchResult(location) {
            console.log('selected location: ', location)

        }

        textfieldNg() {
            if(!this._textfield) {
                this._textfield = this.$element.find('.lp__searchbox');
            }
            return this._textfield;
        }

        textfield() {
            return this.textfieldNg()[0];
        }

        mapMountNg() {
            if(!this._mapMount) {
                this._mapMount = this.$element.find('.lp__mapmount');
            }
            return this._mapMount;
        }

        mapMount() {
            return this.mapMountNg()[0];
        }
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
    return {
        name: searchResult.display_name,
        lon: searchResult.lon,
        lat: searchResult.lat,
        //importance: searchResult.importance,
        boundingbox: searchResult.boundingbox, // TODO use this to set proper zoom
    }
}

export default angular.module('won.owner.components.locationPicker', [
    ])
    .directive('wonLocationPicker', genComponentConf)
    .name;


window.searchNominatim4dbg = searchNominatim;