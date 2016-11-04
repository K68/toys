var server = require('http').createServer()
    , express = require('express')
    , bodyParser = require('body-parser')
    , app = express()
    , port = 8668;

var WebSocket = require('ws');
var request = require('request');
var URL = process.argv[2];

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

app.get('/assets/stylesheets/main.css', function(req, res) {
    request({
        url: 'https://' + URL + '/assets/stylesheets/main.css',
        "rejectUnauthorized": false
    }).pipe(res)
});

app.get('/astate', function(req, res) {
    request({
        url: 'https://' + URL + '/agent_console?agentId=' + req.query.id + '&pswd=' + req.query.ps,
        "rejectUnauthorized": false
    }, function (error, response, body) {
        if (!error) {
            res.send(body)
        } else {
            res.send(error.toString())
        }
    })
});

app.get('/cstate', function(req, res) {
    request({
        url: 'https://' + URL + '/c_info?cid=' + req.query.id + '&cname=' + req.query.nm,
        "rejectUnauthorized": false
    }, function (error, response, body) {
        if (!error) {
            res.send(body)
        } else {
            res.send(error.toString())
        }
    })
});

server.on('request', app);
server.listen(port, function () { console.log('Listening on ' + server.address().port) });
