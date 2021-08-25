// ==UserScript==
// @name        CDS Financials
// @namespace   http://tampermonkey.net/
// @version     0.1
// @description Authentication
// @match       http*://*/auth-login-stub/gg-sign-in*
// @grant       none
// @updateURL   to be added after merging to master for correct URL
// ==/UserScript==

(function() {
    'use strict';

    prePopulateFields();

    document.getElementById('global-header').appendChild(quickSubmit());
})();

function prePopulateFields() {
    document.getElementsByName("redirectionUrl")[0].value = getBaseUrl() + "/customs/payment-records";
    document.getElementById("affinityGroupSelect").selectedIndex = 1;
    document.getElementsByName("enrolment[0].name")[0].value = "HMRC-CUS-ORG";
    document.getElementById("input-0-0-name").value = "EORINumber";
    document.getElementById("input-0-0-value").value = "GB744638982000";
    document.getElementsByName("itmp.dateOfBirth")[0].value = "1990-01-01";
}

function quickSubmit() {
    const button = document.createElement('button');
    button.innerHTML = 'Quick Submit';
    button.onclick = () => document.getElementsByClassName('button')[0].click();
    button.setAttribute('style', 'position: fixed; top: 10px; left: 20px;');
    return button;
}

function getBaseUrl() {
    let host = window.location.host;
    if (window.location.hostname === 'localhost') {
        host = 'localhost:9876'
    }
    return window.location.protocol + "//" + host;
}
