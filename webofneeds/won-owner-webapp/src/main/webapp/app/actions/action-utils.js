/**
 * Created by ksinger on 26.11.2015.
 */

import { reduceAndMapTreeKeys, flattenTree, tree2constants } from '../utils';

export function hierarchy2Creators(actionHierarchy) {
    const actionTypes = tree2constants(actionHierarchy);
    return Object.freeze(flattenTree(
        reduceAndMapTreeKeys(
            (path, k) => path.concat(k), //construct paths, e.g. ['draft', 'new']
            (path) => {
                /* leaf can either be a defined creator or a
                 * placeholder asking to generate one.
                 */
                const potentialCreator = won.lookup(actionHierarchy, path);
                if(typeof potentialCreator === 'function') {
                    return potentialCreator; //already a defined creator. hopefully.
                } else {
                    const type = won.lookup(actionTypes, path);
                    return createActionCreator(type);
                }
            }, [], actionHierarchy
        ),
        '__'));
}

export function createActionCreator(type) {
    return (payload) => {
        console.debug('creating instance of actionType ', type, ' with payload: ', payload);
        return {type, payload};
    };
}
