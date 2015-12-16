;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import matchesFlowItemModule from '../matches-flow-item';
import matchesGridItemModule from '../matches-grid-item';
import matchesListItemModule from '../matches-list-item';

class OverviewMatchesController {
    constructor() {
        this.selection = 3;

        this.r1 = {timeStamp: "yesterday 12:05", match: {type: 2, title: "i am the match to that", titleImgSrc: "images/furniture3.png"}, id: "123213213", title: "I am the request one",  group: "User XP", type: 1, message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", images: [{src: "images/furniture2.png"}]};
        this.r2 = {timeStamp: "yesterday 12:05", match: {type: 3, title: "i am the match to that" }, id: "123213213", title: "I am the request one", type: 1, message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", images: [{src: "images/furniture3.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"}]};
        this.r3 = {timeStamp: "yesterday 12:05", match: {type: 4, title: "bananas", titleImgSrc: "images/furniture3.png"}, id: "123213213", title: "I am the request one", type: 2, message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", images: [{src: "images/furniture1.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"}]};
        this.r4 = {timeStamp: "yesterday 12:05", match: {type: 4, title: "i am thethat", titleImgSrc: "images/furniture2.png"}, id: "123213213", title: "I am the request one", type: 3, message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis"};
        this.r5 = {timeStamp: "yesterday 12:05", match: {type: 1, title: "i am thech to that"}, id: "123213213", title: "I am the request one", type: 3, message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", images: [{src: "images/furniture4.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"}]};
        this.r6 = {timeStamp: "yesterday 12:05", match: {type: 2, title: "i am thech to that", titleImgSrc: "images/furniture1.png"}, id: "123213213", title: "I am the request one",  group: "gaming", type: 4, titleImgSrc: "images/someNeedTitlePic.png", message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", images: [{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"}]};
        this.r7 = {timeStamp: "yesterday 12:05", match: {type: 3, title: "i am theatch to that", titleImgSrc: "images/furniture3.png"}, id: "123213213", title: "I am the request one", type: 2, message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", images: [{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"}]};
        this.r8 = {timeStamp: "yesterday 12:05", match: {type: 2, title: "i am the match to that", titleImgSrc: "images/furniture4.png"}, id: "123213213", title: "I am the request one", type: 3, message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis"};
        this.r9 = {timeStamp: "yesterday 12:05", match: {type: 4, title: "i am theto that", titleImgSrc: "images/furniture3.png"}, id: "123213213", title: "I am the request one", type: 4, message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", images: [{src: "images/furniture3.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"}]};
        this.r10 = {timeStamp: "yesterday 12:05", match: {type: 1, title: "i am the match to that" }, id: "123213213", title: "I am the request one", type: 1, message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", images: [{src: "images/furniture1.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"}]};
        this.r11 = {timeStamp: "yesterday 12:05", match: {type: 1, title: "i am the match to that"}, id: "123213213", title: "I am the request one", type: 2, message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", images: [{src: "images/furniture4.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"}]};
        this.r12 = {timeStamp: "yesterday 12:05", match: {type: 2, title: "i am the match", titleImgSrc: "images/furniture3.png"}, id: "123213213", title: "I am the request one", type: 3, message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis"};

        this.items = [this.r1, this.r2, this.r3, this.r4, this.r5, this.r6, this.r7, this.r8, this.r9, this.r10, this.r11, this.r12];

        this.matches = [{type: 2, group: "User XP", title: "i am the match", titleImgSrc: "images/furniture1.png", matches: [this.r1, this.r2, this.r3]},
            {type: 3, title: "i am the matchdafsf", titleImgSrc: "images/furniture3.png", matches: [this.r4, this.r5, this.r6]},
            {type: 4, group: "Soccer", title: "i am the dafsd", titleImgSrc: "images/furniture3.png", matches: [this.r7, this.r8, this.r9]},
            {type: 1, title: "i am the matchsafdsdf", titleImgSrc: "images/furniture2.png", matches: [this.r10, this.r11, this.r12]}];

        this.viewType = 0;
    }
}

export default angular.module('won.owner.components.overviewMatches', [
    overviewTitleBarModule,
    matchesFlowItemModule,
    matchesGridItemModule,
    matchesListItemModule
])
    .controller('OverviewMatchesController', OverviewMatchesController)
    .name;

