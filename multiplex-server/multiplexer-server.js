var port = 8080;
if(process.argv.length > 2 && typeof process.argv[2] != 'undefined' && !isNaN(process.argv[2])) {
    port = Number(process.argv[2]);
}

var io = require('socket.io').listen(port);
console.log('listening on Port: ' + port);

var slideIndeces = { h: 0, v: 0, f:-1 };
var codeIsHighlighted = false;
io.sockets.on('connection', function (socket) {
    // sync up any new clients
    socket.emit('sync', slideIndeces);
    if(codeIsHighlighted) {
        socket.emit('toggleHighlightCode');
    }

    // if the master has connected, use her slide Indeces and re-sync everybody up.
    socket.on('masterSync', function(data){
        slideIndeces = data;
        io.sockets.emit('sync', slideIndeces);
    });
    socket.on('masterToggleHighlightCode', function(){
        codeIsHighlighted = !codeIsHighlighted;
        io.sockets.emit('toggleHighlightCode');
    });

    // login the Master
    socket.on('masterLogin', function(data){
        // not very secure lol.
        if(data == 'cs780') {
            socket.emit('masterLoginSuccess');
            io.sockets.emit('masterActive');

            socket.on('disconnect', function(){
                io.sockets.emit('masterInactive');
            });
        }
    });
});