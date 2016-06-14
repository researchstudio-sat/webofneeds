/**
 * Created by ksinger on 14.06.2016.
 */


;

import angular from 'angular';
import Immutable from 'immutable';
import 'angular-sanitize';
import { dispatchEvent, attach } from '../utils';

function genComponentConf() {
    let template = `
        <div class="wdt__left">
            <input type="text"/>
            <div class="wdt__text"
                 ng-class="{'wdt__text--placeholder' : self.displayingPlaceholder, 'wdt__text--invalid' : !self.valid()}"
                 contenteditable="true">
                 <p ng-repeat="lineOfText in self.state.get('text').toJS()">
                    {{ lineOfText }}
                 </p>
            </div>
            <span class="wdt__charcount" ng-show="self.maxChars">
                {{ self.charactersLeft() }} characters left
            </span>
        </div>
        <button
            class="wdt__submitbutton red"
            ng-show="::self.submitButtonLabel"
            ng-click="::self.submit()">
            {{ ::self.submitButtonLabel }}
        </button>
    `;

    //TODO move down to constructor
    const initialState4dbg = Immutable.fromJS({
        text: ["<placeholder text here>"],
        isPlaceholder: true,
        selectionStartLine: -1,
        selectionStartChar: -1,
        selectionEndLine: -1,
        selectionEndChar: -1,
    });

    const debugState = Immutable.fromJS({
        text: ["A title so textiful and long", "Lorem ipsum 2nd mies siet dolor", "More text for a 3rd line."],
        isPlaceholder: false,
        selectionStartLine: 1,
        selectionStartChar: 5,
        selectionEndLine: 1,
        selectionEndChar: 7,
    });

    const serviceDependencies = ['$scope', '$element', '$sanitize', '$sce'/*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);

            window.dt4dbg = this;

            this.state = debugState;

            /* mustn't cancel further processing of keydown/keyup
            * as this will break non-latin input schemes and shortcuts
            * like ctrl+v.
             */
            //this.textFieldNg().bind('keydown keyup focus blur drop paste input', e => this.update(e));
            this.textFieldNg().bind('focus blur drop paste input', e => this.update(e));
            /*
            for(let type in ['keydown', 'keyup', 'focus', 'blur', 'drop', 'paste', 'input'] ) {
                this.textFieldNg().bind(type, e => this.update(type, e));
            }
            this.textFieldNg().bind('keydown',e => console.log('qwerqwerqwr')) //prevent enter
                .bind('keyup', () => console.log('asdfasdfasdf')) // handle title changes
                .bind('focus', (e) => this.update('focus', e))
                */

            /* TODO actions:
             * - moving caret left/right
             * - typing
             * - selecting
             * - pasting
             * - copying
             */

        }

        update(e) {
            console.log('got event on dt: ', e.type,  e);


            switch(e.type) {
                case 'keydown':
                    e.originalEvent.

                    break;
            }






            this.render(); //just trigger angular if the ngBind doesn't already

            //cancel further processing
            e.stopPropagation();
            return false;
        }
        render() {

            //const html = `${this.state.text[0]}`

            //this.textField().innerHTML = html;
        }

        onKeydown(e) {

        }
        onFocus(e) {
            console.log('dt: ', this.textField(), this.textField().selectionStart, window.selectionStart);

        }
        onBlur(e) {

        }
        input(e) {

        }
        textFieldNg() {
            if(!this._textField) {
                this._textField = this.$element.find('.wdt__text');
            }
            return this._textField;
        }
        textField() {
            return this.textFieldNg()[0];
        }
    }
    Controller.$inject = serviceDependencies;


    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            placeholder: '=',
            maxChars: '=',
            /*
             * Usage:
             *  on-input="::myCallback(value, valid)"
             */
            onInput: '&',
            /*
             * Usage:
             *  on-change="::myCallback(value, valid)"
             */
            onChange: '&',

            submitButtonLabel: '=',
            /*
             * Usage:
             *  on-submit="::myCallback(value)"
             */
            onSubmit: '&',

            /*
             * if you don't specify a submit-button-label
             * set this flag to true to enable submit-events.
             */
            submittable: '='

        },
        template: template
    }
}


export default angular.module('won.owner.components.dynamicTextfieldRdxd', [ ])
    .directive('wonDynamicTextfieldRdxd', genComponentConf)
    .name;


/**
 * adapted from http://stackoverflow.com/questions/6139107/programatically-select-text-in-a-contenteditable-html-element
 * @param el
 */
function selectElementContents(el, start, end) {
    var range = document.createRange();
    range.selectNodeContents(el);
    var sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
}

// tf.childNodes => [DOM/String] ["What?"]
// tf.firstChild
/*
 range.setStart(tf.firstChild, 0) "What?"
 range.setEnd(tf.firstChild, 2)
 sel.removeAllRanges()
 sel.addRange(range)
 selects `Wha`


 https://developer.mozilla.org/en-US/docs/Web/API/Selection
 s = window.getSelection()
 s.anchorNode and s.anchorOffset give start
 s.focusOffset is end/cursor-position
 s.isCollapsed (just cursor, no range-selection)
 s.rangeCount > 1 => user has used ctrl-select to select more than one range of text
 s.baseNode?
 s.extendNode?
 s.toString()

 */