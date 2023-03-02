package oy.tol.chatclient;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import oy.tol.chat.ChatMessage;
import oy.tol.chat.ErrorMessage;
import oy.tol.chat.Message;

public class ChatTCPClient implements Runnable {

	private ChatClientDataProvider dataProvider = null;

	private static final DateTimeFormatter jsonDateFormatter = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

	private String certificateFile;
	private boolean useTlsInRequests = true;

	private Socket socket;
	private boolean running = true;

	ChatTCPClient(ChatClientDataProvider provider, String certificateFileWithPath) {
		dataProvider = provider;
		certificateFile = certificateFileWithPath;
		if (null == certificateFile) {
			useTlsInRequests = false;
		}
	}

	public synchronized void postChatMessage(String message)
			throws KeyManagementException, KeyStoreException, CertificateException,
			NoSuchAlgorithmException, IOException {
		String addr = dataProvider.getServer();
		String auth = dataProvider.getUsername();

		byte[] msgBytes;
		JSONObject msg = new JSONObject();
		msg.put("user", dataProvider.getNick());
		msg.put("message", message);
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
		String dateText = now.format(jsonDateFormatter);
		msg.put("sent", dateText);
		msgBytes = msg.toString().getBytes(StandardCharsets.UTF_8);
		OutputStream writer = socket.getOutputStream();
		writer.write(msgBytes);
		writer.close();
	}

	@Override
	public void run() {

		while (running) {
			try {
				DataInputStream inStream = new DataInputStream(socket.getInputStream());
				String data = "";
				byte[] sizeBytes = new byte[2];
				sizeBytes[0] = inStream.readByte();
				sizeBytes[1] = inStream.readByte();
				ByteBuffer byteBuffer = ByteBuffer.wrap(sizeBytes, 0, 2);
				int bytesToRead = byteBuffer.getShort();
				System.out.println("Read " + bytesToRead + " bytes");
	
				if (bytesToRead > 0) {
					int bytesRead = 0;
					byte[] messageBytes = new byte[bytesToRead];
					byteBuffer = ByteBuffer.wrap(messageBytes, 0, bytesToRead);
					while (bytesToRead > bytesRead) {
						byteBuffer.put(inStream.readByte());
						bytesRead++;
					}
					if (bytesRead == bytesToRead) {
						data = new String(messageBytes, 0, bytesRead, StandardCharsets.UTF_8);
						handleMessage(data);
					}
				}
			} catch (EOFException e) {
				System.out.println("ChatSession: EOFException");
				close();
			} catch (IOException e) {
				System.out.println("ChatSession: IOException");
				close();
			}
		}
	}

	private void handleMessage(String data) {
		Message received = null;
		try {
			JSONObject jsonObject = new JSONObject(data);
			int msgType = jsonObject.getInt("type");

			switch (msgType) {
				case Message.CHAT_MESSAGE:
					received = handleChatMessage(jsonObject);
					break;

				case Message.STATUS_MESSAGE:
					break;

				case Message.ERROR_MESSAGE:
					break;

				default: // Clients cannot send other message types.
					received = new ErrorMessage("Invalid message from client, not handled");
					break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
			received = new ErrorMessage("Invalid JSON message from client");
		}
		dataProvider.handleReceived(received);
	}

	private Message handleChatMessage(JSONObject jsonObject) {
		String userName = jsonObject.getString("user");
		String msg = jsonObject.getString("message");
		String dateStr = jsonObject.getString("sent");
		OffsetDateTime odt = OffsetDateTime.parse(dateStr);
		return new ChatMessage(odt.toLocalDateTime(), userName, msg);
	}

	public void close() {
		running = false;
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
