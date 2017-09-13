/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular'
import 'ng-redux';
import createNeedTitleBarModule from './create-need-title-bar.js';
import posttypeSelectModule from './posttype-select.js';
import labelledHrModule from './labelled-hr.js';
import needTextfieldModule from './need-textfield.js';
import imageDropzoneModule from './image-dropzone.js';
import locationPickerModule from './location-picker.js';
import {
    attach,
    reverseSearchNominatim,
    nominatim2draftLocation,
} from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import won from '../won-es6.js';
import {
    connect2Redux,
} from '../won-utils.js';

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

function genComponentConf() {
    const template = `
        <div class="cp__inner">
            <button type="submit"
                    class="won-button--filled red"
                    ng-click="::self.createWhatsAround()">
                <span ng-show="!self.pendingPublishing">
                    See What's Around
                </span>
                <span ng-show="self.pendingPublishing">
                    Retrieving What's Around&nbsp;&hellip;
                </span>
            </button>
            <won-labelled-hr label="::'or create your own'"></won-labelled-hr>
            <won-posttype-select
                    options="::self.postTypeTexts"
                    on-select="::self.selectType(idx)"
                    on-unselect="::self.unselectType()">
            </won-posttype-select>

            <div class="cp__mandatory-rest" ng-show="self.draft.type">
                <won-image-dropzone
                        on-image-picked="::self.pickImage(image)">
                </won-image-dropzone>

                <need-textfield on-draft-change="::self.setDraft(draft)"></need-textfield>
            </div>

            <hr ng-show="self.draft.tags && self.draft.tags.length > 0"/>
            <div class="cp__tags" ng-show="self.draft.tags && self.draft.tags.length > 0">
                <div class="cp__header tags">Tags</div>
                <span class="cp__tags__tag" ng-repeat="tag in self.draft.tags">{{tag}}</span>
            </div>


            <hr ng-show="self.isValid()"/>
            <div class="cp__location" ng-show="self.isValid()">
                <div class="cp__header location">Location</div>
                <won-location-picker on-draft-change="::self.setDraft(draft)" location-is-saved="::self.locationIsSaved()"></won-location-picker>
            </div>

            <won-labelled-hr label="::'or'" ng-show="self.isValid()"></won-labelled-hr>

            <button type="submit" class="won-button--filled red"
                    ng-show="self.isValid()"
                    ng-click="::self.publish()">
                <span ng-show="!self.pendingPublishing">
                    Publish
                </span>
                <span ng-show="self.pendingPublishing">
                    Publishing&nbsp;&hellip;
                </span>
            </button>
        </div>
    `


    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);

            this.postTypeTexts = postTypeTexts;
            this.characterLimit = 140; //TODO move to conf
            this.draft = {title: "", type: undefined, description: "", tags: undefined, location: undefined, thumbnail: undefined};
            this.pendingPublishing = false;
            //TODO debug; deleteme
            window.cnc = this;

            const selectFromState = (state) => {
                return {
                    existingWhatsAroundNeeds: state.get("needs").filter(need => need.get("isWhatsAround")),
                }
            };

            // Using actionCreators like this means that every action defined there is available in the template.
            connect2Redux(selectFromState, actionCreators, [], this);
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

                                    this.existingWhatsAroundNeeds.map(
                                        need => this.needs__close(need.get("uri"))
                                    );

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
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {/*scope-isolation*/},
        template: template
    }
}

export default angular.module('won.owner.components.createPost', [
        createNeedTitleBarModule,
        posttypeSelectModule,
        labelledHrModule,
        imageDropzoneModule,
        needTextfieldModule,
        locationPickerModule,
    ])
    .directive('wonCreatePost', genComponentConf)
    //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
    .name;
