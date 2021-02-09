import React from "react";
import { useSelector } from "react-redux";
import { getQueryParams } from "../../utils.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import WonAtomInfo from "../../components/atom-info.jsx";
import WonGenericPage from "~/app/pages/genericPage";

import "~/style/_post.scss";
import { useHistory } from "react-router-dom";
import * as generalSelectors from "../../redux/selectors/general-selectors";

export default function PagePost() {
  const history = useHistory();
  const { postUri: atomUri, connectionUri, tab } = getQueryParams(
    history.location
  );
  const atom = useSelector(generalSelectors.getAtom(atomUri));
  const ownedConnection = useSelector(
    generalSelectors.getOwnedConnection(connectionUri)
  );

  const externalDataState = useSelector(generalSelectors.getExternalDataState);

  const atomTitle = atomUtils.getTitle(atom, externalDataState);

  return (
    <WonGenericPage pageTitle={atomTitle}>
      <main className="atomcontent">
        <WonAtomInfo
          atom={atom}
          atomUri={atomUri}
          ownedConnectionUri={connectionUri}
          ownedConnection={ownedConnection}
          initialTab={tab}
        />
      </main>
    </WonGenericPage>
  );
}
