const fs = require('fs');

// 隐藏警告（来自 CyberChef 内部循环依赖）
process.noDeprecation = true;
process.noWarnings = true;

// CyberChef异步导入
let chef = null;

// 初始化CyberChef
async function initCyberChef() {
    if (!chef) {
        chef = await require('cyberchef');
    }
    return chef;
}

// 需要自拟的部分---------------------------------------------------------------
// require('./muban_ende.js');

// 供burp调用的加密函数的加载方式,可用于进一步处理加密结果;入参为打标记字段
async function encryptFunction(data) {
    const c = await initCyberChef();
    const result = c.bake(data, [
  { "op": "SM4 Encrypt",
    "args": [{ "option": "UTF8", "string": "00013ayr35fnlflv" }, { "option": "Hex", "string": "" }, "ECB", "Raw", "Hex"] },
  { "op": "findReplace",
    "args": [{ "option": "Regex", "string": " " }, "", true, false, true, false] }
]);
    return result.toString();
}

// 供burp调用的解密函数的加载方式;入参为响应体，默认原文返回不做处理
async function decryptFunction(data) {
    const c = await initCyberChef();
    const result = c.bake(data, [
  { "op": "From Hex",
    "args": ["Auto"] },
  { "op": "SM4 Decrypt",
    "args": [{ "option": "UTF8", "string": "00013ayr35fnlflv" }, { "option": "Hex", "string": "" }, "ECB", "Raw", "Raw"] }
]);
    return result.toString();
}

// 供burp右键解密功能的加载方式;入参为密文,默认直接调用解密函数
async function decryptFunction_RB(data) {
    const c = await initCyberChef();
    const result = c.bake(data, [
  { "op": "From Hex",
    "args": ["Auto"] },
  { "op": "SM4 Decrypt",
    "args": [{ "option": "UTF8", "string": "00013ayr35fnlflv" }, { "option": "Hex", "string": "" }, "ECB", "Raw", "Raw"] }
]);
    return result.toString();
}

// 下面代码不要动---------------------------------------------------------------
if (process.argv.length < 4) {
    console.log("Usage: node script.js [server] [port]");
    console.log("Usage: node script.js [encrypt_c|decrypt_c] [input]");
    console.log("Usage: node script.js [encrypt|decrypt] [inputfile] [outputfile]");
    process.exit(1);
}

const mode = process.argv[2];

// 主异步执行函数
(async function() {
    if (process.argv.length == 4) {
        let input = process.argv[3];
        let output;
        switch (mode) {
            case 'server':
                var http = require('http');
                const url = require('url');
                const querystring = require('querystring');
                http.createServer(function (req, res) {
                    let path = url.parse(req.url);
                    let postparms = '';
                    if (path.pathname.endsWith('/encrypt')) {
                        console.log("Encrypt:");
                        req.on('data', (parms) => {
                            postparms += parms;
                        });
                        req.on('end', async () => {
                            console.log(postparms);
                            const result = await encryptFunction(postparms);
                            console.log(result);
                            res.end(result);
                        })
                    } else if (path.pathname.endsWith('/decrypt')) {
                        console.log("Decrypt:")
                        req.on('data', (parms) => {
                            postparms += parms
                        })
                        req.on('end', async () => {
                            console.log(postparms);
                            const dec = await decryptFunction(postparms);
                            console.log(dec);
                            res.end(dec);
                        })
                    } else if (path.pathname.endsWith('/decrypt_RB')) {
                        console.log("Decrypt RightButton:")
                        req.on('data', (parms) => {
                            postparms += parms
                        })
                        req.on('end', async () => {
                            console.log(postparms);
                            const dec = await decryptFunction_RB(postparms);
                            console.log(dec);
                            res.end(dec);
                        })
                    } else {
                        res.write("end");
                        res.end()
                    }
                }).listen(input);
                break;
            case 'encrypt_c':
                output = await encryptFunction(input);
                console.log(output);
                process.exit(0);
                break;
            case 'decrypt_c':
                output = await decryptFunction(input);
                console.log(output);
                process.exit(0);
                break;
            case 'decrypt_c_RB':
                output = await decryptFunction_RB(input);
                console.log(output);
                process.exit(0);
                break;
            default:
                console.error(`Unknown mode: ${mode}`);
                process.exit(1);
        }
    } else if (process.argv.length > 4) {
        const mode = process.argv[2];
        let inputFile = process.argv[3];
        let outputFile = process.argv[4];
        let inputData, outputData;
        switch (mode) {
            case 'encrypt':
                try {
                    inputData = fs.readFileSync(inputFile, 'utf8');
                } catch (err) {
                    console.error(`Error reading input file: ${inputFile}`, err);
                    process.exit(1);
                }
                outputData = await encryptFunction(inputData);
                break;
            case 'decrypt':
                try {
                    inputData = fs.readFileSync(inputFile, 'utf8');
                } catch (err) {
                    console.error(`Error reading input file: ${inputFile}`, err);
                    process.exit(1);
                }
                outputData = await decryptFunction(inputData);
                break;
            case 'decrypt_RB':
                try {
                    inputData = fs.readFileSync(inputFile, 'utf8');
                } catch (err) {
                    console.error(`Error reading input file: ${inputFile}`, err);
                    process.exit(1);
                }
                outputData = await decryptFunction_RB(inputData);
                break;
            default:
                console.error(`Unknown mode: ${mode}`);
                process.exit(1);
        }
        try {
            fs.writeFileSync(outputFile, outputData, 'utf8');
        } catch (err) {
            console.error(`Error writing to output file: ${outputFile}`, err);
            process.exit(1);
        }
    }
})();
