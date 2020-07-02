import { actionTypes } from "../actions/actions.js";
import { get } from "../utils.js";
import Immutable from "immutable";

const initialState = Immutable.fromJS({});

export default function(externalData = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.externalData.store: {
      const data = get(action, "payload");
      if (data) {
        return externalData.merge(data);
      }
      return externalData;
    }

    default:
      return externalData;
  }
}
