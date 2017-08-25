/**
 * Created by fsuda on 21.08.2017.
 */
import angular from 'angular';
import inviewModule from 'angular-inview';

import { attach, decodeUriComponentProperly} from '../utils';
import won from '../won-es6';
import {
    selectOpenPostUri,
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
        <div class="connections-map__inner">
            <div class="connections-map__mapmount"            
                 id="connections-map__mapmount"
                 in-view="$inview && self.mapInView($inviewInfo)"
                 ng-show="self.location || self.connections">
            </div>
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
                'self.location',
                (newValue, oldValue) => {
                    if(newValue) {
                        this.updateMap(this.location, this.connections, this.needs);
                        this._mapHasBeenAutoCentered = true;
                    }
                }
            );

            this.$scope.$watch(
                'self.needs',
                (newValue, oldValue) => {
                    if(newValue) {
                        this.updateMap(this.location, this.connections, this.needs);
                        this._mapHasBeenAutoCentered = true;
                    }
                }
            );

            this.$scope.$watch(
                'self.connections',
                (newValue, oldValue) => {
                    if(newValue) {
                        this.updateMap(this.location, this.connections, this.needs);
                        this._mapHasBeenAutoCentered = true;
                    }
                }
            );

            const selectFromState = (state) => {
                const connectionTypeInParams = decodeUriComponentProperly(
                    state.getIn(['router', 'currentParams', 'connectionType'])
                );
                const postUri = selectOpenPostUri(state);
                const post = postUri && state.getIn(["needs", postUri]);
                const isWhatsAround = post && post.get("isWhatsAround");
                const location = post && post.get('location');
                const connectionType = connectionTypeInParams || self.connectionType;
                return {
                    post: post,
                    isWhatsAround: isWhatsAround,
                    needs: state.get("needs"),
                    location: location,
                    connections: post && post.get("connections").filter(conn => conn.get("state") === connectionType),
                    address: location && location.get('address'),
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

        updateMap(location, connections, needs) {
            console.log("Call an update on the map for location: ", location, "connections: ", connections, "needs: ", needs);
            this.markers.forEach(marker => this.map.removeLayer(marker)); //Remove all existing markers
            this.markers = []; //RESET MARKERS

            if(location){
                this.markers.push(L.marker([location.get("lat"), location.get("lng")]).bindPopup("Your need - " + location.get("address")));
            }

            if(connections && connections.size > 0){
                console.log("Setting markers for connections");
                connections.map(function(conn){
                    console.log("Looking Up remoteNeed with Uri: ",conn.get("remoteNeedUri")," with data ", needs && needs.get(conn.get("remoteNeedUri")));
                    let need = needs && needs.get(conn.get("remoteNeedUri"));
                    let connLocation = needs && needs.getIn([conn.get("remoteNeedUri"), "location"]);
                    if(need && need.get("location")) {
                        console.log("setting marker for connectionLocation: ",connLocation.toJS());
                        this.markers.push(
                            L.marker([need.getIn(["location", "lat"]), need.getIn(["location", "lng"])])
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
                                )
                        );
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