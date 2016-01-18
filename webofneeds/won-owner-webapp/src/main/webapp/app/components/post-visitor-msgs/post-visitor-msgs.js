/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import visitorTitleBarModule from '../visitor-title-bar';
import galleryModule from '../gallery';
import postMessagesModule from '../post-messages';

class Controller {
    constructor() {
        this.selection = 1;

        this.messages = [{text: "this is my test message", timeStamp: "12.2.2015 17:30", ownMessage: true},
            {text: "this is my test messa lkfja sdlkj ge", timeStamp: "12.2.2015 17:30", ownMessage: false},
            {text: "this is my test message t, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore t, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore t, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore ", timeStamp: "12.2.2015 17:30", ownMessage: false},
            {text: "this is my test message", timeStamp: "12.2.2015 17:30", ownMessage: true},
            {text: "this is my test messaget, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore t, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore ", timeStamp: "12.2.2015 17:30", ownMessage: false},
            {text: "this is my test messaget, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore ", timeStamp: "12.2.2015 17:30", ownMessage: false},
            {text: "this is my test messaget, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore t, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore ", timeStamp: "12.2.2015 17:30", ownMessage: true}
        ];

        this.post = {id: "121337345", location: "Vendiger A6, Umkreis 20 km", title: "Clean park 1020 Vienna", description: "Tatquunt, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore a que culparu nduciet quam aut velent exerfer chicil exeri autatem peritem eat ut everum aliquis excepro eos doluptatis alicturem nonsedic totatist ad ex et aliciatibus cusci ommo blandit, et labo. Ut aut mint quam ipis et optinve liquam nest ma cor rero dolores esequaspiet recusandendi nus evelectios pediae dolenie turitec aepedioribus velessequi debis arum, serro blanda nim facessumquo voluptam, qui dolumquosant.", creationDate: "20.11.1998", type: 4, group: "gaming", titleImgSrc: "images/someNeedTitlePic.png", messages: this.messages};
    }
}

Controller.$inject = [];

export default angular.module('won.owner.components.postVisitor.messages', [
        visitorTitleBarModule,
        galleryModule,
        postMessagesModule
    ])
    .controller('PostVisitorMsgsController', Controller)
    .name;
