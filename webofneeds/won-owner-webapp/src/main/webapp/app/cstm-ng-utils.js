/**
 *
 * Created by ksinger on 02.09.2015.
 */

import { zipWith } from "./utils.js";

/**
 * Attaches the contents of `attachments` to `target` using the constiable names from `names`
 * @param target the object
 * @param names array of constiable names
 * @param attachments array of objects/values
 */
export function attach(target, names, attachments) {
  const pairs = zipWith(
    (name, attachment) => [name, attachment],
    names,
    attachments
  );
  for (const [name, attachment] of pairs) {
    target[name] = attachment;
  }
}
