/**
 * Created by syim on 11.12.2015.
 */
import { actionTypes } from '../actions/actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';
import { buildCreateMessage } from '../won-message-utils';
import won from '../won-es6';

const initialState = Immutable.fromJS({
    posts: []

})
export default createReducer(
    initialState,
    {
        [actionTypes.needs.receive]: (state, {payload:{needs}}) => {
            let uri = 'https://satsrv04.researchstudio.at:8888/won/resource/need/139322635933712380'
            needs.forEach((elem,index,array)=>{
                let uri = elem;
                won.fetch(uri).then(function(result){
                    console.log("linked data fetched for need: "+uri );
                })
            })

            const dummy=[{id: "121337345", title: "New ffdsalat, need furniture", creationDate: "20.11.2015", type: 1, group: "ux barcamp stuff",state:"active", requests: [{},{},{}], matches: [{},{},{}],messages: [{},{},{}], titleImgSrc: "images/someNeedTitlePic.png"},
                {id: "121337345", title: "Clean park 1020 Vienna", creationDate: "20.11.1998", type: 4, group: "gaming",state:"active",requests: [{},{},{}], matches: [{},{},{}], messages: [{},{},{}]},
                {id: "121337345", title: "Car sharing 1020 Vienna", creationDate: "2.3.2001", type: 2, state:"active",requests: [{},{},{}],matches: [{},{},{}],messages: [{},{},{}], titleImgSrc: "images/someNeedTitlePic.png"},
                {id: "121337345", title: "tutu", creationDate: "7.9.2015", type: 3, group: "sat lunch group",state:"active", requests: [{},{},{}],matches: [{},{},{}],messages: [{},{},{}]},
                {id: "121337345", title: "Local Artistry", creationDate: "20.11.2005", type: 2,state:"active",requests: [{},{},{}],matches: [{},{},{}],messages: [{},{},{}], titleImgSrc: "images/someNeedTitlePic.png"},
                {id: "121337345", title: "Cycling Tour de France", creationDate: "1.1.2000", type: 3,state:"inactive",requests: [{},{},{}],matches: [{},{},{}],messages: [{},{},{}]},
                {id: "121337345", title: "Cycling Tour de France", creationDate: "1.1.2000", type: 3,state:"inactive",requests: [{},{},{}],matches: [{},{},{}],messages: [{},{},{}]}]

            return  state.set('posts', Immutable.fromJS(dummy));
        },
        [actionTypes.needs.clean]:(state,{})=>{
            return Immutable.fromJS(initialState);
        }
    }

)