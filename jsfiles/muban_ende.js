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


//自拟加解密函数
function encrypt(t){
    // var d=loader("xxx");
    // var D= d.default.encrypt;
    return t+"en"
}
function decrypt(t){
    // var d=loader("xxx");
    // var D= d.default.decrypt;
    return t+"de"
}

global.encrypt = encrypt ;//将需要调用的函数或对象编程全局
exports.encrypt = encrypt ;//使用export来暴露接口，不然nodejs无法找到我们的加密方法
global.decrypt = decrypt ;
exports.decrypt = decrypt ;