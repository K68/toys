var server = require('http').createServer()
    , express = require('express')
    , bodyParser = require('body-parser')
    , app = express()
    , port = 8668;

var WebSocket = require('ws');

app.use(bodyParser.json());

app.post('/inspect', function(req, res) {
    var ws = new WebSocket(req.body.ipath + "?token=" + req.body.token);
    var pingo = false;
    ws.on('message', function(data, flags) {
        if (!pingo) {
            pingo = true;
            var _data = JSON.parse(data);
            res.send({
                "cpu": _data[0].cpu.load_average,
                "memory": _data[0].memory.usage * 1.0 / 1024 / 1024,
                "txBytes": _data[0].network.tx_bytes
            });
            ws.close();
        }
    });
});

server.on('request', app);
server.listen(port, function () { console.log('Listening on ' + server.address().port) });
