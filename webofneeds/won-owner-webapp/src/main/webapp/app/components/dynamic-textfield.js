/**
 * Created by ksinger on 14.09.2015.
 */
;

import angular from 'angular';
import 'angular-sanitize';
import { dispatchEvent, attach } from '../utils';

function genComponentConf() {
    let template = `
        <div class="wdt__left">
            <div class="wdt__text"
                 ng-class="{'wdt__text--placeholder' : self.displayingPlaceholder, 'wdt__text--invalid' : !self.valid()}"
                 contenteditable="true">
                 {{ ::self.placeholder }}
            </div>
            <span class="wdt__charcount" ng-show="self.maxChars">
                {{ self.maxChars - self.value.length }} Chars left
            </span>
        </div>
        <button
            class="wdt__submitbutton red"
            ng-show="::self.submitButtonLabel"
            ng-click="::self.submit()">
            {{ ::self.submitButtonLabel }}
        </button>
    `;

    const serviceDependencies = ['$scope', '$element', '$sanitize', '$sce'/*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);

            //TODO debug; deleteme
            /*
            window.dtfCtrl = this;
            window.tf = this.textField();
            window.tf2 = this.$element.find('.wdt__text');
            console.log('dynamic-textfield.js : in ctrl', this, this.$element)
            */

            window.dtf4dbg = this;

            this.displayingPlaceholder = true;
            this.value = '';

            this.textFieldNg().bind('keydown',e => this.onKeydown(e)) //prevent enter
                              .bind('keyup', () => this.input()) // handle title changes
                              .bind('focus', (e) => this.onFocus(e))
                              .bind('blur', (e) => this.onBlur(e))
                            /*
                              .bind('drop paste', (e) => {
                                  e.stopPropagation();
                                  this.sanitize();
                                  return this.input();
                              })
                              */
                              //don't want the default input event to bubble and leak into this directives event-stream
                              .bind('input', (e) => e.stopPropagation());


            /*
            *   TODO
            *    * clean up watches in destructor
            *    * force line-breaks on very long words
            *    * maxchars
            */
        }
        onKeydown(e) {
            // prevent typing enter as it causes `<div>`s in the value
            if(e.keyCode === 13) {
                this.submit();
                return false;
            }
        }
        onFocus(e) {
            this.preEditValue = this.value;
            this.clearPlaceholder();
        }
        onBlur(e) {
            this.addPlaceholderIfEmpty();
            if(this.value !== this.preEditValue) {
                const payload = {
                    value: this.value,
                    valid: this.valid()
                };
                this.onChange(payload);
                dispatchEvent(this.$element[0], 'change', payload);
                //this.$scope.$emit(eventName, payload); //bubbles through $scopes not dom
            }
        }
        submit () {
            if(this.submitButtonLabel || this.submittable) {
                const payload = {
                    value: this.value,
                    valid: this.valid()
                };
                this.onSubmit(payload);
                dispatchEvent(this.$element[0], 'submit', payload);

                // clear text
                this.setText('');
            }
        }
        input () {
            console.log('got input in dynamic textfeld ', this.getText());
            if(!this.displayingPlaceholder) {
                if(this.getUnsanitizedText() !== this.getText() ||
                    this.textField().innerHTML.match(/<br>./)) { //also supress line breaks inside the text in copy-pasted text
                        this.setText(this.getText()); //sanitize
                    }
                const newVal = this.getText().trim();
                //make sure the text field contains the sanitized text (so user sees what they're posting)
                //this.setText(newVal);

                //compare with previous value, if different
                if(this.value !== newVal) {
                    this.value = newVal;

                    // -> publish input event
                    const payload = {
                        value: this.value,
                        valid: this.valid()
                    };
                    this.onInput(payload);
                    dispatchEvent(this.$element[0], 'input', payload);
                    //this.$scope.$emit(eventName, payload); //bubbles through $scopes not dom

                    this.$scope.$digest(); //update charcount
                }
            }
        }
        clearPlaceholder() {
            if(this.displayingPlaceholder) {
                this.setText('');
                this.setDisplayingPlaceholder(false);
            }
        }
        addPlaceholderIfEmpty() {
            if(this.getText() === '') {
                console.log('onBlur - inner');
                this.setText(this.placeholder);
                this.setDisplayingPlaceholder(true);
            }
        }
        setDisplayingPlaceholder(flag) {
            this.displayingPlaceholder = flag;
            this.$scope.$digest();
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
        getUnsanitizedText() {
            return this.textField().textContent;
        }
        getText() {
            //sanitize input
            //return this.$sanitize(this.getUnsanitizedText())
            return this.getUnsanitizedText()
                .replace(/<br>/gm, ' ')
                .replace(/<(?:.|\n)*?>/gm, ''); //strip html tags

        }
        setText(txt) {
            this.textField().innerHTML = txt
        }
        valid() {
            return this.value.length < this.maxChars;
        }
        // view -> model
        // model -> view
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
export default angular.module('won.owner.components.dynamicTextfield', [
        'ngSanitize'
    ])
    .directive('wonDynamicTextfield', genComponentConf)
    .name;
