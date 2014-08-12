/**
 * Created by fsalcher on 06.08.2014.
 */

var socket;


var connect = function () {

    var url = document.getElementById("targetAddress").value;

    var options = {debug: true};

    socket = new SockJS(url, null, options);

    socket.onopen = function () {
        console.log("connection has been established!")
        writeOutput("connection has been established!");
    }

    socket.onmessage = function (event) {
        console.log("Received data: "+event.data);

        writeOutput('Received data: ' + event.data);
    };

    socket.onclose = function () {
        console.log("Lost connection")
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
