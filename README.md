# 写在前面
该工具为原git项目RaindropEcho的JAVA重构，有一些功能增删，更适合日常使用。原作者:tingyusys，项目地址:[RaindrioEcho](https://github.com/tingyusys/RaindropEcho)。

工具特点：


1. 可实现请求、响应的自动加解密，适用于Reapter、Intruder下的接口测试；
2. 自定义正则匹配逻辑。匹配到的内容会交给对应JS处理；
3. 自定义加解密JS，
  适用于能直接扣JS的情况：简单的加解密方法可直接复制前端源码，复杂情况需要做逆向；已提供Webpack的逆向模板。


# ✈️ 一、工具概述

日常渗透过程中，经常会碰到一些网站需要破解其 JS 加密算法后，才能对数据包进行修改。我们将加密算法破解出来后，一般就是使用 python 的 requests 库，发送修改的数据包来进行渗透测试，导致效率低下，因此考虑将 JS 逆向的结果通过插件与 burp 结合，提高效率。在 JS 逆向结束之后，通过 RaindropEcho 提供的 JS 模版，导入到插件，就可以完成 burp 数据包的自动加解密。


# 📝 二、TODO

## 功能支持的更新

* [x] 提供 JS 逆向模版，支持自定义编写：指定域名下指定接口的加解密算法
* [x] 支持导入多个模版，同时破解多个接口的加密算法
* [x] 导入模板后，将数据包送到插件里，RaindropEcho 会自动解密，并将解密的数据包放入到重放器
* [x] 修改完数据后，发送数据包，RaindropEcho 会自动拦截明文数据包，进行加密后发出
* [x] 支持响应数据包解密，支持Repeater、Intruder自动加、解密----2024.1002
* [x] 扩大接口匹配范围，匹配子目录下所有接口----2025.0106
* [x] Java重构，使用新的Montoya API(2024.12)；使用正则匹配请求包中要加密的字段，支持url、header、body中多个字段的同时加密----20250130
* [x] UI更新;新增正则功能，可修改默认表达式;新增右键标记功能----20250131


# 🚨 三、准备工作

- **环境准备**

      开发版本:

          Burpsuite2024.9.2;

          JDK 17;
          
          nodejs v22.12.0;

      建议使用较新版本burp，目前已知2022版本无法加载,jdk1.8无法加载。


# 🐉 四、工具使用


## JS 逆向模版 \*\*重要\*\*

- 在 encryptFunction 函数里写好加密逻辑
- 在 decryptFunction 函数里写好解密逻辑
- 在 config 里写好逆向代码对应的域名和接口

**JS 逆向模版:muban_main.js：**

```js
const fs = require('fs');
require('./muban_ende.js');
// 上面自己编写加密函数

// 写加密函数的加载方式
function encryptFunction(data) {
  // 使用 JSON.parse 将字符串转换为 JSON 对象
  const json_data = JSON.parse(data);
  // 原来数据包时什么格式，就要返回什么格式
  let json_1=encrypt(`{"mobile":"${json_data.mobile}","bizType":"${json_data.bizType}"}`);
  return `{"key":"${json_1.key}","body":"${json_1.data}","app_header":{"partner_no":"0","referrer_no":null}}`
}

//解密函数加载方式
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
```

**针对webpack打包的加解密函数构造模版:muban_ende.js：**
```js
// 补环境
var window = global;
var navigator = [];
/*
补环境，举例
navigator["userAgent"]='Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.112 Mobile Safari/537.36';
window["location"]={
    "ancestorOrigins": {},
    "href": "https://",
    "origin": "https://",
    "protocol": "https:",
    "host": "",
    "hostname": "",
    "port": "",
    "pathname": "/h5web/",
    "search": "",
    "hash": "#/login?redirect=%2Fmine"
};
window["navigator"]=[];
window["location"]["href"]="";
window["navigator"]["userAgent"]=navigator["userAgent"]
*/


// 加载器引用变量
var loader = undefined;
/*
1、扣出主函数，获取加载器
2、加载器全局引用
3、扣加解密相关函数
3、去掉初始化方法，去检测
5、调用测试，补环境等
*/


//自拟加解密调用函数
function encrypt(t){
    var d=loader("xxx");
    var D= d.default.encrypt;
    return D(t)
}
function decrypt(t){
    var d=loader("xxx");
    var D= d.default.decrypt;
    return D(t)
}

global.encrypt = encrypt ;//将需要调用的函数或对象编程全局
exports.encrypt = encrypt ;//使用export来暴露接口，不然nodejs无法找到我们的加密方法
global.decrypt = decrypt ;
exports.decrypt = decrypt ;
```
## 右键功能标记请求包字段

使用建议：可标记多处字段；勿标记"Host: "、协议、请求方法等请求包必要字段名
![右键](.\README.assets\0202141917189.png)

![右键](README.assets\0202142013170.png)


## 导入模版文件

**选择 js 文件（注：js 文件路径一定不要有中文）**

使用建议：加载前预先命令行调试JS

![JS](README.assets\0202142848948.png)

## 发送请求

**发送时自动加密字段，响应包成功解密**

![请求](.\README.assets\0202143907292.png)


使用建议：实际请求包和响应体在日志中查看；指定一个日志输出文件，burp插件输出窗口有数据量限制

![日志](README.assets\0202144259457.png)

## 字段匹配方式说明

默认使用正则表达式为

![正则](README.assets\0202144653976.png)

测试是否能提取到字段

![提取](README.assets\0202144848219.png)

可自定义表达式，测试提取成功后点击“更新”，全局生效

![自定义](README.assets\0202145333206.png)

保留前后缀，切换后全局生效

![自定义](README.assets\0202145503368.png)

**使用建议：偶有测试自定义表达式时成功，但请求时提取不到，建议多观察请求日志，改进表达式；表达式内最好不使用如"Host: "等头部字段做匹配**


# 🖐 五、免责声明

1. 如果您下载、安装、使用、修改本工具及相关代码，即表明您信任本工具
2. 在使用本工具时造成对您自己或他人任何形式的损失和伤害，我们不承担任何责任
3. 如您在使用本工具的过程中存在任何非法行为，您需自行承担相应后果，我们将不承担任何法律及连带责任
4. 请您务必审慎阅读、充分理解各条款内容，特别是免除或者限制责任的条款，并选择接受或不接受
5. 除非您已阅读并接受本协议所有条款，否则您无权下载、安装或使用本工具
6. 您的下载、安装、使用等行为即视为您已阅读并同意上述协议的约束
