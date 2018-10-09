import Immutable from "immutable";
import { actionTypes } from "../actions/actions";

const initialState = Immutable.fromJS({});

export default function(allIdentitiesInState = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.identities.createSuccessful: {
      const identity = action.payload.identity;
      return allIdentitiesInState.set(identity["@id"], {
        displayName: identity["s:name"],
        aboutMe: identity["s:description"],
        website: identity["s:url"],
      });
    }

    case actionTypes.identities.create:
      //Ignored for now because we don't want to populate our state with unsaved identities
      return allIdentitiesInState;

    case actionTypes.initialPageLoad:
    case actionTypes.needs.fetchUnloadedNeeds:
    case actionTypes.login: {
      let ownNeeds = action.payload.get("ownNeeds");
      ownNeeds = ownNeeds ? ownNeeds : Immutable.Set();

      const stateWithLoadedIdentities = ownNeeds.reduce(
        (updatedState, identity) => {
          if (
            identity.get("@type") &&
            identity.get("@type").includes &&
            identity.get("@type").includes("won:Persona")
          ) {
            return updatedState.set(identity.get("@id"), {
              displayName: identity.get("s:name"),
              aboutMe: identity.get("s:description"),
              website: identity.get("s:url"),
            });
          }
          {
            return updatedState;
          }
        },
        allIdentitiesInState
      );
      return stateWithLoadedIdentities;
    }
  }
}
