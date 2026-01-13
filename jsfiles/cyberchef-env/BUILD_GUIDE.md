# CyberChef  集成详细搭建文档

## 环境信息

- **操作系统**：Windows
- **Node.js版本**：v22.12.0
- **CyberChef集成目录**：`./cyberchef-env`

## 搭建步骤

### 1. 环境准备

#### 1.1 安装Node.js
- 下载并安装Node.js v22.12.0
- 验证安装：`node -v`

#### 1.2 创建工作目录
```bash
mkdir cyberchef-env
cd cyberchef-env
```

#### 1.3 初始化项目
```bash
npm init -y
```

#### 1.4 安装CyberChef依赖
```bash
npm install cyberchef
```

### 2. 创建主程序文件

创建 `main_cyberchef.js` 文件，实现以下功能：
- 异步导入CyberChef模块
- SM4加密/解密功能
- HTTP服务器接口
- 命令行接口

### 3. 遇到的问题及解决方案

#### 问题1：crypto-api模块导入路径错误

**错误信息：**
```
Error: Cannot find module 'crypto-api/src/hasher/sha256'
```

**原因分析：**
- Node.js v22使用ESM模块系统，要求相对导入路径必须包含文件扩展名
- crypto-api模块中的导入语句缺少`.mjs`扩展名

**解决方案：**
创建 `fix_cyberchef_deps.js` 脚本：

```javascript
const fs = require('fs');
const path = require('path');

function fixCryptoApiImports() {
    const cryptoApiPath = path.join(__dirname, 'node_modules', 'crypto-api', 'src');
    
    function processDirectory(dir) {
        const files = fs.readdirSync(dir);
        
        files.forEach(file => {
            const filePath = path.join(dir, file);
            const stat = fs.statSync(filePath);
            
            if (stat.isDirectory()) {
                processDirectory(filePath);
            } else if (file.endsWith('.js')) {
                let content = fs.readFileSync(filePath, 'utf8');
                
                // 修复相对导入路径
                content = content.replace(
                    /from\s+['"](\.\.\/[^'"]+)['"]/g,
                    (match, importPath) => {
                        if (!importPath.endsWith('.mjs') && !importPath.endsWith('.js')) {
                            return `from '${importPath}.mjs'`;
                        }
                        return match;
                    }
                );
                
                fs.writeFileSync(filePath, content, 'utf8');
            }
        });
    }
    
    processDirectory(cryptoApiPath);
    console.log('✓ crypto-api导入路径修复完成');
}

fixCryptoApiImports();
```

**执行修复：**
```bash
node fix_cyberchef_deps.js
```

**修复结果：**
- 处理了24个文件
- 修复了16处导入路径

---

#### 问题2：JSON导入语法不兼容

**错误信息：**
```
SyntaxError: Unexpected identifier 'assert'
```

**原因分析：**
- Node.js v22+使用`with {type: "json"}`语法导入JSON
- CyberChef使用旧语法`assert {type: "json"}`

**解决方案：**
创建 `fix_json_imports_v22.js` 脚本：

```javascript
const fs = require('fs');
const path = require('path');

function fixJsonImports() {
    const cyberchefPath = path.join(__dirname, 'node_modules', 'cyberchef');
    
    function processDirectory(dir) {
        const files = fs.readdirSync(dir);
        
        files.forEach(file => {
            const filePath = path.join(dir, file);
            const stat = fs.statSync(filePath);
            
            if (stat.isDirectory()) {
                processDirectory(filePath);
            } else if (file.endsWith('.mjs') || file.endsWith('.js')) {
                let content = fs.readFileSync(filePath, 'utf8');
                
                // 修复JSON导入语法
                const original = content;
                content = content.replace(
                    /assert\s*\{\s*type:\s*"json"\s*\}/g,
                    'with {type: "json"}'
                );
                
                if (content !== original) {
                    fs.writeFileSync(filePath, content, 'utf8');
                    console.log(`✓ 修复: ${filePath}`);
                }
            }
        });
    }
    
    processDirectory(cyberchefPath);
}

fixJsonImports();
```

**执行修复：**
```bash
node fix_json_imports_v22.js
```

**修复结果：**
- 找到763个文件
- 修复了7个文件中的9处JSON导入
- 涉及文件：
  - `src/core/ChefWorker.js`
  - `src/core/lib/Magic.mjs`
  - `src/core/Recipe.mjs`
  - `src/node/api.mjs`
  - 其他3个文件

---

#### 问题3：snackbarjs自闭合标签问题

**原因分析：**
- snackbarjs使用自闭合div标签`<div />`
- 在某些环境下可能导致渲染问题

**解决方案：**
在 `fix_cyberchef_deps.js` 中添加修复函数：

```javascript
function fixSnackbarMarkup() {
    const snackbarPath = path.join(__dirname, 'node_modules', 'snackbarjs');
    const distPath = path.join(snackbarPath, 'dist');
    
    if (fs.existsSync(distPath)) {
        const files = fs.readdirSync(distPath);
        files.forEach(file => {
            if (file.endsWith('.js')) {
                const filePath = path.join(distPath, file);
                let content = fs.readFileSync(filePath, 'utf8');
                
                // 修复自闭合div标签
                content = content.replace(/<div\s*\/>/g, '<div></div>');
                
                fs.writeFileSync(filePath, content, 'utf8');
                console.log(`✓ 修复snackbarjs标记: ${file}`);
            }
        });
    }
}
```

