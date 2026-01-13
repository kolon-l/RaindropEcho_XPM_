const fs = require('fs');
const path = require('path');

/**
 * 修复crypto-api包中的导入路径
 * 将相对导入路径添加.mjs扩展名
 */
function fixCryptoApiImports() {
    const cryptoApiPath = path.join(__dirname, 'node_modules', 'crypto-api', 'src');
    
    if (!fs.existsSync(cryptoApiPath)) {
        console.log('crypto-api目录不存在，跳过修复');
        return;
    }
    
    let fixedCount = 0;
    let filesProcessed = 0;
    
    // 递归遍历目录
    function traverseDir(dir) {
        const files = fs.readdirSync(dir);
        
        for (const file of files) {
            const fullPath = path.join(dir, file);
            const stat = fs.statSync(fullPath);
            
            if (stat.isDirectory()) {
                traverseDir(fullPath);
            } else if (file.endsWith('.mjs') || file.endsWith('.js')) {
                filesProcessed++;
                let content = fs.readFileSync(fullPath, 'utf8');
                const originalContent = content;
                
                // 匹配相对导入路径，但排除已经包含.mjs的
                // 例如: from "./hasher/sha256" -> from "./hasher/sha256.mjs"
                const regex = /(from\s+"(\.\.\/|\.\/)[^"]+)";/g;
                
                content = content.replace(regex, (match, importPath) => {
                    // 如果已经包含.mjs，不修改
                    if (importPath.endsWith('.mjs"')) {
                        return match;
                    }
                    // 添加.mjs扩展名
                    return importPath + '.mjs";';
                });
                
                if (content !== originalContent) {
                    fs.writeFileSync(fullPath, content, 'utf8');
                    fixedCount++;
                    console.log(`已修复: ${path.relative(cryptoApiPath, fullPath)}`);
                }
            }
        }
    }
    
    console.log('开始修复crypto-api导入路径...');
    traverseDir(cryptoApiPath);
    console.log(`处理了 ${filesProcessed} 个文件，修复了 ${fixedCount} 个导入路径`);
}

// 修复snackbarjs的标记问题
function fixSnackbarMarkup() {
    const snackbarPath = path.join(__dirname, 'node_modules', 'snackbarjs', 'src', 'snackbar.js');
    
    if (!fs.existsSync(snackbarPath)) {
        console.log('snackbarjs文件不存在，跳过修复');
        return;
    }
    
    let content = fs.readFileSync(snackbarPath, 'utf8');
    const originalContent = content;
    
    // 修复自闭合div标签
    content = content.replace(/<div id=snackbar-container\/>/g, '<div id=snackbar-container>');
    
    if (content !== originalContent) {
        fs.writeFileSync(snackbarPath, content, 'utf8');
        console.log('已修复: snackbarjs标记问题');
    } else {
        console.log('snackbarjs标记无需修复');
    }
}

// 修复jimp的package.json
function fixJimpModule() {
    const jimpPackagePath = path.join(__dirname, 'node_modules', 'jimp', 'package.json');
    
    if (!fs.existsSync(jimpPackagePath)) {
        console.log('jimp package.json不存在，跳过修复');
        return;
    }
    
    let content = fs.readFileSync(jimpPackagePath, 'utf8');
    const originalContent = content;
    
    // 在"es/index.js"后面添加"type": "module"
    content = content.replace(
        /"es\/index\.js",/,
        '"es/index.js",\n  "type": "module",'
    );
    
    if (content !== originalContent) {
        fs.writeFileSync(jimpPackagePath, content, 'utf8');
        console.log('已修复: jimp package.json');
    } else {
        console.log('jimp package.json无需修复');
    }
}

// 执行所有修复
console.log('=== CyberChef依赖修复脚本 ===\n');
fixCryptoApiImports();
console.log();
fixSnackbarMarkup();
console.log();
fixJimpModule();
console.log('\n=== 所有修复完成 ===');
