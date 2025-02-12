const fs = require('fs');

require('./muban_ende.js');
// 上面自己编写加密函数

function encryptFunction(data) {
    // 写加密函数的加载方式
    // 示例
    // 使用 JSON.parse 将字符串转换为 JSON 对象
    const json_data = JSON.parse(data);
    // 原数据什么格式，就返回什么格式
    let json_1=encrypt(`{"mobile":"${json_data.mobile}","bizType":"${json_data.bizType}"}`);
    return `{"key":"${json_1.key}","body":"${json_1.data}","app_header":{"partner_no":"0","referrer_no":null}}`
}

function decryptFunction(data){
    // 解密函数的加载方式
    // 示例
    // 原样返回
    let res = data;
    return res
}
// 编写监控域名和接口
const config = {
    domain: "",
    path: ""
};


// 下面代码不要动---------------------------------------------------------------

// 检查传递的参数数量
const mode = process.argv[2];

if (mode === 'config') {
    console.log(JSON.stringify(config));
    process.exit(0);
}

let inputData;
let outputData;
if (process.argv.length < 4) {
    console.error("Usage: node script.js [mode] [input]");
    console.error("Usage: node script.js [mode] [inputfile] [outputfile]");
    process.exit(1);
}
else if(process.argv.length == 4){
    inputData = process.argv[3];
    // 直接传值
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
