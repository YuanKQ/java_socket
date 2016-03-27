package com.example.tryagain;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Environment;

/**
 * Socket�շ��� ͨ��Socket�������ݣ���ʹ�����̼߳���Socket���յ�������
 * 
 * @author jzj1993
 * @since 2015-2-22
 */
public abstract class SocketTransceiver implements Runnable {

	protected Socket socket;
	private String hostIP;
	protected InetAddress addr;
	protected DataInputStream in;
	protected DataOutputStream out;
	private boolean runFlag;
    private final File savePath;
	
	private final int portSend = 61010;
	private final int portRecv = 60000;
	/**
	 * ʵ����
	 * 
	 * @param socket
	 *            �Ѿ��������ӵ�socket
	 */
	public SocketTransceiver(Socket socket, String hostIP) {
		this.socket = socket;
		this.hostIP = hostIP;
		this.addr = socket.getInetAddress();
		savePath = new File(Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS), "ndnFile");
	}

	/**
	 * ��ȡ���ӵ���Socket��ַ
	 * 
	 * @return InetAddress����
	 */
	public InetAddress getInetAddress() {
		return addr;
	}

	/**
	 * ����Socket�շ�
	 * <p>
	 * �������ʧ�ܣ���Ͽ����Ӳ��ص�{@code onDisconnect()}
	 */
	public void start() {
		runFlag = true;
		new Thread(this).start();
	}

	/**
	 * �Ͽ�����(����)
	 * <p>
	 * ���ӶϿ��󣬻�ص�{@code onDisconnect()}
	 */
	public void stop() {
		runFlag = false;
		try {
			socket.shutdownInput();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * �����ַ���
	 * 
	 * @param s
	 *            �ַ���
	 * @return ���ͳɹ�����true
	 */
	public boolean send(String s, int type) {
		if (out != null) {
			try {
				out.writeInt(type);
				out.writeUTF(s);
				out.flush();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * ����Socket���յ�����(���߳�������)
	 */
	@Override
	public void run() {
		try {
			in = new DataInputStream(this.socket.getInputStream());
			out = new DataOutputStream(this.socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
			runFlag = false;
		}
		while (runFlag) {
			try {
				final int type = in.readInt();
				if (type == 0){
					final String s = in.readUTF();
					this.onReceive(addr, s);
				} else {
					downloadFile();  //����onReceive(addr, s)����ʾ�ļ����յĽ���
				}
				
			} catch (IOException e) {
				// ���ӱ��Ͽ�(����)
				runFlag = false;
			}
		}
		// �Ͽ�����
		try {
			in.close();
			out.close();
			socket.close();
			in = null;
			out = null;
			socket = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.onDisconnect(addr);
	}
	
	/**
	 * �������Է������˵��ļ�
	 */
	private void downloadFile() {
		ServerSocket tmpServer = null;
		Socket socketRecv = null;
		DataInputStream din = null;
		DataOutputStream dout = null;
		
		try {
			tmpServer = new ServerSocket(portRecv);
			socketRecv = tmpServer.accept();
		} catch (Exception e) {
			System.out.println("@tmpServer in Client����ʧ�� " + e.getMessage());
			//return null;
			onReceive(addr, "Error�����ܷ��������ʧ�ܣ�");
		}
		
		try {
			din = new DataInputStream(socketRecv.getInputStream());
			dout = new DataOutputStream(socketRecv.getOutputStream());
		} catch (Exception e) {
			System.out.println("@socketRecv in client�������������ʧ�� " + e.getMessage());
			onReceive(addr, "***Error�����ܷ��������ʧ�ܣ�");
		}
		
		try {
			String head = din.readUTF(); //����readLine
			System.out.println("***" + head);
			
			if (head != null) {
				String[] items = head.split(";");
				String fileName = items[1].substring(items[1].indexOf("=") + 1);
				File fileRecv = new File(savePath, fileName);
				if (fileRecv.exists()){
					fileName = fileName.substring(0, fileName.indexOf(".")-1) + "-" + System.currentTimeMillis() + fileName.substring(fileName.indexOf("."));
					fileRecv = new File(savePath, fileName);
				}
				
				onReceive(addr, "***׼�������ļ� " + fileName);  //����Ϊ����ˢ��
				
				FileOutputStream fout = new FileOutputStream(fileRecv);
				byte[] buf = new byte[1024];
				/*int num =  din.read(buf);
				while (num != -1) {
					fout.write(buf, 0, num);
					fout.flush();
					num =  din.read(buf);
				}*/
				while (true) {
					int num = din.read(buf);
					if (num == -1)
						break;
					fout.write(buf, 0, num);
					fout.flush();
				}
				
				fout.close();
				
				onReceive(addr, "***�ļ� " + fileName + "�ѱ�����·����" + fileRecv.getPath());  //����Ϊ����ˢ��
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		//dout.writeUTF("OK");
		
		if (socketRecv != null)
			try {
				din.close();
				dout.close();
				socketRecv.close();
				tmpServer.close();
				din = null;
				dout = null;
				socketRecv = null;
			} catch (Exception e) {
				System.out.println("@socketRecv��tmpServer in client�ر�ʧ�ܣ� " + e.getMessage());
			}
		
	}

	/**
	 * ���յ�����
	 * <p>
	 * ע�⣺�˻ص��������߳���ִ�е�
	 * 
	 * @param addr
	 *            ���ӵ���Socket��ַ
	 * @param s
	 *            �յ����ַ���
	 */
	public abstract void onReceive(InetAddress addr, String s);

	/**
	 * ���ӶϿ�
	 * <p>
	 * ע�⣺�˻ص��������߳���ִ�е�
	 * 
	 * @param addr
	 *            ���ӵ���Socket��ַ
	 */
	public abstract void onDisconnect(InetAddress addr);
}