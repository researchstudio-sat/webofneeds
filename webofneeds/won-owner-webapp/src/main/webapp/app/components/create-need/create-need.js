/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular'
import 'ng-redux';
import createNeedTitleBarModule from '../create-need-title-bar';
import posttypeSelectModule from '../posttype-select';
import labelledHrModule from '../labelled-hr';
import needTextfieldModule from '../need-textfield';
import imageDropzoneModule from '../image-dropzone';
import locationPickerModule from '../location-picker';
import {
    attach,
    reverseSearchNominatim,
    nominatim2draftLocation,
} from '../../utils';
import { actionCreators }  from '../../actions/actions';
import won from '../../won-es6';

const postTypeTexts = [
    {
        type: won.WON.BasicNeedTypeDemand,
        text: 'I\'m looking for',
        helpText: 'Select this if you are looking for things or services other people offer',
    },
    {
        type: won.WON.BasicNeedTypeSupply,
        text: 'I\'m offering',
        helpText: 'Use this if you are offering an item or a service. You will find people who said' +
        ' that they\'re looking for something.'
    },
    {
        type: won.WON.BasicNeedTypeDotogether,
        text: 'I want to find someone to',
        helpText: 'Select this if you are looking to find other people who share your interest. You will be matched' +
        ' with other people who chose this option as well.'
    },
];

//TODO can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = ['$ngRedux', '$scope'/*'$routeParams' /*injections as strings here*/];

class CreateNeedController {
    constructor(/* arguments <- serviceDependencies */) {
        attach(this, serviceDependencies, arguments);

        this.postTypeTexts = postTypeTexts;
        this.characterLimit = 140; //TODO move to conf
        this.draft = {title: "", type: undefined, description: "", tags: undefined, location: undefined, thumbnail: undefined};
        this.pendingPublishing = false;
        //TODO debug; deleteme
        window.cnc = this;

        const selectFromState = (state) => {
            return {}
        };

        // Using actionCreators like this means that every action defined there is available in the template.
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }
    isValid(){
        return this.draft && this.draft.type && this.draft.title && this.draft.title.length < this.characterLimit;
    }

    selectType(typeIdx) {
        console.log('selected type ', postTypeTexts[typeIdx].type);
        this.draft.type = postTypeTexts[typeIdx].type;
    }
    unselectType() {
        console.log('unselected type ');
        this.draft.type = undefined;
    }
    titlePicZoneNg() {
        return angular.element(this.titlePicZone())
    }
    titlePicZone() {
        if(!this._titlePicZone) {
            this._titlePicZone = this.$element[0].querySelector('#titlePic');
        }
        return this._titlePicZone;
    }

    publish() {
        if (!this.pendingPublishing) {
            this.pendingPublishing = true;
            this.needs__create(
                this.draft,
                this.$ngRedux.getState().getIn(['config', 'defaultNodeUri'])
            );
        }
    }

    setDraft(updatedDraft) {
        Object.assign(this.draft, updatedDraft);
    }

    locationIsSaved() {
        return this.draft.location && this.draft.location.name;
    }

    pickImage(image) {
        this.draft.thumbnail = image;
    }

    createWhatsAround(){
        if (!this.pendingPublishing) {
            this.pendingPublishing = true;
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
                                    description: "Automatically created Need to see what's happening in your Area",
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
                        if (err.code === 2) {
                            console.log("create whats around not possible due to error");
                            //TODO: SHOW TOAST FOR ERROR
                            this.pendingPublishing = false;
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
}

export default angular.module('won.owner.components.createNeed', [
        createNeedTitleBarModule,
        posttypeSelectModule,
        labelledHrModule,
        imageDropzoneModule,
        needTextfieldModule,
        locationPickerModule,
    ])
    .controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
    .name;
