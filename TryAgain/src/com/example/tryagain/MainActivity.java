package com.example.tryagain;
import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements OnClickListener {

	private ToggleButton bnConnect;
	private TextView txReceive;
	private EditText edIP, edPort, edData;
	private Button bnSendMsg, bnSendFile;

	private Handler handler = new Handler(Looper.getMainLooper());

	private TcpClient client = new TcpClient() {

		@Override
		public void onConnect(SocketTransceiver transceiver) {
			refreshUI(true);
		}

		@Override
		public void onDisconnect(SocketTransceiver transceiver) {
			refreshUI(false);
		}

		@Override
		public void onConnectFailed() {
			handler.post(new Runnable() {
				@Override
				public void run() {
					bnConnect.setChecked(false);
					Toast.makeText(MainActivity.this, "连接失败",
							Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public void onReceive(SocketTransceiver transceiver, final String s) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					txReceive.append(s + "\r\n");
				}
			});
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		bnSendFile = (Button)this.findViewById(R.id.btn_sndFile);
		bnSendFile.setOnClickListener(this);
		bnSendMsg = (Button)this.findViewById(R.id.bn_send);
		bnSendMsg.setOnClickListener(this);
		bnConnect = (ToggleButton) this.findViewById(R.id.bn_connect);
		bnConnect.setOnClickListener(this);

		edIP = (EditText) this.findViewById(R.id.ed_ip);
		edPort = (EditText) this.findViewById(R.id.ed_port);
		edData = (EditText) this.findViewById(R.id.ed_dat);
		txReceive = (TextView) this.findViewById(R.id.tx_receive);
		txReceive.setOnClickListener(this);

		refreshUI(false);
	}

	@Override
	public void onStop() {
		client.disconnect();
		super.onStop();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bn_connect:
			connect();
			break;
		case R.id.bn_send:
			sendStr();
			break;
		case R.id.tx_receive:			
			clear();
			break;
		case R.id.btn_sndFile:
			sendFile();
			break;
		}
	}

	/**
	 * 刷新界面显示
	 * 
	 * @param isConnected
	 */
	private void refreshUI(final boolean isConnected) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				edPort.setEnabled(!isConnected);
				edIP.setEnabled(!isConnected);
				bnConnect.setChecked(isConnected);
				//bnConnect.setText(isConnected ? "断开" : "连接");
			}
		});
	}

	/**
	 * 设置IP和端口地址,连接或断开
	 */
	private void connect() {
		if (client.isConnected()) {
			// 断开连接
			client.disconnect();
		} else {
			try {
				String hostIP = edIP.getText().toString();
				int port = Integer.parseInt(edPort.getText().toString());
				client.connect(hostIP, port);
			} catch (NumberFormatException e) {
				Toast.makeText(this, "端口错误", Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
		}
	}

	/**
	 * 发送数据
	 */
	private void sendStr() {
		try {
			String data = edData.getText().toString();
			client.getTransceiver().send(data, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		handler.post(new Runnable() {
			@Override
			public void run() {
				edData.setText("");
			}
		});
		
	}
	
	private void sendFile() {
		try {
			File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/1_socket/fromServer.txt");
			txReceive.append("<<Preparing to send file in :" + file.getPath());			
			client.getTransceiver().send(file.getPath(), 1);
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	/**
	 * 清空接收框
	 */
	private void clear() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				bnConnect.setChecked(false);
				Toast.makeText(MainActivity.this, "Clear!",
						Toast.LENGTH_SHORT).show();
			}
		});
		new AlertDialog.Builder(this).setTitle("确认清除?")
				.setNegativeButton("取消", null)
				.setPositiveButton("确认", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						txReceive.setText("");
					}
				}).show();
	}
}