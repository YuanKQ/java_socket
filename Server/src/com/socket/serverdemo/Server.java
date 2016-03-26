package com.socket.serverdemo;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public abstract class Server implements Runnable{
	private int port;
	private boolean runflag;
	private ArrayList<Transceiver> clients = new ArrayList<Transceiver>();
	
	public Server(int port) {
		this.port = port;
	}
	
	public void start() {
		runflag = true;
		new Thread(this).start();
	}
	
	public void stop() {
		runflag = false;
	}
	
	public void run() {
		try {
			System.out.println("port: "+ port);
			ServerSocket server = new ServerSocket(port);
            while (runflag) {
            	try {
				   final Socket socket = server.accept();
				   startClient(socket);
				} catch (Exception e) {
					System.out.println("#Socket 连接失败" + e.getMessage());
					onConnectFailed();
				}
            }

            try {
            	for (Transceiver client: clients) {
            		client.stop();
            	}
            	clients.clear();
            	server.close();
            } catch (Exception e) {
            	System.out.println("#Server 关闭异常" + e.getMessage());
            }
		} catch (Exception e) {
			System.out.println("#Server 启动异常" + e.getMessage());
		}
		
		onStopServer();
	}
	
	public void startClient(final Socket socket) {
		Transceiver client = new Transceiver(socket){
			public void onReceive(InetAddress addr, String s) {
				Server.this.onReceive(this, s);
			}
			
			public void onDisconnect(InetAddress addr) {
				Server.this.onDisconnect(this);
			}
		};
		
		client.start();
		clients.add(client);
		this.onConnect(client);
	}
	
	
	
	public abstract void onReceive(Transceiver client, String s);
	
	public abstract void onConnect(Transceiver client);
	
	public abstract void onDisconnect(Transceiver client);
	
	public abstract void onConnectFailed();
	
	public abstract void onStopServer();
}
