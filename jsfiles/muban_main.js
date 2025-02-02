const fs = require('fs');

require('./muban_ende.js');
// 上面自己编写加密函数


// 写加密函数的加载方式
function encryptFunction(data) {
    return encrypt(data)
}

function decryptFunction(data){
// 此处传来的是Response Body
// 不做解密，原样返回
    return data
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

if (process.argv.length < 4) {
    console.error("Usage: node script.js [mode] [inputFile] [outputFile]");
    process.exit(1);
}

const inputFile = process.argv[3];
const outputFile = process.argv[4];

// 读取输入文件内容
let inputData;
try {
    inputData = fs.readFileSync(inputFile, 'utf8');
} catch (err) {
    console.error(`Error reading input file: ${inputFile}`, err);
    process.exit(1);
}

let outputData;

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