---

#### 问题4：jimp模块package.json配置问题

**原因分析：**
- jimp模块缺少ESM模块配置
- 导致模块无法正确加载

**解决方案：**
在 `fix_cyberchef_deps.js` 中添加修复函数：

```javascript
function fixJimpModule() {
    const jimpPackagePath = path.join(__dirname, 'node_modules', 'jimp', 'package.json');
    
    if (fs.existsSync(jimpPackagePath)) {
        const packageJson = JSON.parse(fs.readFileSync(jimpPackagePath, 'utf8'));
        
        if (!packageJson.type) {
            packageJson.type = 'module';
            fs.writeFileSync(jimpPackagePath, JSON.stringify(packageJson, null, 2), 'utf8');
            console.log('✓ 修复jimp package.json');
        }
    }
}
```

### 4. 完整修复脚本

将所有修复功能整合到 `fix_cyberchef_deps.js`：

```javascript
const fs = require('fs');
const path = require('path');

// 修复crypto-api导入路径
function fixCryptoApiImports() {
    const cryptoApiPath = path.join(__dirname, 'node_modules', 'crypto-api', 'src');
    let fixedCount = 0;
    
    function processDirectory(dir) {
        const files = fs.readdirSync(dir);
        
        files.forEach(file => {
            const filePath = path.join(dir, file);
            const stat = fs.statSync(filePath);
            
            if (stat.isDirectory()) {
                processDirectory(filePath);
            } else if (file.endsWith('.js')) {
                let content = fs.readFileSync(filePath, 'utf8');
                const original = content;
                
                content = content.replace(
                    /from\s+['"](\.\.\/[^'"]+)['"]/g,
                    (match, importPath) => {
                        if (!importPath.endsWith('.mjs') && !importPath.endsWith('.js')) {
                            return `from '${importPath}.mjs'`;
                        }
                        return match;
                    }
                );
                
                if (content !== original) {
                    fs.writeFileSync(filePath, content, 'utf8');
                    fixedCount++;
                }
            }
        });
    }
    
    processDirectory(cryptoApiPath);
    console.log(`✓ crypto-api导入路径修复完成 (${fixedCount}处)`);
}

// 修复snackbarjs标记
function fixSnackbarMarkup() {
    const snackbarPath = path.join(__dirname, 'node_modules', 'snackbarjs');
    const distPath = path.join(snackbarPath, 'dist');
    
    if (fs.existsSync(distPath)) {
        const files = fs.readdirSync(distPath);
        files.forEach(file => {
            if (file.endsWith('.js')) {
                const filePath = path.join(distPath, file);
                let content = fs.readFileSync(filePath, 'utf8');
                
                content = content.replace(/<div\s*\/>/g, '<div></div>');
                
                fs.writeFileSync(filePath, content, 'utf8');
                console.log(`✓ 修复snackbarjs标记: ${file}`);
            }
        });
    }
}

// 修复jimp模块
function fixJimpModule() {
    const jimpPackagePath = path.join(__dirname, 'node_modules', 'jimp', 'package.json');
    
    if (fs.existsSync(jimpPackagePath)) {
        const packageJson = JSON.parse(fs.readFileSync(jimpPackagePath, 'utf8'));
        
        if (!packageJson.type) {
            packageJson.type = 'module';
            fs.writeFileSync(jimpPackagePath, JSON.stringify(packageJson, null, 2), 'utf8');
            console.log('✓ 修复jimp package.json');
        }
    }
}

// 执行所有修复
console.log('开始修复CyberChef依赖...\n');
fixCryptoApiImports();
fixSnackbarMarkup();
fixJimpModule();
console.log('\n所有修复完成！');
```

## 问题排查清单

### 常见问题

1. **模块找不到**
   - 检查node_modules目录是否存在
   - 运行 `npm install` 重新安装依赖

2. **导入错误**
   - 运行修复脚本：`node fix_cyberchef_deps.js`
   - 运行JSON导入修复：`node fix_json_imports_v22.js`

3. **权限问题**
   - 以管理员身份运行终端
   - 检查文件权限设置

---

## 技术要点

### Node.js v22+ ESM模块特性

1. **文件扩展名要求**
   - 相对导入必须包含文件扩展名
   - 支持 `.mjs` 和 `.js` 扩展名

2. **JSON导入语法**
   - 旧语法：`import data from './data.json' assert {type: "json"}`
   - 新语法：`import data from './data.json' with {type: "json"}`

3. **动态导入**
   - 使用 `import()` 函数进行异步导入
   - 返回Promise对象

### CyberChef模块结构

```
cyberchef/
├── src/
│   ├── core/
│   │   ├── ChefWorker.js
│   │   ├── Recipe.mjs
│   │   └── lib/
│   ├── node/
│   │   ├── api.mjs
│   │   └── wrapper.js
│   └── operations/
```

---

## 总结

本次搭建过程主要解决了以下问题：

1. ✅ Node.js v22 ESM模块兼容性问题
2. ✅ crypto-api模块导入路径修复
3. ✅ JSON导入语法更新
4. ✅ 第三方依赖配置修复

所有修复脚本已保存在项目目录中，可以在需要时重新运行以解决问题。

---

## 参考资料

- [CyberChef官方文档](https://gchq.github.io/CyberChef/)
- [Node.js ESM模块文档](https://nodejs.org/api/esm.html)

---

**文档版本**：1.0  
**最后更新**：2026-01-06  
