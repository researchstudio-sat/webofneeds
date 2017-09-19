/**
 * Created by fsuda on 21.08.2017.
 */
import angular from 'angular';
import inviewModule from 'angular-inview';

import { attach, decodeUriComponentProperly} from '../utils.js';
import won from '../won-es6.js';
import {
    selectOpenPostUri,
    displayingOverview,
    selectAllConnections,
} from '../selectors.js';
import { actionCreators }  from '../actions/actions.js';
import L from '../leaflet-bundleable.js';
import {
    initLeaflet,
} from '../won-utils.js';

import {
    DomCache,
} from '../cstm-ng-utils.js';

const serviceDependencies = ['$ngRedux', '$scope', '$element'];
function genComponentConf() {
    let template = `
        <div class="connections-map__mapmount"            
             id="connections-map__mapmount"
             in-view="$inview && self.mapInView($inviewInfo)"
             ng-show="self.location || self.connections">
        </div>
    `;


    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.domCache = new DomCache(this.$element);

            this.map = initLeaflet(this.mapMount());
            window.connMap4dbg = this;

            this.markers = [];

            this.$scope.$watch(
                'self.postLocation',
                (newValue, oldValue) => {
                    if(newValue) {
                        this.updateMap(this.postLocation, this.connections, this.needs);
                        this._mapHasBeenAutoCentered = true;
                    }
                }
            );

            this.$scope.$watch(
                'self.needs',
                (newValue, oldValue) => {
                    if(newValue) {
                        this.updateMap(this.postLocation, this.connections, this.needs);
                        this._mapHasBeenAutoCentered = true;
                    }
                }
            );

            this.$scope.$watch(
                'self.connections',
                (newValue, oldValue) => {
                    if(newValue) {
                        this.updateMap(this.postLocation, this.connections, this.needs);
                        this._mapHasBeenAutoCentered = true;
                    }
                }
            );

            const selectFromState = (state) => {
                const connectionTypeInParams = decodeUriComponentProperly(
                    state.getIn(['router', 'currentParams', 'connectionType'])
                );

                const isOverview = displayingOverview(state);

                const postUri = selectOpenPostUri(state);
                const post = postUri && state.getIn(["needs", postUri]);
                const isWhatsAround = post && post.get("isWhatsAround");
                const postLocation = post && post.get('location');
                const connectionType = connectionTypeInParams || this.connectionType;
                const connections = isOverview ?
                                        selectAllConnections(state).filter(conn => conn.get("state") === connectionType) :
                                        post && post.get("connections").filter(conn => conn.get("state") === connectionType);

                return {
                    post: post,
                    isWhatsAround: isWhatsAround,
                    needs: state.get("needs"),
                    postLocation: postLocation,
                    connections: connections,
                    address: postLocation && postLocation.get('address'),
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

        updateMap(postLocation, connections, needs) {
            console.log("Call an update on the map for location: ", postLocation, "connections: ", connections, "needs: ", needs);
            this.markers.forEach(marker => this.map.removeLayer(marker)); //Remove all existing markers
            this.markers = []; //RESET MARKERS

            if(postLocation){
                this.markers.push(L.marker([postLocation.get("lat"), postLocation.get("lng")]).bindPopup("Your need - " + postLocation.get("address")));
            }

            if(connections && connections.size > 0){
                console.log("Setting markers for connections");
                connections.map(function(conn){
                    console.log("Looking Up remoteNeed with Uri: ",conn.get("remoteNeedUri")," with data ", needs && needs.get(conn.get("remoteNeedUri")));
                    let need = needs && needs.get(conn.get("remoteNeedUri"));
                    let connLocation = needs && needs.getIn([conn.get("remoteNeedUri"), "location"]);
                    if(need && need.get("location")) {
                        console.log("setting marker for connectionLocation: ",connLocation.toJS());
                        this.markers.push(this.createUniqueMarker(need, conn));
                    }else{
                        console.log("no marker set because connection Need does not have a location");
                    }
                }, this);
            }

            this.markers.forEach(marker => this.map.addLayer(marker));

            if(this.markers.length > 0){
                var markerGroup = new L.featureGroup(this.markers);
                this.map.fitBounds(markerGroup.getBounds());
                this.mapAlreadyInitialized = true;
            }
        }

        createUniqueMarker(need, conn) {
            let lat = need.getIn(["location", "lat"]);
            let lng = need.getIn(["location", "lng"]);

            this.markers.forEach(function(marker){
                const presentLat = marker.getLatLng().lat;
                const presentLng = marker.getLatLng().lng;

                if(lat == presentLat && lng == presentLng){
                    lat += 0.000010;
                    lng -= 0.000010;
                }
            },this);

            return L.marker([lat, lng])
                .bindPopup(need.get("title" + " - " + need.getIn(["location", "address"])))
                .on("click",
                    function() {
                        if(false && this.isWhatsAround){
                            this.router__stateGoAbs('post', {postUri: need.get("uri")});
                        }else{
                            this.onSelectedConnection({connectionUri: conn.get("uri")})
                        }
                    },
                    this
                );
        }

        mapMountNg() { return this.domCache.ng('.connections-map__mapmount'); }
        mapMount() { return this.domCache.dom('.connections-map__mapmount'); }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        template: template,
        scope: {
            connectionType: "=",
            onSelectedConnection: "&"
        }
    }
}

export default angular.module('won.owner.components.connectionsMapModule', [ inviewModule.name ])
    .directive('wonConnectionsMap', genComponentConf)
    .name;