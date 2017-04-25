var ld38 = require('../out/production/ld38/index')
var wallSquare = {
    role: 'WALL',
    assoc: 'ENGINEERING',
    team: -1
}
var squares = []
for (var i = 0; i < 25; i++) {
    squares.push(wallSquare)
}

var gs = ld38.GameState([
    // Chars
    { id: "test", name: "lt test", x: 2, y: 2, type: "ENGINEER", team: 0, health: 30 }
], {
    // Board
    dimX: 5,
    dimY: 5,
    squares: squares,
    doors: []
})
console.log(gs)

