package com.socket.serverdemo;

public class MainProcess {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int port = 53536;
		Server server = new Server(port) {
			public void onConnect(Transceiver client) {
				printInfo(client, "Connect!");
			}
			
			public void onConnectFailed() {
				System.out.println("Client Connect Failed!");
			}
			
			//在这里修改发送的内容
			public void onReceive(Transceiver client, String s){
				printInfo(client, "Receive Data:" + s);
				client.send(">>"+s, 0);
				client.send("D:/cache/Server/send/fromServer.txt", 1);
			}
			
			public void onDisconnect(Transceiver client) {
				printInfo(client, "Disconnect!");
			}
			
			public void onStopServer() {
				System.out.println("----------Server Stopped-----------");
			}
		};
		
		System.out.println("----------Server Started----------");
		server.start();

	}
	
	static void printInfo(Transceiver client, String msg) {
		System.out.println(client.getInetAddress().getHostAddress() + " " + msg);		
	}

}
