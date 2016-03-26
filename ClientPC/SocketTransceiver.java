import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class SocketTransceiver implements Runnable{
	protected Socket socket;
//	protected ServerSocket serverRecv;
	protected InetAddress addr;
	protected DataInputStream in;
	protected DataOutputStream out;
	private String hostIP;
	private boolean runFlag;
	private final String savePath = "D:/cache/Client/recv";
	
	private final int portSend = 61010;
	private final int portRecv = 60000;

	public SocketTransceiver(Socket socket, String hostIP) {
		this.socket = socket;
		this.hostIP = hostIP;
		this.addr = socket.getInetAddress();
	}
	
	public InetAddress getInetAddress() {
		return addr;
	}
	
	public void start() {
		runFlag = true;
		new Thread(this).start();
	}
	
	public void stop() {
		runFlag = false;
		try {
			socket.shutdownInput();
		} catch (Exception e) {
			System.out.println("@Socket�ر�ʧ�� " + e.getMessage());
		}
	}
	
	/****************************
	 * �������ݸ���type�Ĳ�ͬ����ͬ
	 * type = 0�������ַ���; type = 1, ����head + content
	 * @param s
	 * @param type
	 * @return
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
					DataInputStream din = null;
					DataOutputStream dout = null;
					boolean isSend = true;
					
					try {
						socketSend = new Socket(hostIP, portSend);
						din = new DataInputStream(socketSend.getInputStream());
						dout = new DataOutputStream(socketSend.getOutputStream());
					} catch (Exception e) {
						System.out.println("@Client�˵�socketSend����ʧ�� " + e.getMessage());
						isSend = false;
					}
					
					//�����ļ�
					File file = new File(s);
					String head = "Length=" + file.length() + "; Name=" + file.getName() + "; Path=" + file.getPath() + ";\r\n";
					dout.writeUTF(head);
					dout.flush();
					FileInputStream fin = new FileInputStream(file);
					byte[] buf = new byte[1024];
					int num = fin.read(buf);
					while (num != -1) {
						dout.write(buf, 0, num);
						dout.flush();
						num = fin.read(buf);
					}
					fin.close();
					
					/*socket.shutdownOutput();
					
					while (isSend) {
						String ret = din.readUTF();
						if (ret.equals("OK")) {
							System.out.println("@Client�˵�socketSend���ر�����");
							break;
						}
					}
					*/
					if (socketSend != null){
						din.close();
						dout.close();
						socketSend.close();
						din = null;
						dout = null;
						socketSend = null;
					}												
				}
				return true;
			} catch (Exception e) {
				System.out.println("@Socket������� " + e.getMessage());
			}
		}
		
		return false;
	}
	
	public void run() {
		try {
			in = new DataInputStream(this.socket.getInputStream());
			out = new DataOutputStream(this.socket.getOutputStream());
		} catch (Exception e) {
			System.out.println("@Socket�������������ʧ�� " + e.getMessage());
		}
		//��������
		while (runFlag) {
			try {
				final String s = msgHandle();
				this.onReceive(addr, s);
			} catch (Exception e) {
				runFlag = false;
			}
		}
		
		try {
			in.close();
			out.close();
			socket.close();
			in = null;
			out = null;
			socket = null;
		} catch (Exception e) {
			System.out.println("@Socket�ر�ʧ�� " + e.getMessage());
		}
	}
	
	public String msgHandle() {
		try {
			int type = in.readInt();
			
			if (type == 0)
				return in.readUTF();
			else {
				ServerSocket tmpServer = null;
				Socket socketRecv = null;
				DataInputStream din = null;
				DataOutputStream dout = null;
				
				try {
					tmpServer = new ServerSocket(portRecv);
					socketRecv = tmpServer.accept();
				} catch (Exception e) {
					System.out.println("@tmpServer in Client����ʧ�� " + e.getMessage());
					return null;
				}
				
				try {
					din = new DataInputStream(socketRecv.getInputStream());
					dout = new DataOutputStream(socketRecv.getOutputStream());
				} catch (Exception e) {
					System.out.println("@socketRecv in client�������������ʧ�� " + e.getMessage());
					tmpServer.close();
					return null;
				}
				
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
				
				return head;
			}
		} catch(Exception e) {
			System.out.println("@Socket��ȡʧ��in msgHanler " + e.getMessage());
		}
		
		return null;
	}
	
	public abstract void onReceive(InetAddress addr, String s);
	
	public abstract void onDisconnect(InetAddress addr);
}
