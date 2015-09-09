;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import postItemLineModule from '../post-item-line';

class OverviewPostsController {
    constructor() {
        this.selection = 1;

        this.activePosts = [{name: "blabla"}, {name: "tutu"}, {name: "blabla"}, {name: "tutu"}, {name: "blabla"}, {name: "tutu"}];

        this.closedPosts = this.activePosts;
        this.drafts = this.activePosts;
    }
}

OverviewPostsController.$inject = [];

export default angular.module('won.owner.components.overviewPosts', [
        overviewTitleBarModule,
        postItemLineModule
    ])
    .controller('OverviewPostsController', OverviewPostsController)
    .name;
