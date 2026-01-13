const fs = require('fs');
const path = require('path');

// 批量修复cyberchef中所有JSON导入问题 - 使用with语法（Node.js v22+标准）
const cyberchefDir = path.join(__dirname, 'node_modules', 'cyberchef');

console.log('批量修复CyberChef JSON导入问题（使用with语法）...');
console.log('目录:', cyberchefDir);

function findFiles(dir, pattern) {
    const files = [];
    const items = fs.readdirSync(dir, { withFileTypes: true });
    
    for (const item of items) {
        const fullPath = path.join(dir, item.name);
        if (item.isDirectory()) {
            files.push(...findFiles(fullPath, pattern));
        } else if (item.isFile() && pattern.test(item.name)) {
            files.push(fullPath);
        }
    }
    
    return files;
}

const mjsFiles = findFiles(cyberchefDir, /\.(mjs|js)$/);
console.log(`找到 ${mjsFiles.length} 个文件`);

let fixedCount = 0;
let fixedFiles = 0;

for (const file of mjsFiles) {
    let content = fs.readFileSync(file, 'utf-8');
    const originalContent = content;
    
    // 替换 assert {type: "json"} 为 with {type: "json"}（Node.js v22+标准语法）
    content = content.replace(
        /import\s+([^;]+?)\s+from\s+["']([^"']+\.json)["']\s+assert\s+\{type:\s*["']json["']\}/g,
        (match, imports, jsonPath) => {
            fixedCount++;
            return `import ${imports} from "${jsonPath}" with {type: "json"}`;
        }
    );
    
    if (content !== originalContent) {
        fs.writeFileSync(file, content, 'utf-8');
        fixedFiles++;
        console.log(`✓ 修复: ${path.relative(cyberchefDir, file)}`);
    }
}

console.log(`\n修复完成！共修复 ${fixedFiles} 个文件中的 ${fixedCount} 处JSON导入`);
console.log('\n使用的是 Node.js v22+ 标准语法: with {type: "json"}');
