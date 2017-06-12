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
//import draftStoreModule from '../../stores/draft-store';
import { attach } from '../../utils';
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
]

//TODO can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = ['$q', '$ngRedux', '$scope'/*'$routeParams' /*injections as strings here*/];

class CreateNeedController {
    constructor(/* arguments <- serviceDependencies */) {
        attach(this, serviceDependencies, arguments);

        this.postTypeTexts = postTypeTexts;
        this.characterLimit = 140; //TODO move to conf

        //TODO debug; deleteme
        window.cnc = this;

        //this.titlePicZoneNg().bind('click', e => 0);
        //this.titlePicZone().addEventListener('click', e => 0);
        //this.titlePicZone().addEventListener('drop', e => 0);

        const selectFromState = (state) => {
            const draftId = state.getIn(['router', 'currentParams', 'draftId']);
            return {
                draftId,
                tags: state.getIn(['drafts', draftId, 'tags']),
                pendingPublishing: state.hasIn(['drafts', draftId, 'pendingPublishingAs']),
                userHasSelectedType: state.hasIn(['drafts', draftId, 'type']),
            }
        };


        // Using actionCreators like this means that every action defined there is available in the template.
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);

        /*
         does selectFromState make sure to foregoe updates when the
         data that is selected hasn't changed?. Otherwise we need to access
         the state directy (as it's an immutablejs structure)
         -> *nope* -> only use get operations in the selection above (so the
         object reference isn't changed and $watch doesn't trigger) -> just
         pass whole state and select in the template to avoid the boilderplate?

         //@select:: doublewubs: state.get('wubs').concat(state.get('wubs'))
         //this.$scope.$watch(() => this.doublewubs, () => console.log('cnc doublewubs watch ', this.doublewubs.toJS()));

         UPDATE: we can use the memoized selectors provides by the reselect-library
         http://rackt.org/redux/docs/recipes/ComputingDerivedData.html
         */
    }
    isValid(){
        const draft = this.$ngRedux.getState().getIn(['drafts', this.draftId]);
        if(!draft) return false;

        const type = draft.get('type');
        const title = draft.get('title');

        return type && title && title.length < this.characterLimit;
    }

    selectType(typeIdx) {
        console.log('selected type ', postTypeTexts[typeIdx].type);
        //const draftIdx = this.drafts.get('activeDraftIdx');
        this.drafts__change__type({draftId: this.draftId, type: postTypeTexts[typeIdx].type}); //TODO proper draft idx in URL
    }
    unselectType() {
        console.log('unselected type ');
        this.drafts__change__type({draftId: this.draftId, type: undefined}); //TODO proper draft idx in URL
    }
    titlePicZoneNg() {
        if(!this._titlePicZone) {
            this._titlePicZone = this.$element.find('#titlePic');
        }
        return this._titlePicZone;
    }
    titlePicZone() {
        return titlePicZoneNg[0];
    }
    publish() {
        if (!this.pendingPublishing) {
            this.drafts__publish(
                this.$ngRedux.getState().getIn(['drafts', this.draftId]).toJS(),
                this.$ngRedux.getState().getIn(['config', 'defaultNodeUri'])
            );
        }

        //on-image-picked="::self.drafts__change__thumbnail({draftId: self.draftId, image: image})">
    }

}

//CreateNeedController.$inject = serviceDependencies;

export default angular.module('won.owner.components.createNeed', [
        createNeedTitleBarModule,
        posttypeSelectModule,
        labelledHrModule,
        imageDropzoneModule,
        needTextfieldModule,
        locationPickerModule,
    ])
    //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
    .controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
    .name;
