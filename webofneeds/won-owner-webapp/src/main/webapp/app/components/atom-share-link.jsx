import React, { useState } from "react";

import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import { get, toAbsoluteURL } from "../utils.js";
import { ownerBaseUrl } from "~/config/default.js";
import * as wonUtils from "../won-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";

import "~/style/_atom-share-link.scss";
import ico16_copy_to_clipboard from "~/images/won-icons/ico16_copy_to_clipboard.svg";
import ico16_checkmark from "~/images/won-icons/ico16_checkmark.svg";

export default function WonAtomShareLink({ atom, className }) {
  const [showLink, setShowLink] = useState(true);
  const [copied, setCopied] = useState(false);
  const atomUri = get(atom, "uri");
  let linkToPost;
  if (ownerBaseUrl && atom) {
    const path = "#!/post" + `?postUri=${encodeURI(atomUri)}`;

    linkToPost = toAbsoluteURL(ownerBaseUrl).toString() + path;
  }
  let svgQrCodeToPost = wonUtils.generateSvgQrCode(linkToPost);

  const hasConnections = get(atom, "connections")
    ? get(atom, "connections").size > 0
    : false;
  const isOwned = useSelector(state =>
    generalSelectors.isAtomOwned(state, atomUri)
  );

  let linkInput;

  function copyLink() {
    const linkEl = linkInput;
    if (linkEl) {
      linkEl.focus();
      linkEl.setSelectionRange(0, linkEl.value.length);
      if (!document.execCommand("copy")) {
        window.prompt(
          "Something went wrong while automatically copying the link. Copy to clipboard: Ctrl+C",
          linkEl.value
        );
      } else {
        linkEl.setSelectionRange(0, 0);
        linkEl.blur();

        //Temprorarily show checkmark
        setCopied(true);
        setTimeout(() => {
          setCopied(false);
        }, 1000);
      }
    }
  }

  function selectLink() {
    const linkEl = linkInput;
    if (linkEl) {
      linkEl.setSelectionRange(0, linkEl.value.length);
    }
  }

  function clearSelection() {
    const linkEl = linkInput;
    if (linkEl) {
      linkEl.setSelectionRange(0, 0);
    }
  }

  const labelElement = ((hasConnections && isOwned) || !isOwned) && (
    <p className="asl__text">
      Know someone who might also be interested in this atom? Consider sharing
      the link below in social media.
    </p>
  );

  const linkElement = showLink ? (
    <div className="asl__link">
      <div className="asl__link__copyfield">
        <input
          ref={ref => (linkInput = ref)}
          className="asl__link__copyfield__input"
          value={linkToPost}
          readOnly
          type="text"
          onFocus={() => selectLink()}
          onBlur={() => clearSelection()}
        />
        <button
          className="red won-button--filled asl__link__copyfield__copy-button"
          onClick={() => copyLink()}
        >
          <svg className="asl__link__copyfield__copy-button__icon">
            {copied ? (
              <use xlinkHref={ico16_checkmark} href={ico16_checkmark} />
            ) : (
              <use
                xlinkHref={ico16_copy_to_clipboard}
                href={ico16_copy_to_clipboard}
              />
            )}
          </svg>
        </button>
      </div>
    </div>
  ) : (
    <div className="asl__qrcode">
      <img
        className="asl__qrcode__code"
        src={"data:image/svg+xml;utf8," + svgQrCodeToPost}
      />
    </div>
  );

  return (
    <won-atom-share-link class={className ? className : ""}>
      <div className="asl__content">
        {labelElement}
        <div className="asl__tabs">
          <div
            className={
              "asl__tabs__tab clickable " +
              (showLink ? " asl__tabs__tab--selected " : "")
            }
            onClick={() => setShowLink(true)}
          >
            Link
          </div>
          <div
            className={
              "asl__tabs__tab clickable " +
              (!showLink ? " asl__tabs__tab--selected " : "")
            }
            onClick={() => setShowLink(false)}
          >
            QR-Code
          </div>
        </div>
        {linkElement}
      </div>
    </won-atom-share-link>
  );
}
WonAtomShareLink.propTypes = {
  atom: PropTypes.object.isRequired,
  className: PropTypes.string,
};
