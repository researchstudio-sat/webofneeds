/**
 * Created by ksinger on 01.09.2015.
 */

export function hyphen2Camel(hyphened) {
    return hyphened
        .replace(/^([a-z])/, args => args[0].toUpperCase()) //capitalize first letter
        .replace(/-([a-z])/g, args => args[1].toUpperCase()) //hyphens to camel-case
}

export function camel2Hyphen(camelled) {
    return cammelled
        .replace(/^([A-Z])/, args => args[0].toLowerCase()) //de-capitalize first letter
        .replace(/(.)([A-Z])/g, args => args[0] + '-' + args[1].toLowerCase()) // camel-case to hyphens
}
