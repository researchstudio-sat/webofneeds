import React, { useEffect } from "react";
import { useHistory } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { actionCreators } from "~/app/actions/actions";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import {
  get,
  getIn,
  getQueryParams,
  extractAtomUriFromConnectionUri,
} from "../../utils.js";
import * as atomUtils from "../../redux/utils/atom-utils";
import * as processUtils from "../../redux/utils/process-utils.js";
import WonActivityMessages from "../../components/activity-messages.jsx";
import WonGroupAtomMessages from "../../components/group-atom-messages.jsx";
import WonConnectionsOverview from "../../components/connections-overview.jsx";
import WonGenericPage from "~/app/pages/genericPage";

import "~/style/_connections.scss";
import "~/style/_responsiveness-utils.scss";
import ico36_message from "~/images/won-icons/ico36_message.svg";

export default function PageActivities() {
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
    get(atom, "uri") || extractAtomUriFromConnectionUri(selectedConnectionUri);
  const isAtomFetchNecessary = processUtils.isAtomFetchNecessary(
    processState,
    atomUri,
    atom
  );

  const selectedConnection = getIn(atom, [
    "connections",
    selectedConnectionUri,
  ]);
  const selectedTargetAtom = useSelector(
    generalSelectors.getAtom(get(selectedConnection, "targetAtomUri"))
  );

  useEffect(
    () => {
      if (isAtomFetchNecessary) {
        console.debug("fetch atomUri, ", atomUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
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
    },
    [
      selectedConnectionUri,
      atom,
      ownedAtoms,
      isAtomFetchNecessary,
      atomUriInRoute,
    ]
  );

  const ownedAtoms = useSelector(generalSelectors.getOwnedAtoms);

  const isSelectedConnectionGroupChat =
    selectedConnection &&
    atomUtils.getGroupSocket(selectedTargetAtom) ===
      get(selectedConnection, "targetSocketUri");
  const allPartnerActivityConnections = useSelector(
    generalSelectors.getAllPartnerActivityConnections
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
          <WonActivityMessages
            connection={selectedConnection}
            backToChats={!atomUriInRoute}
          />
        )}
      </main>
    );
  } else if (allPartnerActivityConnections.size > 0) {
    contentElements = (
      <React.Fragment>
        <aside
          className={
            "overview__left " + (selectedConnection ? "hide-in-responsive" : "")
          }
        >
          <WonConnectionsOverview
            chatConnections={allPartnerActivityConnections}
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
              <WonActivityMessages
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
