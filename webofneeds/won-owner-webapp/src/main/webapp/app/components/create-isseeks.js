/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';

import 'ng-redux';
import won from '../won-es6.js';
import {
    postTitleCharacterLimit,
} from '../config.js';
import needTextfieldModule from './need-textfield.js';
import imageDropzoneModule from './image-dropzone.js';
import locationPickerModule from './location-picker.js';
import {
    getIn,
    attach,
    deepFreeze,
    clone,
    dispatchEvent,
    is,
    mergeAsSet,
    extractHashtags,
} from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import Immutable from 'immutable';
import {
    connect2Redux,
} from '../won-utils.js';


const emptyDraft = deepFreeze({
    title: "", 
    type: won.WON.BasicNeedTypeCombined, 
    description: "",
    tags: [], 
    location: undefined, 
    thumbnail: undefined, 
    matchingContext: undefined
});

//TODO can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = ['$ngRedux', '$scope', '$element'/*, '$routeParams' /*injections as strings here*/];

function genComponentConf() {
    const template = `
        <!-- TEXTBOX -->
        <div class="cis__mandatory-rest">
            <!-- Remove image dropzone for now -->
            <!--won-image-dropzone on-image-picked="::self.pickImage(image)">
            </won-image-dropzone-->
            <need-textfield on-draft-change="::self.setDraft(draft)"></need-textfield>
        </div>
        <div class="cis__textfield_instruction">
            <span>Title (1st line) &crarr; Longer description. Supports #tags.</span>
        </div>
        <!-- /TEXTBOX/ -->

        <!-- DETAILS Picker -->
        <div class="cis__addDetail">
            <div class="cis__addDetail__header detailPicker clickable"
                ng-click="self.toggleDetail()"
                ng-class="{'closedDetailPicker': !self.showDetail}">
                    <span>Add more detail</span>
                    <svg class="cis__addDetail__header__carret" ng-show="!self.showDetail">
                        <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                    </svg>
                    <svg class="cis__addDetail__header__carret" ng-show="self.showDetail">
                        <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                    </svg>
            </div>
            <div class="cis__detail__items" ng-if="self.showDetail" >

                <!-- LOCATION PICKER -->
                <div class="cis__detail__items__item location"
                    ng-click="self.toggleOpenDetail('location')"
                    ng-class="{'picked' : self.openDetail === 'location'}">
                        <svg class="cis__circleicon" ng-show="!self.details.has('location')">
                            <use xlink:href="#ico36_location_circle" href="#ico36_location_circle"></use>
                        </svg>
                        <svg class="cis__circleicon" ng-show="self.details.has('location')">
                            <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
                        </svg>
                        <span>Address or Location</span>
                </div>

                <!-- TAGS PICKER -->
                <div class="cis__detail__items__item tags"
                    ng-click="self.toggleOpenDetail('tags')"
                    ng-class="{'picked' : self.openDetail === 'tags'}">
                        <svg class="cis__circleicon" ng-show="!self.details.has('tags')">
                            <use xlink:href="#ico36_tags_circle" href="#ico36_tags_circle"></use>
                        </svg>
                        <svg class="cis__circleicon" ng-show="self.details.has('tags')">
                            <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
                        </svg>
                        <span>Tags</span>
                </div>

                <!-- TTL Will be excluded until further notice-->
                <!--div class="cis__detail__items__item ttl"
                    ng-click="self.toggleOpenDetail('ttl')"
                    ng-class="{'picked' : self.openDetail === 'ttl'}">
                        <svg class="cis__circleicon" ng-show="!self.details.has('ttl')">
                            <use xlink:href="#ico36_rdf_logo_circle" href="#ico36_rdf_logo_circle"></use>
                        </svg>
                        <svg class="cis__circleicon" ng-show="self.details.has('ttl')">
                            <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
                        </svg>
                        <span>Turtle (TTL)</span>
                </div-->
            </div>
        </div>
        <!-- /DETAIL Picker/ -->

        <!-- DETAILS -->
        <!-- TODO: move details into the div opened by the detail picker? -->
        <div class="cis__details" ng-if="self.showDetail">

            <!-- LOCATION -->
            <div class="cis__location"  ng-if="self.openDetail === 'location'">
                <div class="cis__addDetail__header location">
                    <svg class="cis__circleicon">
                        <use xlink:href="#ico36_location_circle" href="#ico36_location_circle"></use>
                    </svg>
                    <span>Location</span>
                </div>
                <won-location-picker class="seeksPicker"
                    initial-location="::self.draftObject.location"
                    on-location-picked="::self.updateLocation(location)">
                </won-location-picker>
            </div>

            <!-- TAGS -->
             <div class="cis__tags" ng-if="self.openDetail === 'tags'">
                <div class="cis__addDetail__header tags" ng-click="self.resetTags() && self.updateDraft()">
                    <svg class="cis__circleicon nonHover">
                        <use xlink:href="#ico36_tags_circle" href="#ico36_tags_circle"></use>
                    </svg>
                    <svg class="cis__circleicon hover">
                        <use xlink:href="#ico36_close_circle" href="#ico36_close_circle"></use>
                    </svg>
                    <span class="nonHover">Tags</span>
                    <span class="hover">Remove Tags</span>
                </div>
                <div class="cis__taglist">
                    <span class="cis__taglist__tag" ng-repeat="tag in self.draftObject.tags">{{tag}}</span>
                </div>
                <input class="cis__tags__input"
                    placeholder="e.g. #couch #free" type="text"
                    ng-keyup="::self.updateTags()"
                 />
            </div>

            <!-- TTL Will be excluded until further notice-->
            <!-- div class="cis__ttl" ng-if="self.openDetail === 'ttl'">
                <div class="cis__addDetail__header ttl" ng-click="self.details.delete('ttl') && self.updateDraft()">
                    <svg class="cis__circleicon nonHover">
                        <use xlink:href="#ico36_rdf_logo_circle" href="#ico36_rdf_logo_circle"></use>
                    </svg>
                    <svg class="cis__circleicon hover">
                        <use xlink:href="#ico36_close_circle" href="#ico36_close_circle"></use>
                    </svg>
                    <span class="nonHover">Turtle (TTL)</span>
                    <span class="hover">Remove Turtle (TTL)</span>
                </div>
                <textarea
                    won-textarea-autogrow
                    class="cis__ttl__text won-txt won-txt--code"
                    ng-blur="::self.updateTTL()"
                    ng-keyup="::self.updateTTLBuffered()"
                    placeholder="Enter TTL..."></textarea>
                <div class="cis__ttl__helptext">
                    Expects valid turtle.
                    <{{::self.won.WONMSG.msguriPlaceholder}}> will
                    be replaced by the URI generated for this part (i.e. is/description or seeks/searches)
                    of the need. Use the URI, so your TTL can be found when parsing the need.
                    See \`won.minimalTurtlePrefixes\`
                    for prefixes that will be added automatically. E.g.
                    \`<{{::self.won.WONMSG.msguriPlaceholder}}> dc:title "hello world!". \`
                    For more information see the
                    <a href="https://github.com/researchstudio-sat/webofneeds/blob/master/documentation/need-structure.md">
                        documentation on the need-structure
                    </a>.
                </div>
                <div class="cis__ttl__parse-error" ng-show="self.ttlParseError">{{self.ttlParseError}}</div>
            </div-->
        </div>
        <!-- /DETAILS/ -->
`;
    
    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);
            this.won = won;

            //TODO debug; deleteme
            window.cis4dbg = this;

            this.characterLimit = postTitleCharacterLimit;
           
            this.openDetail = undefined;
            
            this.reset();
            
            const selectFromState = (state) => {
 
                return {
                   
                }
            };

            // Using actionCreators like this means that every action defined there is available in the template.
            connect2Redux(selectFromState, actionCreators, [], this);
        }

        reset(){
            this.draftObject = clone(emptyDraft);
            this.details = new Set(); // remove all detail-cards

            this.resetTags();

            this.showDetail = false; // and close selector
        }
        
        updateDraft(){
            if(!this.details.has("tags")){
                this.draftObject.tags = undefined;
            }
            if(!this.details.has("location")){
                this.draftObject.location = undefined;
            }
            if(!this.details.has("ttl")){
                this.draftObject.ttl = undefined;
            }
            
            this.onUpdate({draft: this.draftObject});
            dispatchEvent(this.$element[0], 'update', {draft: this.draftObject});
        }
        
        setDraft(updatedDraft) {       	
            if(updatedDraft && updatedDraft.tags && updatedDraft.tags.length > 0 && !this.details.has("tags")){
                this.details.add("tags");
            }

            this.textAreaTags = updatedDraft.tags;
            delete updatedDraft.tags; // so they don't overwrite anything when `Object.assign`ing below
            this.updateTags();

            // updatedDraft.tags = this.mergeTags();
            Object.assign(this.draftObject, updatedDraft);
            this.updateDraft();
        }

        resetTags(){
            this.tagsString = "";
            this.textAreaTags = "";
            this.draftObject.tags = [];

            this.details.delete('tags'); // remove card
        }

        updateTags() {
            const tagsInputString = (this.tagsInput() || {}).value;
            this.draftObject.tags = mergeAsSet(
                (this.textAreaTags || []), 
                extractHashtags(tagsInputString)
            );
            
            if(tagsInputString && !this.details.has("tags")){ 
                this.details.add("tags");
            }
        }

        updateTTLBuffered() {
            if(this._ttlUpdateTimeoutId) {
                clearTimeout(this._ttlUpdateTimeoutId);
            }
            this._ttlUpdateTimeoutId = setTimeout(() => this.updateTTL(), 4000);
        }
        updateTTL() {
            //await won.ttlToJsonLd(won.minimalTurtlePrefixes + '\n' + $0.value)
            const ttlString = ((this.ttlInput() || {}).value || "");
            won.ttlToJsonLd(won.minimalTurtlePrefixes + '\n' + ttlString)
            .then(parsedJsonLd => {
                this.$scope.$apply(() => this.ttlParseError = "");
                return parsedJsonLd;
            })
            .catch(parseError => {
                this.$scope.$apply(() => this.ttlParseError = parseError.message);
            })

            if(ttlString && !this.details.has("ttl")){
                this.details.add("ttl");
            }
        }

        updateLocation(location) {
            if(!location && this.details.has("location")){
                this.details.delete("location");
                this.draftObject.location = undefined;
            } else {
                if(!this.details.has("location")) {
                    this.details.add("location");
                }
                this.draftObject.location = location;
            }

            this.updateDraft();
        }

        pickImage(image) {
            this.draftObject.thumbnail = image;
        }

        toggleDetail(){
            this.showDetail = !this.showDetail;
        }

        toggleOpenDetail(detail){
            // open clicked detail
            if(this.openDetail === detail) {
                this.openDetail = undefined;
            } else {
                this.openDetail = detail;
            }
        }
        
        getArrayFromSet(set){
            return Array.from(set);
        }

        ttlInputNg() {
            return angular.element(this.ttlInput());
        }
        ttlInput() {
            if(!this._ttlInput) {
                this._ttlInput = this.$element[0].querySelector('.cis__ttl__text');
            }
            return this._ttlInput;
        }
        tagsInputNg() {
            return angular.element(this.tagsInput());
        }
        tagsInput() {
            if(!this._tagsInput) {
                this._tagsInput = this.$element[0].querySelector('.cis__tags__input');
            }
            return this._tagsInput;
        }
    }
    
    
    
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            isOrSeeks: '=',
             /*
             * Usage:
             *  on-update="::myCallback(draft)"
             */
            onUpdate: '&',
        },
        template: template,
    }
}

export default angular.module('won.owner.components.createIsseek', [
        /*createNeedTitleBarModule,
        posttypeSelectModule,
        labelledHrModule,
        imageDropzoneModule,
        needTextfieldModule,
        locationPickerModule,
        ngAnimate,*/
    ])
    .directive('wonCreateIsseeks', genComponentConf)
    //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
    .name;
