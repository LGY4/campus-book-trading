const net = require('net')

function findFreePort() {
  return new Promise((resolve, reject) => {
    const server = net.createServer()
    server.listen(0, '127.0.0.1', () => {
      const port = server.address().port
      server.close(() => resolve(port))
    })
    server.on('error', reject)
  })
}

findFreePort().then(port => {
  console.log(port)
}).catch(() => {
  console.log(8080)
})
