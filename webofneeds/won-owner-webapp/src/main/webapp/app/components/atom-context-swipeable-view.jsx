/**
 * created by ms on 04.11.2019
 */
import React, { useState } from "react";
import SwipeableViews from "react-swipeable-views";
import "~/style/_atom-context-layout.scss";

import PropTypes from "prop-types";

import ico16_contextmenu from "~/images/won-icons/ico16_contextmenu.svg";
import ico32_buddy_add from "~/images/won-icons/ico32_buddy_add.svg";

export default function WonAtomContextSwipeableView({
  actionButtons,
  className,
  enableMouseEvents,
  children,
}) {
  const [show, setShow] = useState(false);

  const headerElement = <div className="headerElement">{children}</div>;

  let buttons = actionButtons;

  if (actionButtons) {
    buttons = <div onClick={() => setShow(!show)}>{actionButtons}</div>;

    let triggerIcon = (
      <React.Fragment>
        <svg
          className="cl__trigger cl__trigger--waiting clickable"
          onClick={() => setShow(!show)}
        >
          <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
        </svg>
        <svg
          className="cl__trigger cl__trigger--add clickable"
          onClick={() => setShow(!show)}
        >
          <use xlinkHref={ico32_buddy_add} href={ico32_buddy_add} />
        </svg>
        <svg
          className="cl__trigger cl__trigger--default clickable"
          onClick={() => setShow(!show)}
        >
          <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
        </svg>
      </React.Fragment>
    );

    return (
      <won-atom-context-layout class={className}>
        <div className="cl__main ">
          <SwipeableViews
            index={show ? 1 : 0}
            enableMouseEvents={enableMouseEvents}
          >
            {headerElement}
            {buttons}
          </SwipeableViews>
        </div>
        {triggerIcon}
      </won-atom-context-layout>
    );
  } else {
    return headerElement;
  }
}
WonAtomContextSwipeableView.propTypes = {
  className: PropTypes.string,
  actionButtons: PropTypes.any,
  enableMouseEvents: PropTypes.bool,
  children: PropTypes.any.isRequired,
};
