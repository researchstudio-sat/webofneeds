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
                attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
            });

            this.map = L.map('lp__mapmount',{
                center: [51.505, -0.09],
                zoom: 13,
                layers: [secureOsm], //layers enabled from beginning on

            }); //.setView([51.505, -0.09], 13);

            const baseMaps = {
                "Default": secureOsm,
                "Transport (Insecure)": transport,
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
            console.log('TODO starting type-ahead search for: ' + this.textfield().value);
            //buffer for 1s before starting the search

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

export default angular.module('won.owner.components.locationPicker', [
    ])
    .directive('wonLocationPicker', genComponentConf)
    .name;


window.searchNominatim4dbg = searchNominatim;