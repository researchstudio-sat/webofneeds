import React, { useEffect } from "react";
import { useHistory } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { actionCreators } from "~/app/actions/actions";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import {
  getUri,
  generateLink,
  getQueryParams,
  extractAtomUriFromConnectionUri,
} from "../../utils.js";
import * as atomUtils from "../../redux/utils/atom-utils";
import * as connectionUtils from "../../redux/utils/connection-utils";
import * as processUtils from "../../redux/utils/process-utils.js";
import WonAtomMessages from "../../components/atom-messages.jsx";
import WonGroupAtomMessages from "../../components/group-atom-messages.jsx";
import WonConnectionsOverview from "../../components/connections-overview.jsx";
import WonGenericPage from "~/app/pages/genericPage";

import "~/style/_atom-messages.scss";
import "~/style/_connections.scss";
import "~/style/_responsiveness-utils.scss";
import ico36_message from "~/images/won-icons/ico36_message.svg";

export default function PageConnections() {
  const history = useHistory();
  const dispatch = useDispatch();

  const {
    connectionUri: selectedConnectionUri,
    postUri: atomUriInRoute,
  } = getQueryParams(history.location);

  const processState = useSelector(generalSelectors.getProcessState);
  const atom = useSelector(
    generalSelectors.getOwnedAtomByConnectionUri(selectedConnectionUri)
  );
  const atomUri =
    getUri(atom) || extractAtomUriFromConnectionUri(selectedConnectionUri);
  const isAtomFetchNecessary = processUtils.isAtomFetchNecessary(
    processState,
    atomUri,
    atom
  );

  const selectedConnection = atomUtils.getConnection(
    atom,
    selectedConnectionUri
  );
  const selectedTargetAtom = useSelector(
    generalSelectors.getAtom(
      connectionUtils.getTargetAtomUri(selectedConnection)
    )
  );

  const targetAtomUri = selectedTargetAtom && getUri(selectedTargetAtom);
  const isTargetAtomFetchNecessary = processUtils.isAtomFetchNecessary(
    processState,
    targetAtomUri,
    selectedTargetAtom
  );
  const isTargetAtomLoading = processUtils.isAtomLoading(
    processState,
    targetAtomUri
  );

  const isSelectedConnectionGroupChat =
    selectedConnection &&
    selectedTargetAtom &&
    connectionUtils.hasTargetSocketUri(
      selectedConnection,
      atomUtils.getGroupSocket(selectedTargetAtom)
    );

  const isSelectedConnectionChat =
    selectedConnection &&
    selectedTargetAtom &&
    connectionUtils.hasTargetSocketUri(
      selectedConnection,
      atomUtils.getChatSocket(selectedTargetAtom)
    );

  useEffect(
    () => {
      if (isAtomFetchNecessary) {
        console.debug("fetch atomUri, ", atomUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
      }

      if (isTargetAtomFetchNecessary) {
        console.debug("fetch atomUri, ", targetAtomUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(targetAtomUri));
      }

      if (!atomUriInRoute && ownedAtoms) {
        const unloadedAtoms = ownedAtoms.filter((atom, atomUri) =>
          processUtils.isAtomFetchNecessary(processState, atomUri, atom)
        );

        if (unloadedAtoms.size > 0) {
          unloadedAtoms.mapKeys(atomUri => {
            console.debug("fetch atomUri, ", atomUri);
            dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
          });
        }
      }
      //If the request has not a Chat-, or GroupSocket we won't show the messages view
      if (
        !isAtomFetchNecessary &&
        !isTargetAtomFetchNecessary &&
        selectedConnection &&
        targetAtomUri &&
        !isTargetAtomLoading &&
        !isSelectedConnectionGroupChat &&
        !isSelectedConnectionChat
      ) {
        history.replace(
          generateLink(
            history.location,
            { postUri: getUri(selectedTargetAtom) },
            "/post"
          )
        );
      }
    },
    [
      selectedConnectionUri,
      atom,
      ownedAtoms,
      selectedConnection,
      targetAtomUri,
      isTargetAtomLoading,
      isTargetAtomFetchNecessary,
      isAtomFetchNecessary,
      atomUriInRoute,
    ]
  );

  const ownedAtoms = useSelector(generalSelectors.getOwnedAtoms);

  const activePinnedAtomUri = useSelector(viewSelectors.getActivePinnedAtomUri);

  const allChatConnections = useSelector(
    activePinnedAtomUri
      ? generalSelectors.getChatConnectionsOfActivePinnedAtom(
          activePinnedAtomUri
        )
      : generalSelectors.getAllUnassignedUnpinnedChatConnections
  );
  const storedAtoms = useSelector(generalSelectors.getAtoms);

  let contentElements;
  if (selectedConnection && atomUriInRoute) {
    contentElements = (
      <main className="overview__justconnection">
        {isSelectedConnectionGroupChat ? (
          <WonGroupAtomMessages
            connection={selectedConnection}
            backToChats={!atomUriInRoute}
          />
        ) : (
          <WonAtomMessages
            connection={selectedConnection}
            backToChats={!atomUriInRoute}
          />
        )}
      </main>
    );
  } else if (allChatConnections && allChatConnections.size > 0) {
    contentElements = (
      <React.Fragment>
        <aside
          className={
            "overview__left " + (selectedConnection ? "hide-in-responsive" : "")
          }
        >
          <WonConnectionsOverview
            chatConnections={allChatConnections}
            storedAtoms={storedAtoms}
          />
        </aside>
        {selectedConnection ? (
          <main className="overview__right">
            {isSelectedConnectionGroupChat ? (
              <WonGroupAtomMessages
                connection={selectedConnection}
                backToChats={!atomUriInRoute}
              />
            ) : (
              <WonAtomMessages
                connection={selectedConnection}
                backToChats={!atomUriInRoute}
              />
            )}
          </main>
        ) : (
          <main className="overview__rightempty hide-in-responsive">
            <div className="overview__rightempty__noselection">
              <svg
                className="overview__rightempty__noselection__icon"
                title="Messages"
              >
                <use xlinkHref={ico36_message} href={ico36_message} />
              </svg>
              <div className="overview__rightempty__noselection__text">
                No Chat selected
              </div>
              <div className="overview__rightempty__noselection__subtext">
                Click on a Chat on the left to open
              </div>
            </div>
          </main>
        )}
      </React.Fragment>
    );
  } else {
    // TODO: FETCH ALL ATOMS TO DETERMINE IF NO CHATS ARE ACTUALLY TRUE

    contentElements = (
      <main className="overview__nochats">
        <div className="overview__nochats__empty">
          <svg className="overview__nochats__empty__icon" title="Messages">
            <use xlinkHref={ico36_message} href={ico36_message} />
          </svg>
          <div className="overview__nochats__empty__text">
            No Open Chats available
          </div>
        </div>
      </main>
    );
  }

  return <WonGenericPage pageTitle="Chats">{contentElements}</WonGenericPage>;
}
