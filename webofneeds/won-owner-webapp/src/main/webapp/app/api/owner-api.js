import vocab from "../service/vocab.js";

/**
 * Created by quasarchimaere on 11.06.2019.
 */

import fWorker from "workerize-loader?[name].[contenthash:8]!../../fetch-worker.js";

const fetchWorker = fWorker();

export const fetchDefaultNodeUri = () => fetchWorker.fetchDefaultNodeUri();

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {*}
 */
export const login = credentials => fetchWorker.login(credentials);

export const logout = () => fetchWorker.logout();

export const exportAccount = dataEncryptionPassword =>
  fetchWorker.exportAccount(dataEncryptionPassword);

/**
 * Checks whether the user has a logged-in session.
 * Returns a promise with the user-object if successful
 * or a failing promise if an error has occured.
 *
 * @returns {*}
 */
export const checkLoginStatus = () => fetchWorker.checkLoginStatus();

/**
 * Registers the account with the server.
 * The returned promise fails if something went
 * wrong during creation.
 *
 * @param credentials either {email, password} or {privateId}
 * @returns {*}
 */
export const registerAccount = credentials =>
  fetchWorker.registerAccount(credentials);

/**
 * Accept the Terms Of Service
 */
export const acceptTermsOfService = () => fetchWorker.acceptTermsOfService();

/**
 * Confirm the Registration with the verificationToken-link provided in the registration-email
 */
export const confirmRegistration = verificationToken =>
  fetchWorker.confirmRegistration(verificationToken);

/**
 * Resend the verification mail.
 *
 */
export const resendEmailVerification = email =>
  fetchWorker.resendEmailVerification(email);

/**
 * Send Anonymous Link Email
 *
 */
export const sendAnonymousLinkEmail = (email, privateId) =>
  fetchWorker.sendAnonymousLinkEmail(email, privateId);

/**
 * Change the password of the user currently logged in.
 * @param credentials { email, oldPassword, newPassword }
 * @returns {*}
 */
export const changePassword = credentials =>
  fetchWorker.changePassword(credentials);

/**
 * Transfer an existing privateId User,
 * to a non existing User
 * @param credentials {email, password, privateId}
 * @returns {*}
 */
export const transferPrivateAccount = credentials =>
  fetchWorker.transferPrivateAccount(credentials);

/**
 * Change the password of the user currently logged in.
 * @param credentials  {email, newPassword, recoveryKey}
 * @returns {*}
 */
export const resetPassword = credentials =>
  fetchWorker.resetPassword(credentials);

export const serverSideConnect = (
  fromSocketUri,
  toSocketUri,
  fromPending = false,
  toPending = false,
  autoOpen = false,
  message
) =>
  fetchWorker.serverSideConnect(
    fromSocketUri,
    toSocketUri,
    fromPending,
    toPending,
    autoOpen,
    message
  );

/**
 * Send pushNotifications subscription to server
 */
export const sendSubscriptionToServer = subsctiption =>
  fetchWorker.sendSubscriptionToServer(subsctiption);

/**
 * Get ServerKey
 */
export const getServerKey = () => fetchWorker.getServerKey();
/**
 * Returns all stored Atoms including MetaData (e.g. type, creationDate, location, state) as a Map
 * @param state either "ACTIVE" or "INACTIVE"
 * @returns {*}
 */
export const fetchOwnedMetaAtoms = state =>
  fetchWorker.fetchOwnedMetaAtoms(state);

//FIXME: This might be a problem if messages from non owned atoms are fetched (only requesterWebId(param atomUri) is used not token)
export const fetchMessage = (atomUri, messageUri) =>
  fetchWorker.fetchMessage(atomUri, messageUri);

export const fetchAllMetaAtoms = (
  createdAfterDate,
  state = "ACTIVE",
  limit = 600
) => fetchWorker.fetchAllMetaAtoms(createdAfterDate, state, limit);

export const fetchAllActiveMetaPersonas = () =>
  fetchWorker.fetchAllActiveMetaPersonas(vocab);

export const fetchTokenForAtom = (atomUri, params) =>
  fetchWorker.fetchTokenForAtom(atomUri, params);

export const fetchGrantsForAtom = (atomUri, params) =>
  fetchWorker.fetchGrantsForAtom(atomUri, params);

export const fetchAllMetaAtomsNear = (
  createdAfterDate,
  location,
  maxDistance = 5000,
  limit = 500,
  state = "ACTIVE"
) =>
  fetchWorker.fetchAllMetaAtomsNear(
    createdAfterDate,
    location,
    maxDistance,
    limit,
    state
  );

export const fetchMessageEffects = (connectionUri, messageUri) =>
  fetchWorker.fetchMessageEffects(connectionUri, messageUri);

export const fetchAgreementProtocolUris = connectionUri =>
  fetchWorker.fetchAgreementProtocolUris(connectionUri);

export const fetchAgreementProtocolDataset = connectionUri =>
  fetchWorker.fetchAgreementProtocolDataset(connectionUri);

export const fetchPetriNetUris = connectionUri =>
  fetchWorker.fetchPetriNetUris(connectionUri);

/**
 * Send a message to the endpoint and return a promise with a json payload with messageUri and message as props
 * @param msg
 * @returns {*}
 */
export const sendMessage = msg => fetchWorker.sendMessage(msg);
