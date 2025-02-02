package org.intellij;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.utilities.json.JsonStringNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UsuallyJS {

    private final MontoyaApi api;
    UsuallyJS(MontoyaApi montoyaApi) {
        api = montoyaApi;
    }

    public String get_domain_and_path_from_js(String jsfile) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("node", jsfile, "config");
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String stdout = reader.lines().collect(Collectors.joining("\n"));
            String stderr = errorReader.lines().collect(Collectors.joining("\n"));
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new Exception("Failed to retrieve default JSON from JS file: " + jsfile + ". Error: " + stderr);
            }
            JsonStringNode json = JsonStringNode.jsonStringNode(stdout);
            if (!json.getValue().contains("domain")){
                throw new Exception("Failed to retrieve JSON from JS file: " + jsfile + ". getJson Error:The type of config is JSON?");
            }
            return json.getValue().replace("}","")+",\"file\":\""+jsfile+"\"}";
        }
        catch (Exception e) {
            throw new Exception("Failed to retrieve JSON from JS file: " + jsfile + ". Error: " + e.getMessage());

        }
    }

    public  String call_js_endecryption_function(String jsFile, String data,Boolean isencrypt) throws IOException, InterruptedException {
        // 创建临时文件存储输入的数据
        Path tempInPath = Files.createTempFile("input-", ".txt");
        Path tempOutPath = Files.createTempFile("output-", ".txt");

        try (BufferedWriter writer = Files.newBufferedWriter(tempInPath)) {
            writer.write(data);
        }

        // 构建执行 Node.js 脚本的命令
        List<String> command = new ArrayList<>();
        command.add("node");
        command.add(jsFile);
        if(isencrypt){command.add("encrypt");}else{command.add("decrypt");}
        command.add(tempInPath.toString());
        command.add(tempOutPath.toString());

        // 执行 Node.js 脚本
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        // 等待脚本执行完成
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String errorLine;
                StringBuilder errorMessage = new StringBuilder();
                while ((errorLine = errorReader.readLine()) != null) {
                    errorMessage.append(errorLine).append("\n");
                }
                throw new RuntimeException("Node.js script failed with error:\n" + errorMessage.toString());
            }
        }
        // 读取加密后的内容
        String outData = new String(Files.readString(tempOutPath));

        return outData;
    }
}
