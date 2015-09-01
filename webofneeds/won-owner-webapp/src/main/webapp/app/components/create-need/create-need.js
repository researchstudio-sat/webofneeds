/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import createNeedTitleBarModule from '../create-need-title-bar';
import posttypeSelectModule from '../posttype-select';


let options = [
    {
        text: 'I want to have something',
        helpText: 'Use this type in case (want) case sam quam aspic temod et que in prendiae perovidel.'
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
    constructor() {
        this.options = options;
    }

}

CreateNeedController.$inject = [];

export default angular.module('won.owner.components.createNeed', [
        createNeedTitleBarModule,
        posttypeSelectModule
    ])
    //.controller('Create-needController', CreateNeedController)
    .controller('CreateNeedController', CreateNeedController)
    .name;
