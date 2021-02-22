import Immutable from "immutable";

export const parse = async (jsonldAtomAndAuth, fakeNames, vocab) => {
  //TODO: IMPL THIS AS ServiceWorker
  console.debug("TODO: parseAtom-worker: jsonldAtomAndAuth", jsonldAtomAndAuth);
  console.debug("TODO: parseAtom-worker: fakeNames", fakeNames);
  console.debug("TODO: parseAtom-worker: vocab: ", vocab);

  return Immutable.fromJS(jsonldAtomAndAuth);
};
