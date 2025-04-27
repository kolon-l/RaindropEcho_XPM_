const fs = require('fs');
const http = require('http');
const url = require('url');

// 按需引入实现加解密的js
require('./muban_ende.js');

// 自定义调用写好的加解密函数
function encryptFunction(data) {
  return encrypt(data);
}

function decryptFunction(data) {
  return decrypt(data);
}

function decryptFunction_RB(data) {
  return decrypt(data);
}

// 以下不用动
const USAGE = `
Usage:
  node script.js server [port]
  node script.js encrypt_c|decrypt_c [input|inputfile]
  node script.js encrypt|decrypt [inputfile] [outputfile]
`;
function validateArguments() {
  if (process.argv.length < 4) {
    console.error(USAGE.trim());
    process.exit(1);
  }
}

// 服务器模式处理
function handleServerMode(port) {
  const server = http.createServer(async (req, res) => {
    const { pathname } = url.parse(req.url);
    let requestBody = '';

    try {
      for await (const chunk of req) {
        requestBody += chunk;
      }
      if (pathname.endsWith('/encrypt')) {
        const encrypted = encryptFunction(requestBody);
        console.log("encrypt:");
        console.log(requestBody);
        console.log(encrypted);
        res.end(encrypted);
      } else if (pathname.endsWith('/decrypt')) {
        const decrypted = decryptFunction(requestBody);
        console.log("decrypt:");
        console.log(requestBody);
        console.log(decrypted);
        res.end(decrypted);
      } else if (pathname.endsWith('/decrypt_RB')) {
        const decrypted = decryptFunction_RB(requestBody);
        console.log("decrypt_RB:");
        console.log(requestBody);
        console.log(decrypted);
        res.end(decrypted);
      } else {
        res.statusCode = 404;
        console.log('Path Error:' + pathname)
        res.end('Path Not Found');
      }
    } catch (error) {
      console.error('Request handling error:', error);
      res.statusCode = 500;
      res.end('Internal Server Error');
    }
  });
  server.listen(port, () => {
    console.log(`Server running on port ${port}`);
  });
}

// 命令行加密/解密处理
function handleCLICommand(mode, input, outputFile) {
  try {
    let result = '';

    if (mode.startsWith('encrypt')){result = encryptFunction(input)}
    if (mode.startsWith('decrypt')){result = decryptFunction(input)}
    if (mode.startsWith('decrypt_RB')){result = decryptFunction_RB(input)}

    if (outputFile) {
      fs.writeFileSync(outputFile, result, 'utf8');
    } else {
      console.log(result);
    }
  } catch (error) {
    console.error(`Error processing ${mode}:`, error);
    process.exit(1);
  }
}

function main() {
  validateArguments();

  const [mode, arg1, arg2] = process.argv.slice(2);

  switch (mode) {
    case 'server':
      handleServerMode(Number(arg1));
      break;

    case 'encrypt_c':
    case 'decrypt_c':
    case 'decrypt_c_RB':
      handleCLICommand(mode, arg1);
      break;

    case 'encrypt':
    case 'decrypt':
    case 'decrypt_RB':
      handleCLICommand(mode, fs.readFileSync(arg1, 'utf8'), arg2);
      break;

    default:
      console.error(`Unknown mode: ${mode}`);
      console.error(USAGE.trim());
      process.exit(1);
  }
}

main();