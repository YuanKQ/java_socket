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
 * Socket收发器 通过Socket发送数据，并使用新线程监听Socket接收到的数据
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
	 * 实例化
	 * 
	 * @param socket
	 *            已经建立连接的socket
	 */
	public SocketTransceiver(Socket socket, String hostIP) {
		this.socket = socket;
		this.hostIP = hostIP;
		this.addr = socket.getInetAddress();
		savePath = new File(Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS), "ndnFile");
	}

	/**
	 * 获取连接到的Socket地址
	 * 
	 * @return InetAddress对象
	 */
	public InetAddress getInetAddress() {
		return addr;
	}

	/**
	 * 开启Socket收发
	 * <p>
	 * 如果开启失败，会断开连接并回调{@code onDisconnect()}
	 */
	public void start() {
		runFlag = true;
		new Thread(this).start();
	}

	/**
	 * 断开连接(主动)
	 * <p>
	 * 连接断开后，会回调{@code onDisconnect()}
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
	 * 发送字符串
	 * 
	 * @param s
	 *            字符串
	 * @return 发送成功返回true
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
	 * 监听Socket接收的数据(新线程中运行)
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
					downloadFile();  //调用onReceive(addr, s)来显示文件接收的进度
				}
				
			} catch (IOException e) {
				// 连接被断开(被动)
				runFlag = false;
			}
		}
		// 断开连接
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
	 * 接受来自服务器端的文件
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
			System.out.println("@tmpServer in Client建立失败 " + e.getMessage());
			//return null;
			onReceive(addr, "Error：接受服务端数据失败！");
		}
		
		try {
			din = new DataInputStream(socketRecv.getInputStream());
			dout = new DataOutputStream(socketRecv.getOutputStream());
		} catch (Exception e) {
			System.out.println("@socketRecv in client输入输出流建立失败 " + e.getMessage());
			onReceive(addr, "***Error：接受服务端数据失败！");
		}
		
		try {
			String head = din.readUTF(); //或者readLine
			System.out.println("***" + head);
			
			if (head != null) {
				String[] items = head.split(";");
				String fileName = items[1].substring(items[1].indexOf("=") + 1);
				File fileRecv = new File(savePath, fileName);
				if (fileRecv.exists()){
					fileName = fileName.substring(0, fileName.indexOf(".")-1) + "-" + System.currentTimeMillis() + fileName.substring(fileName.indexOf("."));
					fileRecv = new File(savePath, fileName);
				}
				
				onReceive(addr, "***准备接收文件 " + fileName);  //仅作为界面刷新
				
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
				
				onReceive(addr, "***文件 " + fileName + "已保存在路径：" + fileRecv.getPath());  //仅作为界面刷新
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
				System.out.println("@socketRecv或tmpServer in client关闭失败！ " + e.getMessage());
			}
		
	}

	/**
	 * 接收到数据
	 * <p>
	 * 注意：此回调是在新线程中执行的
	 * 
	 * @param addr
	 *            连接到的Socket地址
	 * @param s
	 *            收到的字符串
	 */
	public abstract void onReceive(InetAddress addr, String s);

	/**
	 * 连接断开
	 * <p>
	 * 注意：此回调是在新线程中执行的
	 * 
	 * @param addr
	 *            连接到的Socket地址
	 */
	public abstract void onDisconnect(InetAddress addr);
}