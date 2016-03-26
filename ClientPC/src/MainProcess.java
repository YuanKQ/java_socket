
public class MainProcess {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TcpClient c1 = new TcpClient() {
			public void onReceive(SocketTransceiver st, String s) {
				System.out.println("Client Receive:" + s);
				
			}
			
			public void onDisconnect(SocketTransceiver st) {
				System.out.println("Client Disconnect!");
			}
			
			public void onConnect(SocketTransceiver transceiver) {
				System.out.println("Client Connect!");
			}
			
			public void onConnectFailed() {
				System.out.println("Client Connect Failed!");
			}
		};
		
		c1.connect("127.0.0.1", 53536);
		
		TcpClient c2 = new TcpClient() {
			public void onReceive(SocketTransceiver st, String s) {
				System.out.println("Client Receive:" + s);
			}
			
			public void onDisconnect(SocketTransceiver st) {
				System.out.println("Client Disconnect!");
			}
			
			public void onConnect(SocketTransceiver transceiver) {
				System.out.println("Client Connect!");
			}
			
			public void onConnectFailed() {
				System.out.println("Client Connect Failed!");
			}
		};
		
		c2.connect("127.0.0.1", 53536);
		
		delay();		
		while (true) {
			if (c1.isConnected()) {
				c1.getTransceiver().send("Hello!", 0);
				delay();
				//sendFile
				c1.getTransceiver().send("D:/cache/Client/send/client1.txt", 1);
			}
			if (c2.isConnected()) {
				c2.getTransceiver().send("Fine!", 0);
				delay();
				//sendFile
				c2.getTransceiver().send("D:/cache/Client/send/client2.txt", 1);
			}
			
			delay();

		}
	}
	
	static void delay() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.out.println("#ÖÐ¶Ï´íÎó£¡ " + e.getMessage());
		}
	}

}
