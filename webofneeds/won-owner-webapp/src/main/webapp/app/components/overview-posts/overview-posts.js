;
import Immutable from 'immutable';
import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import postItemLineModule from '../post-item-line';
import { actionCreators }  from '../../actions/actions';
import {
    attach,
    reverseSearchNominatim,
    nominatim2draftLocation,
} from '../../utils.js';
import {
    selectAllOwnNeeds,
} from '../../selectors';

import {
    resetParams,
} from '../../configRouting';
import won from '../../won-es6';

const ZERO_UNSEEN = Object.freeze({
    matches: 0,
    incomingRequests: 0,
    conversations: 0,
});

const serviceDependencies = ['$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];
class OverviewPostsController {

    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 1;
        window.ovp4dbg = this;
        this.resetParams = resetParams;
        this.activePostsOpen = true;
        this.closedPostsOpen = false;

        const selectFromState = (state) => {
            const ownNeeds = selectAllOwnNeeds(state);

            let activePosts = ownNeeds.filter(post =>
                post.get("state") === won.WON.ActiveCompacted
            );
            activePosts = activePosts? activePosts.toArray() : [];

            let inactivePosts = ownNeeds.filter(post =>
                post.get("state") === won.WON.InactiveCompacted
            );
            inactivePosts = inactivePosts? inactivePosts.toArray() : [];

            return {
                activePostsUris: activePosts.map(p => p.get('uri')),
                activePostsCount: activePosts.length,
                inactivePostsUris: inactivePosts.map(p => p.get('uri')),
                inactivePostsCount: inactivePosts.length,
            }
        };

        //this.createWhatsAround();

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }

    createWhatsAround(){
        console.log("Create Whats Around");

        if ("geolocation" in navigator) {
            navigator.geolocation.getCurrentPosition(
                currentLocation => {
                   console.log(currentLocation);
                    const lat = currentLocation.coords.latitude;
                    const lng = currentLocation.coords.longitude;
                    const zoom = 13; // TODO use `currentLocation.coords.accuracy` to control coarseness of query / zoom-level

                    const degreeConstant = 1.0;

                    // center map around current location

                    reverseSearchNominatim(lat, lng, zoom)
                        .then(searchResult => {
                            const location = nominatim2draftLocation(searchResult);

                            let whatsAround = {
                                title: "What's Around?",
                                type: "http://purl.org/webofneeds/model#DoTogether",
                                description: "Automatically created Need to see what's in your location",
                                tags: undefined,
                                location: location,
                                thumbnail: undefined,
                                whatsAround: true
                            };

                            console.log("Creating Whats around with data: ", whatsAround);

                            this.needs__create(
                                whatsAround,
                                this.$ngRedux.getState().getIn(['config', 'defaultNodeUri'])
                            );
                        });
                },
                err => { //error handler
                    if(err.code === 2 ) {
                        console.log("create whats around not possible due to error")
                    }
                },
                { //options
                    enableHighAccuracy: true,
                    timeout: 5000,
                    maximumAge: 0
                }
            );
        }
    }

}

OverviewPostsController.$inject = [];

export default angular.module('won.owner.components.overviewPosts', [
        overviewTitleBarModule,
        postItemLineModule
    ])
    .controller('OverviewPostsController',[...serviceDependencies,OverviewPostsController] )

    .name;


