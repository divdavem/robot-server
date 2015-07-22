/*
 * Copyright 2015 Amadeus s.a.s.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function () {
    var SERVER_URL = '';

    var HttpRequestObject = window.XMLHttpRequest;
    var newHttpRequestObject;
    if (HttpRequestObject) {
        newHttpRequestObject = function () {
            return new HttpRequestObject();
        };
    } else {
        HttpRequestObject = window.ActiveXObject;
        newHttpRequestObject = function () {
            return new HttpRequestObject("Microsoft.XMLHTTP");
        };
    }

    var SeleniumJavaRobot = window.SeleniumJavaRobot = {};

    var callCallback = function (curCallback, success, result) {
        if (typeof curCallback == "function") {
            curCallback = {
                fn : curCallback
            };
        }
        if (curCallback && typeof curCallback.fn == "function") {
            curCallback.fn.call(curCallback.scope, {
                success : success,
                result : result
            }, curCallback.args);
        }
    };

    var slice = Array.prototype.slice;
    var createFunction = function (name, argsNumber) {
        return SeleniumJavaRobot[name] = function () {
            var data = JSON.stringify(slice.call(arguments, 0, argsNumber));
            var callback = arguments[argsNumber];

            var xhr = newHttpRequestObject();
            xhr.open('POST', SERVER_URL + "/" + name, true);
            xhr.send(data);
            xhr.onreadystatechange = function () {
                if (xhr && xhr.readyState == 4) {
                    // Considers 2xx and 304 as a success, anything else as an error
                    var status = xhr.status, success = (status >= 200 && status < 300) || status == 304;
                    var res = xhr.responseText;
                    if (success) {
                        callCallback(callback, true, JSON.parse(res));
                    } else {
                        callCallback(callback, false, (res || "Unknown error: " + status) + " when calling '"
                                + SERVER_URL + "/" + name + "' with: " + data);
                    }
                    // clean the closure:
                    data = xhr = callback = null;
                }
            };
        };
    };

    createFunction("mouseMove", 2);
    createFunction("smoothMouseMove", 5);
    createFunction("mousePress", 1);
    createFunction("mouseRelease", 1);
    createFunction("mouseWheel", 1);
    createFunction("keyPress", 1);
    createFunction("keyRelease", 1);
    createFunction("calibrate", 2);

    SeleniumJavaRobot.getOffset = function (callback) {
        var div = document.createElement("div");
        var border = 30;
        div.style.cssText = "display:block;position:absolute;background-color:rgb(255, 0, 0);border:" + border
                + "px solid rgb(100, 100, 100);left:0px;top:0px;right:0px;bottom:0px;z-index:999999;";
        document.body.appendChild(div);
        // wait some time for the browser to display the element
        setTimeout(function () {
            SeleniumJavaRobot.calibrate(div.offsetWidth - 2 * border, div.offsetHeight - 2 * border, function (response) {
                div.parentNode.removeChild(div);
                var result = response.result;
                if (response.success) {
                    result = {
                        x : result.x - border,
                        y : result.y - border
                    };
                }
                callCallback(callback, response.success, result);
            });
        }, 200);
    };

})();
