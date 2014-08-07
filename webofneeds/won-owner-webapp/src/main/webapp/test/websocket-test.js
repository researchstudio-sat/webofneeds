/**
 * Created by fsalcher on 06.08.2014.
 */

var socket;


var connect = function () {

    var url = document.getElementById("targetAddress").value;

    var options = {debug: true};

    socket = new SockJS(url, null, options);

    socket.onopen = function () {
        writeOutput("connection has been established!");
    }

    socket.onmessage = function (event) {
        writeOutput('Received data: ' + event.data);
    };

    socket.onclose = function () {
        writeOutput('Lost connection!');
    };
};

var sendMessage = function () {

    var message = document.getElementById("messageText").value;

    socket.send(message);
}

var closeConnection = function () {
    socket.close;
}

var writeOutput = function (output) {
    var outputDIV = document.getElementById("output");
    outputDIV.value = outputDIV.innerHTML + output + "<br />";
}
