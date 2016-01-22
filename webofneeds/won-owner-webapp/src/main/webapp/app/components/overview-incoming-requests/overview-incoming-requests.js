/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import requestItemLineModule from '../request-item-line';
import openRequestModule from '../open-request';
import { attach,mapToMatches } from '../../utils';
import { actionCreators }  from '../../actions/actions';
const serviceDependencies = ['$q', '$ngRedux', '$scope'];

class IncomingRequestsController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        window.oireq = this;
        this.selection = 2;

        this.r1 = {id: "123213213", title: "I am the request one", type: 3, titleImgSrc: "images/someNeedTitlePic.png", timeStamp: "Today 15:30", message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", read: false, images: [{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"}]};
        this.r2 = {id: "123213213", title: "I am the request one", type: 3, titleImgSrc: "images/someNeedTitlePic.png", timeStamp: "Today 15:30", message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", read: false, images: [{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"}]};
        this.r3 = {id: "123213213", title: "I am the request one", type: 3, titleImgSrc: "images/someNeedTitlePic.png", timeStamp: "Today 15:30", message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", read: false, images: [{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"},{src: "images/furniture2.png"},{src: "images/furniture1.png"},{src: "images/furniture3.png"},{src: "images/furniture4.png"},{src: "images/furniture_big.jpg"}]};
        this.r4 = {id: "123213213", title: "I am the request one", type: 3, titleImgSrc: "images/someNeedTitlePic.png", timeStamp: "Today 15:30", message: "To whoom it may concern. We are a group of peole in the Lestis et eaquuntiore dolluptaspid quatur quisinia aspe sus voloreiusa plis", read: false};

        this.requests = [{id: "121337345", title: "New flat, need furniture", type: 1, requests: [this.r1]},
            {id: "121337345", title: "Clean park 1020 Vienna", type: 4, group: "gaming", requests: [this.r1, this.r2, this.r3, this.r4]},
            {id: "121337345", title: "Car sharing 1020 Vienna", type: 2, titleImgSrc: "images/someNeedTitlePic.png", requests: [this.r2]},
            {id: "121337345", title: "tutu", type: 3, group: "sat lunch group" , requests: [this.r1, this.r2, this.r3, this.r4]},
            {id: "121337345", title: "Local Artistry", type: 2, titleImgSrc: "images/someNeedTitlePic.png", requests: [this.r1, this.r2]},
            {id: "121337345", title: "Cycling Tour de France", type: 3, requests: [this.r1, this.r2]}];

        const selectFromState = (state)=>{

            return {
                incomingRequests: Object.keys(state.getIn(['connections','connections']).toJS())
                    .map(key=>state.getIn(['connections','connections']).toJS()[key])
                    .filter(conn=>{
                        if(conn.connection.hasConnectionState===won.WON.RequestReceived){
                            return true
                        }
                    }),
                incomingRequestsOfNeed:mapToMatches(Object.keys(state.getIn(['connections','connections']).toJS())
                    .map(key=>state.getIn(['connections','connections']).toJS()[key])
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

IncomingRequestsController.$inject = [];

export default angular.module('won.owner.components.overviewIncomingRequests', [
        overviewTitleBarModule,
        requestItemLineModule,
        openRequestModule
    ])
    .controller('OverviewIncomingRequestsController', [...serviceDependencies,IncomingRequestsController])
    .name;
