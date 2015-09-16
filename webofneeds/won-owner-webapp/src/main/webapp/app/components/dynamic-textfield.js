/**
 * Created by ksinger on 14.09.2015.
 */
;

import angular from 'angular';
import 'angular-sanitize';
import { dispatchEvent, attach } from '../utils';

function genComponentConf() {
    let template = `
        <div class="wdt__text"
             ng-class="{'wdt__text--placeholder' : self.displayingPlaceholder, 'wdt__text--invalid' : !self.valid()}"
             contenteditable="true">
             {{::self.placeholder}}
        </div>
        <span class="wdt__charcount" ng-show="self.maxChars">
            {{self.maxChars - self.value.length}} Chars left
        </span>
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


            this.displayingPlaceholder = true;
            this.value = '';

            this.textFieldNg().bind('focus', (e) => this.onFocus(e))
                              .bind('blur', (e) => this.onBlur(e))
                              .bind('keyup drop paste', () => this.input())
            //this.textField().addEventListener('keyup', () => this.input());
            //this.textField().addEventListener('drop', () => this.input());
            //this.textField().addEventListener('paste', () => this.input());

            //don't want the default input event to bubble and leak into this directives event-stream
                              .bind('input', (e) => e.stopPropagation());
            //this.textField().addEventListener('input', (e) => e.stopPropagation());

            /*
            *   TODO
            *    * clean up watches in destructor
            *    * force line-breaks on very long words
            *    * maxchars
            */
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
        input () {
            if(!this.displayingPlaceholder) {
                const newVal = this.getText();

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
        getText() {
            //sanitize input
            return this.$sanitize(this.textField().innerHTML)
                .replace(/<br\/?>/g, '\n')
                .trim();
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
             *  on-input="myCallback(value)"
             */
            onInput: '&',
            /*
             * Usage:
             *  on-input="myCallback(value)"
             */
            onChange: '&',
        },
        template: template
    }
}
export default angular.module('won.owner.components.dynamicTextfield', [
        'ngSanitize'
    ])
    .directive('wonDynamicTextfield', genComponentConf)
    .name;
