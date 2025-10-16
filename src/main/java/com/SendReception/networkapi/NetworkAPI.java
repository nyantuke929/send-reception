package com.SendReception.networkapi;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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
        
        public void message(String msg) throws IOException {
            try (Socket socket = new Socket(ip, port);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                
                // パスワードハッシュを送信
                String hash = hashPassword(password);
                out.writeUTF(hash);
                
                // メッセージを送信
                out.writeUTF(msg);
                out.flush();
            }
        }
    }
    
    // 受信用サーバー
    private static ServerSocket serverSocket;
    private static String serverPassword;
    
    // 送信メソッド
    public static Sender send(String ip, int port, String password) {
        return new Sender(ip, port, password);
    }
    
    // 受信メソッド（サーバー起動と受信を兼ねる）
    public static String reception(int port, String password) throws IOException {
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
            return in.readUTF();
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
