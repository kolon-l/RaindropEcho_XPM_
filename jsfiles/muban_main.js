const fs = require('fs');

// 需要自拟的部分---------------------------------------------------------------
// require('./muban_ende.js');

// 供burp调用的加密函数的加载方式,可用于进一步处理加密结果;入参为打标记字段
function encryptFunction(data) {
    // return encrypt(data)
    return data.replaceAll("§",",")+"en1"
}
// 供burp调用的解密函数的加载方式;入参为响应体，默认原文返回不做处理
function decryptFunction(data){
    // let res = (JSON.parse(data));
    // return decrypt(data)
    return data.replaceAll("§",",")+"de1"
}
// 供burp右键解密功能的加载方式;入参为密文,默认直接调用解密函数
function decryptFunction_RB(data){
    // return decrypt(data)
    return data.replaceAll("§",",")+"derb1"
}

// 下面代码不要动---------------------------------------------------------------
if (process.argv.length < 4) {
    console.log("Usage: node script.js [server] [port]");
    console.log("Usage: node script.js [encrypt_c|decrypt_c] [input]");
    console.log("Usage: node script.js [encrypt|decrypt] [inputfile] [outputfile]");
    process.exit(1);
}

const mode = process.argv[2];
if(process.argv.length == 4){
    let input = process.argv[3];
    let output;
    switch(mode){
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
                    req.on('end', () => {
                        console.log(postparms);
                        console.log(encryptFunction(postparms));
                        res.end(encryptFunction(postparms));
                    })
                } else if (path.pathname.endsWith('/decrypt')) {
                    console.log("Decrypt:")
                    req.on('data', (parms) => {
                        postparms += parms
                    })
                    req.on('end', () => {
                        console.log(postparms);
                        let dec=decryptFunction(postparms);
                        console.log(dec);
                        res.end(dec);
                    })
                } else if (path.pathname.endsWith('/decrypt_RB')) {
                    console.log("Decrypt RightButton:")
                    req.on('data', (parms) => {
                        postparms += parms
                    })
                    req.on('end', () => {
                        console.log(postparms);
                        let dec=decryptFunction_RB(postparms);
                        console.log(dec);
                        res.end(dec);
                    })
                } else{
                    res.write("end");
                    res.end()
                }
            }).listen(input);
            break;
        case 'encrypt_c':
            output = encryptFunction(input);
            console.log(output);
            process.exit(0);
            break;
        case 'decrypt_c':
            output = decryptFunction(input);
            console.log(output);
            process.exit(0);
            break;
        case 'decrypt_c_RB':
            output = decryptFunction_RB(input);
            console.log(output);
            process.exit(0);
            break;
        default:
            console.error(`Unknown mode: ${mode}`);
            process.exit(1);
    }
}
else if(process.argv.length > 4){
    const mode = process.argv[2];
    let inputFile = process.argv[3];
    let outputFile = process.argv[4];
    switch (mode) {
        case 'encrypt':
            try {
                inputData = fs.readFileSync(inputFile, 'utf8');
            } catch (err) {
                console.error(`Error reading input file: ${inputFile}`, err);
                process.exit(1);
            }
            outputData = encryptFunction(inputData);
            break;
        case 'decrypt':
            try {
                inputData = fs.readFileSync(inputFile, 'utf8');
            } catch (err) {
                console.error(`Error reading input file: ${inputFile}`, err);
                process.exit(1);
            }
            outputData = decryptFunction(inputData);
            break;
        case 'decrypt_RB':
            try {
                inputData = fs.readFileSync(inputFile, 'utf8');
            } catch (err) {
                console.error(`Error reading input file: ${inputFile}`, err);
                process.exit(1);
            }
            outputData = decryptFunction_RB(inputData);
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
