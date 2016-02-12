import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import feedItemModule from '../feed-item'
import { actionCreators }  from '../../actions/actions';
import { attach } from '../../utils';
const serviceDependencies = ['$q', '$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];
class FeedController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 0;

        this.r1 = {id: "123213213", title: "I am the request one", type: 3, titleImgSrc: "images/someNeedTitlePic.png", message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", images: [{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"}]};
        this.r2 = {id: "123213213", title: "I am the request one", type: 3, titleImgSrc: "images/someNeedTitlePic.png", message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", images: [{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"}]};
        this.r3 = {id: "123213213", title: "I am the request one", type: 3, titleImgSrc: "images/someNeedTitlePic.png", message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", images: [{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"}]};
        this.r4 = {id: "123213213", title: "I am the request one", type: 3, titleImgSrc: "images/someNeedTitlePic.png", message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis"};

        this.items = [{id: "121337345", title: "New flat, need furniture", type: 1, requests: [this.r1], matches: [this.r1]},
            {id: "121337345", title: "Clean park 1020 Vienna", type: 4, group: "gaming", requests: [this.r1, this.r2, this.r3, this.r4], matches: [this.r1, this.r2, this.r3, this.r4]},
            {id: "121337345", title: "Car sharing 1020 Vienna", type: 2, titleImgSrc: "images/someNeedTitlePic.png", requests: [this.r2], matches: [this.r2]},
            {id: "121337345", title: "tutu", type: 3, group: "sat lunch group" , requests: [this.r1, this.r2, this.r3, this.r4]},
            {id: "121337345", title: "Local Artistry", type: 2, titleImgSrc: "images/someNeedTitlePic.png", requests: [this.r1, this.r2]},
            {id: "121337345", title: "Cycling Tour de France", type: 3, requests: [this.r1, this.r2]}];

        const selectFromState = (state) =>({
            posts: state.getIn(["needs", "needs"]).toJS()
        })
        const disconnect = this.$ngRedux.connect(selectFromState,actionCreators)(this)
        this.$scope.$on('$destroy', disconnect);
    }

}

export default angular.module('won.owner.components.feed', [
    overviewTitleBarModule,
    feedItemModule
])
    .controller('FeedController', [...serviceDependencies,FeedController])
    .name;

