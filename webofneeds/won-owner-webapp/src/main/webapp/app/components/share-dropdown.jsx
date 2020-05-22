import React, { useState, useEffect } from "react";

import PropTypes from "prop-types";
import WonAtomShareLink from "./atom-share-link.jsx";

import "~/style/_share-dropdown.scss";
import ico16_share from "~/images/won-icons/ico16_share.svg";

export default function WonShareDropdown({ atom, className }) {
  const [contextMenuOpen, setContextMenuOpen] = useState(false);
  let thisNode;
  useEffect(() => {
    function handleClick(e) {
      if (!thisNode.contains(e.target) && contextMenuOpen) {
        setContextMenuOpen(false);

        return;
      }
    }

    document.addEventListener("mousedown", handleClick, false);

    return function cleanup() {
      document.removeEventListener("mousedown", handleClick, false);
    };
  });

  const dropdownElement = contextMenuOpen && (
    <div className="sdd__sharemenu">
      <div className="sdd__sharemenu__content">
        <div className="topline">
          <svg
            className="sdd__icon__small__sharemenu clickable"
            onClick={() => setContextMenuOpen(false)}
          >
            <use xlinkHref={ico16_share} href={ico16_share} />
          </svg>
        </div>
        <WonAtomShareLink atom={atom} />
      </div>
    </div>
  );

  return (
    <won-share-dropdown
      class={className ? className : ""}
      ref={node => (thisNode = node)}
    >
      <svg
        className="sdd__icon__small clickable"
        onClick={() => setContextMenuOpen(true)}
      >
        <use xlinkHref={ico16_share} href={ico16_share} />
      </svg>
      {dropdownElement}
    </won-share-dropdown>
  );
}
WonShareDropdown.propTypes = {
  atom: PropTypes.object.isRequired,
  className: PropTypes.string,
};
