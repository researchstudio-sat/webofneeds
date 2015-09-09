;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import postItemLineModule from '../post-item-line';

class OverviewPostsController {
    constructor() {
        this.selection = 1;

        this.activePosts = [{id: "121337345", title: "New flat, need furniture", creationDate: "20.11.2015", type: 1, group: "ux barcamp stuff", requests: [{},{},{}], matches: [{},{},{}], titleImgSrc: "images/someNeedTitlePic.png"},
            {id: "121337345", title: "Clean park 1020 Vienna", creationDate: "20.11.1998", type: 4, group: "gaming", messages: [{},{},{}]},
            {id: "121337345", title: "Car sharing 1020 Vienna", creationDate: "2.3.2001", type: 2, requests: [{},{},{}],matches: [{},{},{}],messages: [{},{},{}], titleImgSrc: "images/someNeedTitlePic.png"},
            {id: "121337345", title: "tutu", creationDate: "7.9.2015", type: 3, group: "sat lunch group", requests: [{},{},{}]},
            {id: "121337345", title: "Local Artistry", creationDate: "20.11.2005", type: 2, matches: [{},{},{}], titleImgSrc: "images/someNeedTitlePic.png"},
            {id: "121337345", title: "Cycling Tour de France", creationDate: "1.1.2000", type: 3}];


        this.closedPosts = this.activePosts;
        this.drafts = this.activePosts;

        this.activePostsOpen = true;
        this.draftsOpen = false;
        this.closedPostsOpen = false;
    }
}

OverviewPostsController.$inject = [];

export default angular.module('won.owner.components.overviewPosts', [
        overviewTitleBarModule,
        postItemLineModule
    ])
    .controller('OverviewPostsController', OverviewPostsController)
    .name;
