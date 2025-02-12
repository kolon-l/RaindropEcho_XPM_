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

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
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

    public  String call_js_endecryption_function(String jsFile, String data,Boolean isencrypt) throws Exception {
        // 构建执行 Node.js 脚本的命令
        List<String> command = new ArrayList<>();
        command.add("node");
        command.add(jsFile);
        if(isencrypt){command.add("encrypt_c");}else{command.add("decrypt_c");}
        command.add(data);
        // 执行 Node.js 脚本
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        String outData = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String stdout = reader.lines().collect(Collectors.joining("\n"));
        String stderr = errorReader.lines().collect(Collectors.joining("\n"));
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Failed to retrieve result from JS file: " + jsFile + ". Error: " + stderr);
        }
        outData=stdout;

        return outData;
    }
}
