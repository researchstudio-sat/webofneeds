import Immutable from "immutable";
import { actionTypes } from "../actions/actions";

const initialState = Immutable.fromJS({});

export default function(allPersonasInState = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.personas.createSuccessful: {
      const persona = action.payload.persona;
      return allPersonasInState.set(persona["@id"], {
        displayName: persona["s:name"],
        aboutMe: persona["s:description"],
        website: persona["s:url"],
      });
    }

    case actionTypes.personas.create:
      //Ignored for now because we don't want to populate our state with unsaved personas
      return allPersonasInState;

    case actionTypes.initialPageLoad:
    case actionTypes.needs.fetchUnloadedNeeds:
    case actionTypes.login: {
      let ownNeeds = action.payload.get("ownNeeds");
      ownNeeds = ownNeeds ? ownNeeds : Immutable.Set();

      const stateWithLoadedPersonas = ownNeeds.reduce(
        (updatedState, persona) => {
          if (
            persona.get("@type") &&
            persona.get("@type").includes &&
            persona.get("@type").includes("won:Persona")
          ) {
            return updatedState.set(persona.get("@id"), {
              displayName: persona.get("s:name"),
              aboutMe: persona.get("s:description"),
              website: persona.get("s:url"),
            });
          }
          {
            return updatedState;
          }
        },
        allPersonasInState
      );
      return stateWithLoadedPersonas;
    }
    default:
      return allPersonasInState;
  }
}
