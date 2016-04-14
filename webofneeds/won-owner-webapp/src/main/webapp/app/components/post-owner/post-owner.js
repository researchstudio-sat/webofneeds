;

import angular from 'angular';
import 'ng-redux';
import visitorTitleBarModule from '../owner-title-bar';
import galleryModule from '../gallery';
import postMessagesModule from '../post-messages';
import { labels, relativeTime, updateRelativeTimestamps } from '../../won-label-utils';
import { attach } from '../../utils';
import { actionCreators }  from '../../actions/actions';

const serviceDependencies = ['$scope', '$interval', '$ngRedux', '$q'];

class Controller {
    constructor() {
        window.poc4dbg = this;
        attach(this, serviceDependencies, arguments);

        this.selection = 4;

        const selectFromState = (state) => {
            const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
            const post = state.getIn(['needs','ownNeeds', postId]);
            return {
                postUri: postId,
                post: post,
                postJS: post? post.toJS() : {},
            }
        };

        // Using actionCreators like this means that every action defined there is available in the template.
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);

        updateRelativeTimestamps(
            this.$scope,
            this.$interval,
            this.post.creationDate,
                t => this.post.creationDate = t);
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
