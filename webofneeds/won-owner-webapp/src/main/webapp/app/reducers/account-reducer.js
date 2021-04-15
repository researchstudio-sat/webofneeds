/**
 * Created by ksinger on 10.05.2016.
 */
import { actionTypes } from "../actions/actions.js";
import { getUri, get } from "../utils.js";
import Immutable from "immutable";

import { isDisclaimerAccepted } from "../won-localstorage.js";

const initialState = Immutable.fromJS({
  loggedIn: false,
  email: undefined,
  emailVerified: false,
  isAnonymous: false,
  acceptedTermsOfService: false,
  acceptedDisclaimer: isDisclaimerAccepted(),
  ownedAtomUris: Immutable.Set(),
  atomSettings: Immutable.Map(),
});

export default function(userData = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.account.store: {
      const username = get(action.payload, "username");
      const emailVerified = get(action.payload, "emailVerified");
      const acceptedTermsOfService = get(
        action.payload,
        "acceptedTermsOfService"
      );
      const privateId = get(action.payload, "privateId");
      const isAnonymous = get(action.payload, "isAnonymous");

      return userData
        .set("loggedIn", true)
        .set("email", username)
        .set("emailVerified", emailVerified)
        .set("acceptedTermsOfService", acceptedTermsOfService)
        .set("isAnonymous", isAnonymous)
        .set("privateId", privateId);
    }

    case actionTypes.atoms.storeOwnedMetaAtoms: {
      const metaAtoms = get(action.payload, "metaAtoms");

      metaAtoms &&
        metaAtoms.map((metaAtom, metaAtomUri) => {
          userData = userData.update("ownedAtomUris", ownedAtomUris =>
            ownedAtomUris.add(metaAtomUri)
          );
        });

      return userData;
    }

    case actionTypes.atoms.createFailure:
    case actionTypes.atoms.removeDeleted:
    case actionTypes.atoms.delete: {
      const atomUri = getUri(action.payload);

      const ownedAtomUris = get(userData, "ownedAtomUris");
      const atomSettings = get(userData, "atomSettings");
      return userData
        .set("ownedAtomUris", ownedAtomUris.remove(atomUri))
        .set("atomSettings", atomSettings.remove(atomUri));
    }

    case actionTypes.atoms.create: //for optimistic additions
    case actionTypes.atoms.createSuccessful: {
      const ownedAtomUris = get(userData, "ownedAtomUris");
      return userData.set(
        "ownedAtomUris",
        ownedAtomUris.add(action.payload.atomUri)
      );
    }

    case actionTypes.account.resendVerificationEmailFailed:
    case actionTypes.account.verifyEmailAddressFailed:
      return userData.set(
        "emailVerificationError",
        action.payload.emailVerificationError
      );
    case actionTypes.account.fetchUserSettingsSuccess: {
      const userSettings = action.payload.atomUserSettings;
      userSettings.forEach(setting => {
        userData = addAtomSettings(userData, setting);
      });
      return userData;
    }
    case actionTypes.account.addAtomUserSetting:
    case actionTypes.account.updateAtomUserSettingsSuccess: {
      return addAtomSettings(userData, action.payload.atomUserSetting);
    }

    case actionTypes.account.verifyEmailAddressSuccess:
      return userData
        .set("emailVerificationError", undefined)
        .set("emailVerified", true);
    case actionTypes.account.verifyEmailAddressStarted:
      return userData.set("emailVerificationError", undefined);

    case actionTypes.account.acceptTermsOfServiceSuccess:
      return userData.set("acceptedTermsOfService", true);
    case actionTypes.account.acceptTermsOfServiceFailed:
      return userData.set("acceptedTermsOfService", false);

    case actionTypes.account.reset:
      return initialState.set(
        "acceptedDisclaimer",
        get(userData, "acceptedDisclaimer")
      );

    case actionTypes.account.loginFailed:
      return userData
        .set("loginError", action.payload.loginError)
        .set("loggedIn", false);

    case actionTypes.view.clearLoginError:
      if (!get(userData, "loggedIn")) {
        return userData.set("loginError", undefined);
      } else {
        return userData;
      }

    case actionTypes.view.clearRegisterError:
      return userData.set("registerError", undefined);

    case actionTypes.account.registerFailed:
      return userData
        .set("registerError", action.payload.registerError)
        .set("loggedIn", false);

    case actionTypes.account.resetPasswordFailed:
      return userData.set(
        "resetPasswordError",
        action.payload.resetPasswordError
      );
    case actionTypes.account.acceptDisclaimerSuccess:
      return userData.set("acceptedDisclaimer", true);

    default:
      return userData;
  }
}

function addAtomSettings(userData, atomSettings) {
  const atomUri = get(atomSettings, "atomUri");
  if (atomUri) {
    let settings = get(userData, "atomSettings");
    return userData.set(
      "atomSettings",
      settings.set(atomUri, Immutable.fromJS(atomSettings))
    );
  } else {
    console.error("Tried to add invalid atom-settings-object: ", atomSettings);
    return userData;
  }
}
