/**
 * Created by ksinger on 02.09.2015.
 */
/**
 * Expects an object with a $scope and/or $element variable (e.g. a controller
 * with those two bound two it) and publishes an event to both of these, yielding
 * a solution that's both angular-1.X- as well as standard-js-conform.
 * @param scopeAndElem
 * @param eventName
 * @param eventData
 */
export function broadcastEvent(scopeAndElem, eventName, eventData) {
    const payload = {'detail': eventData};
    if(scopeAndElem.$scope) {
        //scopeAndElem.$scope.$broadcast(eventName, eventData);
        scopeAndElem.$scope.$broadcast(eventName, payload);

        console.log('broadcasting');
    }
}