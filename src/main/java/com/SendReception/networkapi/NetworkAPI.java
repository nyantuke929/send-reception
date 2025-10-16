package com.SendReception.networkapi;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class NetworkAPI {
    
    // 送信用クラス
    public static class Sender {
        private String ip;
        private int port;
        private String password;
        
        private Sender(String ip, int port, String password) {
            this.ip = ip;
            this.port = port;
            this.password = password;
        }
        
        // メッセージのみ送信
        public void message(String msg) throws IOException {
            sendData(msg, new HashMap<>());
        }
        
        // メッセージと変数1つを送信
        public void message(String msg, String key, String value) throws IOException {
            Map<String, String> vars = new HashMap<>();
            vars.put(key, value);
            sendData(msg, vars);
        }
        
        // メッセージと変数を送信
        public void message(String msg, Map<String, String> variables) throws IOException {
            sendData(msg, variables);
        }
        
        // Minecraftプレイヤー情報を送信(Bukkit/Spigot用)
        public void messageWithPlayer(String msg, Object player) throws IOException {
            Map<String, String> vars = new HashMap<>();
            try {
                // リフレクションでPlayer情報を取得
                Class<?> playerClass = player.getClass();
                vars.put("playerName", (String) playerClass.getMethod("getName").invoke(player));
                vars.put("playerUUID", playerClass.getMethod("getUniqueId").invoke(player).toString());
                vars.put("playerWorld", playerClass.getMethod("getWorld").invoke(player).getClass().getMethod("getName").invoke(playerClass.getMethod("getWorld").invoke(player)).toString());
            } catch (Exception e) {
                throw new IOException("Failed to extract player data", e);
            }
            sendData(msg, vars);
        }
        
        // 内部送信処理
        private void sendData(String msg, Map<String, String> variables) throws IOException {
            try (Socket socket = new Socket(ip, port);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                
                // パスワードハッシュを送信
                String hash = hashPassword(password);
                out.writeUTF(hash);
                
                // メッセージを送信
                out.writeUTF(msg);
                
                // 変数の数を送信
                out.writeInt(variables.size());
                
                // 変数を送信
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeUTF(entry.getValue());
                }
                
                out.flush();
            }
        }
    }
    
    // 受信データクラス
    public static class ReceivedData {
        private String message;
        private Map<String, String> variables;
        
        public ReceivedData(String message, Map<String, String> variables) {
            this.message = message;
            this.variables = variables;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Map<String, String> getVariables() {
            return variables;
        }
        
        public String getVariable(String key) {
            return variables.get(key);
        }
    }
    
    // 受信用サーバー
    private static ServerSocket serverSocket;
    private static String serverPassword;
    
    // 送信メソッド
    public static Sender send(String ip, int port, String password) {
        return new Sender(ip, port, password);
    }
    
    // 受信メソッド(メッセージのみ)
    public static String reception(int port, String password) throws IOException {
        ReceivedData data = receptionWithVariables(port, password);
        return data.getMessage();
    }
    
    // 受信メソッド(メッセージと変数)
    public static ReceivedData receptionWithVariables(int port, String password) throws IOException {
        serverPassword = password;
        
        if (serverSocket == null || serverSocket.isClosed()) {
            serverSocket = new ServerSocket(port);
        }
        
        try (Socket client = serverSocket.accept();
             DataInputStream in = new DataInputStream(client.getInputStream())) {
            
            // パスワード検証
            String receivedHash = in.readUTF();
            String expectedHash = hashPassword(password);
            
            if (!receivedHash.equals(expectedHash)) {
                throw new SecurityException("Invalid password");
            }
            
            // メッセージ受信
            String message = in.readUTF();
            
            // 変数の数を受信
            int varCount = in.readInt();
            
            // 変数を受信
            Map<String, String> variables = new HashMap<>();
            for (int i = 0; i < varCount; i++) {
                String key = in.readUTF();
                String value = in.readUTF();
                variables.put(key, value);
            }
            
            return new ReceivedData(message, variables);
        }
    }
    
    // サーバーを閉じる
    public static void closeServer() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
    
    // パスワードハッシュ化
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
