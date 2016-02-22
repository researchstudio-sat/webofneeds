;

import angular from 'angular';
import 'ng-redux';
import visitorTitleBarModule from '../owner-title-bar';
import galleryModule from '../gallery';
import postMessagesModule from '../post-messages';
import { labels } from '../../won-label-utils';
import { attach } from '../../utils';
import { actionCreators }  from '../../actions/actions';

const serviceDependencies = ['$scope', '$interval', '$ngRedux', '$q'];

class Controller {
    constructor() {
        attach(this, serviceDependencies, arguments);

        this.selection = 4;

        const selectFromState = (state) => {
            const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
            return {
                post: state.getIn(['needs','ownNeeds', postId]).toJS()
            }
        };

        // Using actionCreators like this means that every action defined there is available in the template.
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }
}

Controller.$inject = [];

export default angular.module('won.owner.components.postOwner', [
        visitorTitleBarModule,
        galleryModule,
        postMessagesModule
    ])
    .controller('PostOwnerController',  [...serviceDependencies, Controller])
    .name;
