/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import visitorTitleBarModule from '../owner-title-bar';
import galleryModule from '../gallery';
import postMessagesModule from '../post-messages';
import { attach,mapToMatches } from '../../utils';
import won from '../../won-es6';
import { actionCreators }  from '../../actions/actions';
import needConnectionMessageLineModule from '../connection-message-item-line';
import openConversationModule from '../open-conversation';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
class Controller {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 0;
        window.postownermsg = this;
        this.operConversation = undefined;
        this.connections={
            "https://12344556.com/12345/connections/123":{
                "remoteNeed":{"uri":"https://12344556.com/12345","title":"this is a need","basicNeedType":won.WON.BasicNeedTypeDemand},
                "ownNeed":{"uri":"https://12345566.com/123456","title":"this is an offer","basicNeedType":won.WON.BasicNeedTypeSupply},
                "connection:":{"uri":"https://12344556.com/12345/connections/123","timestamp":"Tuesday"},
                "lastEvent":{"uri":"https://12344556.com/12345/connections/123/events/1","msg":"hello"},
                "messages":[{message:"Hello", timestamp:"Tuesday"},{message:"How are you", timestamp:"Monday"}]
            },
            "https://12344556.com/12345/connections/1234":{
                "remoteNeed":{"uri":"https://12344556.com/123456","title":"this is a need","basicNeedType":won.WON.BasicNeedTypeDemand},
                "ownNeed":{"uri":"https://12345566.com/1234567","title":"this is an offer","basicNeedType":won.WON.BasicNeedTypeSupply},
                "connection:":{"uri":"https://12344556.com/12345/connections/123"},
                "lastEvent":{"uri":"https://12344556.com/12345/connections/123/events/1","msg":"Lalalalal"},
                "messages":[{message:"i want this", timestamp:"Tuesday"},{message:"you can have it", timestamp:"Monday"}
                ]
            }
        }


        this.messages = [{text: "this is my test message", timeStamp: "12.2.2015 17:30", ownMessage: true},
            {text: "this is my test messa lkfja sdlkj ge", timeStamp: "12.2.2015 17:30", ownMessage: false},
            {text: "this is my test message t, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore t, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore t, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore ", timeStamp: "12.2.2015 17:30", ownMessage: false},
            {text: "this is my test message", timeStamp: "12.2.2015 17:30", ownMessage: true},
            {text: "this is my test messaget, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore t, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore ", timeStamp: "12.2.2015 17:30", ownMessage: false},
            {text: "this is my test messaget, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore ", timeStamp: "12.2.2015 17:30", ownMessage: false},
            {text: "this is my test messaget, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore t, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore ", timeStamp: "12.2.2015 17:30", ownMessage: true}
        ];

        this.post = {id: "121337345", location: "Vendiger A6, Umkreis 20 km", title: "Clean park 1020 Vienna", description: "Tatquunt, cum aute ni ut dolluptia is remquam ut ut laut liatque esequam autecatet quat odi aut labore a que culparu nduciet quam aut velent exerfer chicil exeri autatem peritem eat ut everum aliquis excepro eos doluptatis alicturem nonsedic totatist ad ex et aliciatibus cusci ommo blandit, et labo. Ut aut mint quam ipis et optinve liquam nest ma cor rero dolores esequaspiet recusandendi nus evelectios pediae dolenie turitec aepedioribus velessequi debis arum, serro blanda nim facessumquo voluptam, qui dolumquosant.", creationDate: "20.11.1998", type: 4, group: "gaming", titleImgSrc: "images/someNeedTitlePic.png", messages: this.messages};

        const selectFromState = (state)=>{

            return {
                 messages: Object.keys(state.getIn(['connections','connectionsDeprecated']).toJS())
                    .map(key=>state.getIn(['connections','connectionsDeprecated']).toJS()[key])
                    .filter(conn=>{
                        if(conn.connection.hasConnectionState===won.WON.RequestReceived && state.getIn(['events',conn.connection.uri]) !== undefined){
                            return true
                        }
                    }),
                messagesOfNeed:mapToMatches(Object.keys(state.getIn(['connections','connectionsDeprecated']).toJS())
                    .map(key=>state.getIn(['connections','connectionsDeprecated']).toJS()[key])
                    .filter(conn=>{
                        if(conn.connection.hasConnectionState===won.WON.RequestReceived){
                            return true
                        }
                    }))
            };
        }

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        //  this.loadMatches();
        this.$scope.$on('$destroy', disconnect);

    }
}

Controller.$inject = serviceDependencies;

export default angular.module('won.owner.components.postOwner.messages', [
        visitorTitleBarModule,
        galleryModule,
        postMessagesModule,
        needConnectionMessageLineModule
    ])
    .controller('PostOwnerMessagesController', Controller)
    .name;
