package com.socket.serverdemo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class Transceiver implements Runnable{
	protected Socket socket;
	protected InetAddress addr;
	protected DataInputStream in;
	protected DataOutputStream out;
	private boolean runflag;
	private final String savePath = "D:/cache/Server/recv";
	private final int portSend = 60000;
	private final int portRecv = 61010;
	
	public Transceiver(Socket socket) {
		this.socket = socket;
		addr = socket.getInetAddress();
	}
	
	public InetAddress getInetAddress(){
		return addr;
	}
	
	public void start() {
		runflag = true;
		new Thread(this).start();
	}
	
	public void stop() {
		runflag = false;
		try {
			socket.shutdownInput();
			in.close();
			out.close();
		} catch (Exception e) {
			System.out.println("#Socket������  �ر��쳣" + e.getMessage());
		}
	}
	
	public void run() {
		try{
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (Exception e) {
			System.out.println("#Socket �����������ȡʧ��" + e.getMessage());
		}
		
		while (runflag) {
			try {
				final String msg = msgHandle();
				//System.out.println(">> " + msg);
				onReceive(addr, msg);
			} catch (Exception e) {
				runflag = false;
				System.out.println("#Socket ��ȡʧ��" + e.getMessage());
			}			
		}
		
		try {
			socket.close();
			in.close();
			out.close();
			socket = null;
			in = null;
			out = null;
		} catch (Exception e) {
			System.out.println("#Socket �ر��쳣" + e.getMessage());
		}
		
		onDisconnect(addr);
	}
	
	/****************************
	 * �������ݸ���type�Ĳ�ͬ����ͬ
	 * type = 0�������ַ���; type = 1, �½�socket,����head + content
	 * #param s
	 * #param type
	 * #return
	 */	
	public boolean send(String s, int type) {
		if (out != null) {
			try {
				out.writeInt(type);  //���͵���������
				out.flush();
				if (type == 0) {
					out.writeUTF(s);
				    out.flush();
				}
				else {
					Socket socketSend = null;
					DataOutputStream dout = null;
					DataInputStream din = null;
					boolean isSend = true;
					
					//����
					try {
						socketSend = new Socket(addr.getHostAddress(), portSend);
						dout = new DataOutputStream(socketSend.getOutputStream());
						din  =new DataInputStream(socketSend.getInputStream());
					} catch (Exception e) {
						System.out.println("#Sever�˵�socketSend����ʧ�� " + e.getMessage());
						isSend = false;
					}
					
					//�����ļ�
					File file = new File(s);
					String head = "Length=" + file.length() + "; Name=" + file.getName() + "; Path=" + file.getPath() + ";\r\n";
					dout.writeUTF(head);
					FileInputStream fin = new FileInputStream(file);
					byte[] buf = new byte[1024];
					/*int num = fin.read(buf);
					while(num != -1) {
						dout.write(buf);
						dout.flush();
						num = fin.read(buf);
					}*/
					int length = 0;
					while ((length = fin.read(buf, 0, buf.length)) > 0) {
						dout.write(buf, 0, length);
						dout.flush();
					}
					fin.close();
					 //����������Ļ��ԣ� һ��Ҫ������䣬����ͻ��˽��ܲ�����Ϣ 
					/*socket.shutdownOutput();
					
					//�Ѿ����յ��ͻ��˵�ACK
					while (isSend) {
						String ret = din.readUTF();
						if (ret.equals("OK")) {
							System.out.println("#Server�˵�socketSend���ر�����");
							break;
						}
					}*/
					//ֱ�ӶϿ����ӾͿ����ˣ�����
					if (socketSend != null)
						try {
							din.close();
					        dout.close();
					        socketSend.close();
					        din = null;
					        dout = null;
					        socketSend = null;
					    } catch (Exception e) {
					    	System.out.println("#Server�˵�socketSend�ر�ʧ�� " + e.getMessage());
					    }
				}
				return true;
			} catch (Exception e) {
				System.out.println("#Socket������� " + e.getMessage());
			}
		}
		
		return false;
	}
	
	public String msgHandle() {
		try {
			int type = in.readInt();
			
			System.out.println("type = " + type);
			
			if (type == 0)
				return in.readUTF();
			else {
				ServerSocket tmpServer = null;
				Socket socketRecv = null;
				DataOutputStream dout = null;
				DataInputStream din = null;
				//�����˿ڣ���������
				try {
					tmpServer = new ServerSocket(portRecv);
				    socketRecv = tmpServer.accept();
				} catch (Exception e) {
					System.out.println("#Server in msgHandler����ʧ�� " + e.getMessage());
					return null;
				}
				
				try {
					dout = new DataOutputStream(socketRecv.getOutputStream());
					din = new DataInputStream(socketRecv.getInputStream());
				} catch(Exception e) {
					 System.out.println("#socketRecv�½����������ʧ�� " + e.getMessage());
					 tmpServer.close();
					 return null;
				}
				String head = din.readUTF(); //����readLine
				if (head != null) {
				    String[] items = head.split(";");
					String fileName = items[1].substring(items[1].indexOf("=") + 1);
					File fileRecv = new File(savePath, fileName);
					if (fileRecv.exists()){
						fileName = fileName.substring(0, fileName.indexOf(".")-1) + "-" + System.currentTimeMillis() + fileName.substring(fileName.indexOf("."));
						fileRecv = new File(savePath, fileName);
					}
					FileOutputStream fout = new FileOutputStream(fileRecv);
					byte[] buf = new byte[1024];
					int num =  din.read(buf);
					while (num > 0) {
					    fout.write(buf, 0, num);
					    num =  din.read(buf);
					 }
					 fout.close();
				  }	
				  //�����ļ��ɹ����ظ�
				  //dout.writeUTF("OK");
				    
				  //ֱ�ӹر�����������Ϳ����ˣ�������
				  try {
					din.close();
					dout.close();
					socketRecv.close();
					tmpServer.close();
					din = null;
					dout = null;
					socketRecv = null;
					tmpServer = null;
				  } catch (Exception e) {
					System.out.println("#socketRecv��socketServer�ر�ʧ�� " + e.getMessage());
				  }				    
				  return head;				    																
			}
		} catch(Exception e) {
			System.out.println("#Socket��ȡʧ��in msgHanler " + e.getMessage());
		}
		
		return null;
	}
	
	public abstract void onReceive (InetAddress addr, String msg);
	
	public abstract void onDisconnect(InetAddress addr);
}
