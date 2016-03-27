package com.example.tryagain;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements OnClickListener {

	private ToggleButton bnConnect;
	private TextView txReceive;
	private EditText edIP, edPort, edData;

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
					Toast.makeText(MainActivity.this, "����ʧ��",
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

		this.findViewById(R.id.bn_send).setOnClickListener(this);
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
		}
	}

	/**
	 * ˢ�½�����ʾ
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
				//bnConnect.setText(isConnected ? "�Ͽ�" : "����");
			}
		});
	}

	/**
	 * ����IP�Ͷ˿ڵ�ַ,���ӻ�Ͽ�
	 */
	private void connect() {
		if (client.isConnected()) {
			// �Ͽ�����
			client.disconnect();
		} else {
			try {
				String hostIP = edIP.getText().toString();
				int port = Integer.parseInt(edPort.getText().toString());
				client.connect(hostIP, port);
			} catch (NumberFormatException e) {
				Toast.makeText(this, "�˿ڴ���", Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
		}
	}

	/**
	 * ��������
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

	/**
	 * ��ս��տ�
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
		new AlertDialog.Builder(this).setTitle("ȷ�����?")
				.setNegativeButton("ȡ��", null)
				.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						txReceive.setText("");
					}
				}).show();
	}
}