import React, { useState, useEffect } from "react";

import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
import { useSelector, useDispatch } from "react-redux";
import * as generalSelectors from "../redux/selectors/general-selectors";
import { generateLink, get, toAbsoluteURL } from "../utils";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils";
import { ownerBaseUrl } from "~/config/default.js";
import * as wonUtils from "../won-utils.js";
import pdfMake from "pdfmake/build/pdfmake";
import pdfFonts from "pdfmake/build/vfs_fonts";

import "~/style/_context-dropdown.scss";
import ico16_contextmenu from "~/images/won-icons/ico16_contextmenu.svg";
import { useHistory } from "react-router-dom";

export default function WonAtomContextDropdown({ atom, className }) {
  const dispatch = useDispatch();
  const history = useHistory();
  const atomUri = get(atom, "uri");
  const [contextMenuOpen, setContextMenuOpen] = useState(false);

  let thisNode;

  useEffect(() => {
    function handleClick(e) {
      if (thisNode && !thisNode.contains(e.target) && contextMenuOpen) {
        setContextMenuOpen(false);

        return;
      }
    }

    document.addEventListener("mousedown", handleClick, false);

    return function cleanup() {
      document.removeEventListener("mousedown", handleClick, false);
    };
  });

  let linkToAtom;
  if (ownerBaseUrl && atom) {
    const path = "#!/post" + `?postUri=${encodeURI(get(atom, "uri"))}`;

    linkToAtom = toAbsoluteURL(ownerBaseUrl).toString() + path;
  }

  const process = useSelector(generalSelectors.getProcessState);

  const theme = useSelector(generalSelectors.getTheme);
  const adminEmail = get(theme, "adminEmail");
  const isOwnAtom = useSelector(state =>
    generalSelectors.isAtomOwned(state, atomUri)
  );
  const isActive = atomUtils.isActive(atom);
  const isInactive = atomUtils.isInactive(atom);
  const isUsableAsTemplate = useSelector(state =>
    generalSelectors.isAtomUsableAsTemplate(state, atomUri)
  );
  const isEditable = useSelector(state =>
    generalSelectors.isAtomEditable(state, atomUri)
  );
  const atomLoading =
    !atom || processUtils.isAtomLoading(process, get(atom, "uri"));
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(process, get(atom, "uri"));

  function generateReportAtomMailParams() {
    //todo
    const subject = `[Report Atom] - ${atomUri}`;
    const body = `Link to Atom: ${linkToAtom}%0D%0AReason:%0D%0A`; //hint: %0D%0A adds a linebreak

    return `subject=${subject}&body=${body}`;
  }

  function exportPdf() {
    if (!atom) return;
    const docDefinition = wonUtils.createDocumentDefinitionFromPost(atom);

    if (docDefinition) {
      pdfMake.vfs = pdfFonts.pdfMake.vfs;
      pdfMake.createPdf(docDefinition).download();
    }
  }

  function reOpenAtom() {
    if (isOwnAtom) {
      dispatch(actionCreators.atoms__reopen(atomUri));
    }
  }

  function deleteAtom() {
    if (isOwnAtom) {
      const payload = {
        caption: "Attention!",
        text: "Deleting the Atom is irreversible, do you want to proceed?",
        buttons: [
          {
            caption: "Yes",
            callback: () => {
              dispatch(actionCreators.atoms__delete(atomUri));
              dispatch(actionCreators.view__hideModalDialog());
              history.push("/inventory");
            },
          },
          {
            caption: "No",
            callback: () => {
              dispatch(actionCreators.view__hideModalDialog());
            },
          },
        ],
      };
      dispatch(actionCreators.view__showModalDialog(payload));
    }
  }

  function closeAtom() {
    if (isOwnAtom) {
      const payload = {
        caption: "Attention!",
        text:
          "Deleting or archiving the Atom will close all connections, do you want to proceed?",
        buttons: [
          {
            caption: "Delete",
            callback: () => {
              deleteAtom();
            },
          },
          {
            caption: "Archive",
            callback: () => {
              dispatch(actionCreators.atoms__close(atomUri));
              dispatch(actionCreators.view__hideModalDialog());
            },
          },
          {
            caption: "Cancel",
            callback: () => {
              dispatch(actionCreators.view__hideModalDialog());
            },
          },
        ],
      };
      dispatch(actionCreators.view__showModalDialog(payload));
    }
  }

  const iconElement =
    atomLoading || atomFailedToLoad ? (
      <svg className="cdd__icon__small">
        <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
      </svg>
    ) : (
      <svg
        className="cdd__icon__small clickable"
        onClick={() => setContextMenuOpen(true)}
      >
        <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
      </svg>
    );

  let dropdownElement;

  if (contextMenuOpen) {
    const buttons = [];

    buttons.push(
      <button
        key="export"
        className="won-button--outlined thin red"
        onClick={() => exportPdf()}
      >
        Export as PDF
      </button>
    );
    isUsableAsTemplate &&
      buttons.push(
        <button
          key="duplicate"
          className="won-button--outlined thin red"
          onClick={() =>
            history.push(
              generateLink(
                history.location,
                {
                  fromAtomUri: atomUri,
                  mode: "DUPLICATE",
                },
                "/create"
              )
            )
          }
        >
          Post this too!
        </button>
      );
    isEditable &&
      buttons.push(
        <button
          key="edit"
          className="won-button--outlined thin red"
          onClick={() =>
            history.push(
              generateLink(
                history.location,
                {
                  fromAtomUri: atomUri,
                  mode: "EDIT",
                },
                "/create"
              )
            )
          }
        >
          Edit
        </button>
      );
    adminEmail &&
      buttons.push(
        <a
          key="report"
          className="won-button--outlined thin red"
          href={"mailto:" + adminEmail + "?" + generateReportAtomMailParams()}
        >
          Report
        </a>
      );
    isOwnAtom &&
      isInactive &&
      buttons.push(
        <button
          key="reopen"
          className="won-button--filled red"
          onClick={() => reOpenAtom()}
        >
          Reopen
        </button>
      );
    isOwnAtom &&
      isInactive &&
      buttons.push(
        <button
          key="delete"
          className="won-button--filled red"
          onClick={() => deleteAtom()}
        >
          Delete
        </button>
      );
    isOwnAtom &&
      isActive &&
      buttons.push(
        <button
          key="close"
          className="won-button--filled red"
          onClick={() => closeAtom()}
        >
          Remove
        </button>
      );

    dropdownElement = (
      <div className="cdd__contextmenu">
        <div
          className="cdd__contextmenu__content"
          onClick={() => setContextMenuOpen(false)}
        >
          <div className="topline">
            <svg className="cdd__icon__small__contextmenu clickable">
              <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
            </svg>
          </div>
          {/* Buttons when connection is available -->*/}
          {buttons}
        </div>
      </div>
    );
  }

  return (
    <won-atom-context-dropdown
      class={className ? className : ""}
      ref={node => (thisNode = node)}
    >
      {iconElement}
      {dropdownElement}
    </won-atom-context-dropdown>
  );
}
WonAtomContextDropdown.propTypes = {
  atom: PropTypes.object.isRequired,
  className: PropTypes.string,
};
