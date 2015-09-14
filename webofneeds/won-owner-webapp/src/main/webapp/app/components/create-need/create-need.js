/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import createNeedTitleBarModule from '../create-need-title-bar';
import posttypeSelectModule from '../posttype-select';
import labelledHrModule from '../labelled-hr';
import dynamicTextfieldModule from '../dynamic-textfield';


let postTypeTexts = [
    {
        text: 'I want to have something',
        helpText: 'Use this type in case (want) foo sam quam aspic temod et que in prendiae perovidel.'
    },
    {
        text: 'I offer something',
        helpText: 'Use this type in case (offer) case sam quam aspic temod et que in prendiae perovidel.'
    },
    {
        text: 'I want to do something together',
        helpText: 'Use this type in case case (together) sam quam aspic temod et que in prendiae perovidel.'
    },
    {
        text: 'I want to change something',
        helpText: 'Use this type in case (change) case sam quam aspic temod et que in prendiae perovidel.'
    }
]

class CreateNeedController {
    constructor($scope) {
        this.postTypeTexts = postTypeTexts;

        //TODO debug; deleteme
        window.cnc = this;
        console.log('create-need-controller: ', this);
        console.log('create-need $scope: ', $scope);
    }

    selectType(idx) {
        console.log('selected type ', idx);
    }
    unselectType() {
        console.log('unselected type ');
    }

}

//CreateNeedController.$inject = ['$scope'];
CreateNeedController.$inject = [];

export default angular.module('won.owner.components.createNeed', [
        createNeedTitleBarModule,
        posttypeSelectModule,
        labelledHrModule,
        dynamicTextfieldModule,
    ])
    .controller('CreateNeedController', ['$q', /*dependency injections as strings here*/ CreateNeedController])
    .name;
