/**
 * Created by ksinger on 21.08.2017.
 */

import {
    getParameterByName,
    getParameters,
} from './utils';

import {
    makeParams,
    resetParams,
    resetParamsImm,
    constantParams,
    addConstParams,
} from './configRouting';

/**
 * e.g. `absSRef('post', {postUri: 'http://...'})` + pre-existing private Id =>
 *     `"post({postUri: 'http://..', privatId: '...'})"`
 * @param toRouterState
 * @param queryParams
 * @returns {string} a string that can be used with `ui-sref` and then the same
 *                   behaviour as e.g. `ng-click="self.router__stateGoAbs('post', {postUri: '...'})"`
 *                   except that it supports middle-mouse-button clicks and shows
 *                   the right cursor by default (i.e. it is a regular link)
 */
export function absSRef(toRouterState, queryParams) {

    const currentParams = getParameters();
    //if(!appState) return "";
    //const currentParams = appState.getIn(['router', 'currentParams']);
    //const currentParams = Immutable.Map(constantParams.map(p => [p, getParameterByName(p)]));
    //console.log('asdfasdfasdf foooo ', currentParams);

    const paramsWithConst = addConstParams(resetParamsImm.merge(queryParams), currentParams);
    const paramsString = JSON.stringify(paramsWithConst);
    const srefString = toRouterState + '(' + paramsString + ')';
    return srefString;
}

/**
 * e.g. `resetParamsSRef('post')` + pre-existing private Id =>
 *     `"post({privateId: 'http://..', privatId: '...'})"`
 * @param toRouterState
 * @returns {string} a string that can be used with `ui-sref` and then the same
 *                   behaviour as e.g. `ng-click="self.router__stateGoResetParams('post')"`
 *                   except that it supports middle-mouse-button clicks and shows
 *                   the right cursor by default (i.e. it is a regular link)
 */
export function resetParamsSRef(toRouterState) {
    const currentParams = getParameters();
    const paramsString = JSON.stringify(
        addConstParams(resetParamsImm, currentParams)
    );
    const srefString = toRouterState + '(' + paramsString + ')';
    return srefString;
}
