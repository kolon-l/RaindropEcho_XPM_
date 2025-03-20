const fs = require('fs');
const express = require('express');

require('./muban_ende-test.js');
// 上面自己编写加密函数

// 写加密函数的加载方式
function encryptFunction(data) {
    return encrypt(data)
}

function decryptFunction(data){
    // let res = (JSON.parse(data));
    let res = data+"de"
    return res
}


// 下面代码不要动---------------------------------------------------------------

// 检查传递的参数数量
const mode = process.argv[2];

if (mode === 'config') {
    console.log(JSON.stringify(config));
    process.exit(0);
}

let inputData;
let outputData;
if (process.argv.length < 3) {
    console.error("Usage: node script.js [mode]");
    console.error("Usage: node script.js [mode] [input]");
    console.error("Usage: node script.js [mode] [inputfile] [outputfile]");
    process.exit(1);
}
else if(process.argv.length == 3){
    switch(mode){
        case 'server':
            var http = require('http');
            const url = require('url');
            const querystring = require('querystring');
            http.createServer(function (req, res) {
                let path = url.parse(req.url);
                let postparms = '';
                if (path.pathname === '/encrypt') {
                    console.log("encrypt");
                    req.on('data', (parms) => {
                        postparms += parms;
                    });
                    req.on('end', () => {
                        console.log(postparms);
                        console.log(encryptFunction(postparms));
                        // Data = "X-BASE-DATA=" + Data;
                        res.end(encryptFunction(postparms));
                    })
                } else if (path.pathname === '/decrypt') {
                    console.log("decrypt")
                    req.on('data', (parms) => {
                        postparms += parms
                    })
                    req.on('end', () => {
                        console.log(postparms);
                        let dec=decryptFunction(postparms);
                        console.log(dec);
                        // Data = "X-BASE-DATA=" + Data;
                        res.end(dec);
                    })
                } else{
                    res.write("end");
                    res.end()

                }
            }).listen(8888);
    }

}
else if(process.argv.length == 4){
    inputData = process.argv[3];
    switch(mode){
        case 'encrypt_c':
            outputData = encryptFunction(inputData);
            break;
        case 'decrypt_c':
            outputData = decryptFunction(inputData);
            break;
        default:
            console.error(`Unknown mode: ${mode}`);
            process.exit(1);
    }
    console.log(outputData);
    process.exit(0);
}
else if(process.argv.length >= 4){
    let inputFile = process.argv[3];
    let outputFile = process.argv[4];
    // 读取输入文件内容
    try {
        inputData = fs.readFileSync(inputFile, 'utf8');
    } catch (err) {
        console.error(`Error reading input file: ${inputFile}`, err);
        process.exit(1);
    }

    switch (mode) {
        case 'encrypt':
            outputData = encryptFunction(inputData);
            break;
        case 'decrypt':
            outputData = decryptFunction(inputData);
            break;
        default:
            console.error(`Unknown mode: ${mode}`);
            process.exit(1);
    }

    // 将输出数据写入输出文件
    try {
        fs.writeFileSync(outputFile, outputData, 'utf8');
    } catch (err) {
        console.error(`Error writing to output file: ${outputFile}`, err);
        process.exit(1);
    }
}
