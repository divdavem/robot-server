# Robot server

This repository contains a tool which allows JavaScript scripts running in a browser
to generate keyboard and mouse events at the operating system level.

This is especially useful when writing tests for your application, to simulate
actions from the user.

It has the same API as the [Selenium Java Robot](https://github.com/attester/selenium-java-robot),
but it does not use Selenium at all. It is implemented as a small HTTP server which listens for incoming XHR
requests and executes the corresponding mouse and keyboard actions.

## Installing this tool

Before installing this tool, please make sure [node.js](http://nodejs.org/), [npm](https://www.npmjs.org/doc/README.html)
and [Java](https://java.com/download) are installed on your computer.

Then, this tool can be installed with the following command line:

```
npm install -g robot-server
```

## Command line usage

Execute the `robot-server` command:

```
robot-server
```

Here is the list of accepted options:

**`--host <host>`**

Replace `<host>` with the host name or IP address to bind the server to.

**`--port <port>`**

Replace `<port>` with the port to bind the server to.

**`--help`**

If this option is present, the list of accepted options is displayed and the *Robot server* exits immediately.

**`--version`**

If this option is present, the version of the *Robot server* is displayed and the *Robot server* exits immediately.

## JavaScript API

Once the *Robot server* is loaded, it is possible to use its API from a web page by including a script tag similar to the following one
(this may have to be adapted depending on the host and port the robot server is bound to, as specified on the command line):

```html
<script src="http://localhost:7778/robot"></script>
```

This script tag creates a JavaScript global object called `SeleniumJavaRobot`.
(This name is used for compatibility with the [Selenium Java Robot](https://github.com/attester/selenium-java-robot))
This object contains some methods which can be called to simulate keyboard and mouse events.

### Callback

Each method on the `SeleniumJavaRobot` object accepts a callback as its last parameter, to be notified when
the corresponding operation is done. When the callback is provided (which is optional), it is expected to
be either a simple function, or an object with the following structure:

```js
{
   fn: function (response, args) { /* ... */ }, // function to be called when the operation is done.
   scope: window, // object to be available as this in the callback function
   args: { /* something */ } // second argument passed to the callback function
}
```

Here is the structure of the `response` object passed in the callback as the first argument:

```js
{
   success: true, // true if there was no problem during the execution of the method, false otherwise
   result: null // if success is true, this is the result of the method (currently only relevant for getOffset)
   // if success is false, result contains a string with the error message
}
```

### List of methods

You can find in this section the description of the methods available on the `SeleniumJavaRobot` object.
Note that most of those methods are simply a bridge to the corresponding method in the
[Java Robot](http://docs.oracle.com/javase/6/docs/api/java/awt/Robot.html).

* `getOffset (callback: Callback)`

This method triggers a calibration of the robot and then returns the coordinates of the top left corner of
the viewport in the screen, as detected during the calibration phase.

```js
SeleniumJavaRobot.getOffset({
   fn: function (response) {
      if (response.success) {
         var coordinates = response.result;
         alert("The coordinates of the viewport in the screen are: " + coordinates.x + "," + coordinates.y);
      }
   }
})
```

* `mouseMove (x: Number, y: Number, callback: Callback)`

Instantly moves the mouse to the specified `x`, `y` screen coordinates.

* `smoothMouseMove (fromX: Number, fromY: Number, toX: Number, toY: Number, duration: Number, callback: Callback)`

Instantly moves the mouse to the specified `fromX`, `fromY` screen coordinates, then smoothly moves the mouse
from there to the `toX`, `toY` screen coordinates. The duration of the move must be expressed in milliseconds.

* `mousePress (buttons: Number, callback: Callback)`

Presses one or more mouse buttons. The mouse buttons should be released using the mouseRelease method.
The `buttons` parameter can be a combination (with the logical OR operator `a | b`) of one or more of the following flags:

```js
var BUTTON1_MASK = 16;
var BUTTON2_MASK = 8;
var BUTTON3_MASK = 4;
```

For example, to press both the button 1 and button 2 of the mouse at the same time, call:

```js
SeleniumJavaRobot.mousePress(16 | 8);
```

* `mouseRelease (buttons: Number, callback: Callback)`

Releases one or more mouse buttons.

* `mouseWheel (amount: Number, callback: Callback)`

Rotates the scroll wheel on wheel-equipped mice.

The `amount` parameter is the number of "notches" to move the mouse wheel Negative values indicate movement up/away from the user,
positive values indicate movement down/towards the user.

* `keyPress (keyCode: Number, callback: Callback)`

Presses a given key. The key should be released using the keyRelease method.
Valid key codes are the constants starting with `VK_` as listed in
[this Java documentation](http://docs.oracle.com/javase/6/docs/api/constant-values.html#java.awt.event.KeyEvent.VK_0).

* `keyRelease (keyCode: Number, callback: Callback)`

Releases a given key.

## How to recompile this tool

Before recompiling this tool, you need [a Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
and [Maven](http://maven.apache.org/) to be installed on your computer.

Then you can simply execute the following command from the root directory of your clone
of this repository:

```
npm install
```

This will install dependencies and compile the *Robot server* tool.

## How it is implemented

The [Java Robot class](http://docs.oracle.com/javase/6/docs/api/java/awt/Robot.html) is used to send keyboard and
mouse events to the operating system, and to perform the screen capture for the calibration.

For more information about the implementation, do not hesitate to have a look at the source code in this repository.

## License

[Apache License 2.0](LICENSE)
