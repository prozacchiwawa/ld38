var ld38 = require('../out/production/ld38/index');
var qfs = require('q-io/fs');
var PcgRandom = require('pcg-random');
var readline = require('readline');
var Bacon = require('baconjs');

var randomSeed = 12345;
var rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});
var seedDate = new Date();
seedDate.setTime(randomSeed);
var randomizer = new PcgRandom(seedDate);

// Game runner interface
var runGame = new Bacon.Bus();
function sendLine(l) {
    runGame.push({type: 'readline', data: l});
}

runGame.scan({}, function(seed,event) {
    var res = seed;
    console.log('event',event);
    if (event.type === 'start') {
        res = event.data;
        if (res.hopper.length == 0) {
            rl.question("> ", sendLine);
        }
    } else if (event.type === 'doneauto') {
        res.hopper = [];
        rl.question("> ", sendLine);
    } else if (event.type === 'runauto') {
        if (res.running) {
            res.running = setTimeout(function() { runGame.push({type: 'runauto'}); }, 100);
            res.state = ld38.run(res.state, 0.1);
            console.log(ld38.showBoard(res.state));
        }
    } else if (event.type === 'readline') {
        if (res.running) {
            clearTimeout(res.running);
            res.running = false;
        }
        else if (res.phase === 'yourturn') {
            if (event.data === 'end') {
                // Take turn
                res.remaining = {};
            } else if (event.data === 'list') {
                // Show who we have
                for (var k in res.remaining) {
                    rl.write(' ' + k + '\n');
                    for (var l in res.remaining[k]) {
                        rl.write('  ' + l + ': ' + res.remaining[k][l]+'\n');
                    }
                }
            } else if (event.data === 'board') {
                rl.write(ld38.showBoard(res.state)+'\n');
	        } else if (event.data.indexOf('pathfind') === 0) {
		        var coords = event.data.split(' ');
		        coords.shift();
                var path = ld38.pathfind(res.state, parseInt(coords[0]), parseInt(coords[1]), parseInt(coords[2]), parseInt(coords[3]));
                console.log(path);
            } else if (event.data === 'run') {
                res.running = setTimeout(function() { runGame.push({type: 'runauto'}); }, 100);
            } else {
                var split = event.data.split('@')
                var chname = split[0].trim()
                delete res.remaining[chname]
                
                var newState = ld38.execute(res.state,event.data);
                if (typeof newState === 'string') {
                    rl.write('error: ' + newState + '\n')
                } else {
                    res.state = newState;
                }
            }
        } else {
            rl.write('waiting...');
        }
        if (res.hopper.length == 0) {
            rl.question("> ", sendLine);
        }
    }
    return res;
}).onValue(function() { });

function charSetFromNames(ch) {
    var res = {};
    for (var i = 0; i < ch.length; i++) {
        res[ch[i].name] = ch[i];
    }
    return res;
}
             
// Load board
qfs.read(process.argv[2], 'r').then(function(text) {
    ld38.setRandom(function() { return randomizer.number(); });
    var lines = text.split("\n").map(function(l) { return l.trim(); }).filter(function(t) { return t.length > 0; });
    return ld38.simpleBoardConvert(lines);
}).then(function(state) {
    state = {
        hopper: [],
        phase: 'yourturn',
        state: state,
        remaining: charSetFromNames(ld38.getCharList(0,state))
    };
    if (process.argv.length > 3) {
        return qfs.read(process.argv[3], 'r').then(function(text) {
            state.hopper = text.split("\n").map(function(l) { return l.trim(); }).filter(function(t) { return t.length > 0 && t[0] != '#'; });
            return state;
        })
    } else {
        return state;
    }
}).then(function(state) {
    console.log("start");
    runGame.push({
        type: 'start',
        data: state
    });
    for (var i = 0; i < state.hopper.length; i++) {
        runGame.push({type: 'readline', data: state.hopper[i]});
    }
    runGame.push({type: 'doneauto'});
}).catch(function(e) {
    console.error("Error:",e);
    console.log('Usage: test.js [board]');
});
