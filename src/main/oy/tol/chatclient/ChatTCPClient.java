package oy.tol.chatclient;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONException;
import org.json.JSONObject;

import oy.tol.chat.ChatMessage;
import oy.tol.chat.ErrorMessage;
import oy.tol.chat.JoinMessage;
import oy.tol.chat.Message;
import oy.tol.chat.MessageFactory;

public class ChatTCPClient implements Runnable {

	private ChatClientDataProvider dataProvider = null;

	private static final DateTimeFormatter jsonDateFormatter = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

	private String certificateFile;
	private boolean useTlsInRequests = false;

	private Socket socket;
	private boolean running = true;

	private OutputStream writer;
	private DataInputStream inStream;

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
		String userName = dataProvider.getNick();
		ChatMessage msg = new ChatMessage(LocalDateTime.now(), userName, message);
		String jsonObjectString = msg.toJSON();
		write(jsonObjectString);
	}

	private synchronized void write(String message) throws IOException {
		ChatClient.println("Sending: " + message, ChatClient.colorInfo);
		byte [] msgBytes = message.getBytes(StandardCharsets.UTF_8);
		byte[] allBytes = new byte[msgBytes.length + 2];
		ByteBuffer byteBuffer = ByteBuffer.wrap(allBytes, 0, allBytes.length);
		short msgLen = (short)allBytes.length;
		ChatClient.println("Writing bytes: " + msgLen, ChatClient.colorInfo);
		byteBuffer = byteBuffer.putShort(msgLen);
		byteBuffer = byteBuffer.put(msgBytes);
		writer.write(allBytes);
	}

	@Override
	public void run() {
		while (running) {
			try {
				if (socket == null) {
					connect();
				}
				String data = "";
				byte[] sizeBytes = new byte[2];
				sizeBytes[0] = inStream.readByte();
				sizeBytes[1] = inStream.readByte();
				ByteBuffer byteBuffer = ByteBuffer.wrap(sizeBytes, 0, 2);
				int bytesToRead = byteBuffer.getShort();
	
				if (bytesToRead > 0) {
					int bytesRead = 0;
					byte[] messageBytes = new byte[bytesToRead];
					byteBuffer = ByteBuffer.wrap(messageBytes, 0, bytesToRead);
					while (bytesToRead > bytesRead) {
						byteBuffer.put(inStream.readByte());
						bytesRead++;
					}
					data = new String(messageBytes, 0, bytesRead, StandardCharsets.UTF_8);
					ChatClient.println("Received: " + data, ChatClient.colorInfo);
					handleMessage(data);
				}
			} catch (EOFException e) {
				ChatClient.println("ChatSession: EOFException", ChatClient.colorError);
				close();
			} catch (IOException e) {
				ChatClient.println("ChatSession: IOException", ChatClient.colorError);
				close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void connect() throws Exception {
		String address = dataProvider.getServer();
		ChatClient.println("Connecting to server " + address, ChatClient.colorError);
		String components [] = address.split(":");
		if (components.length == 2) {
			int port = Integer.parseInt(components[1]);
			socket = new Socket(components[0], port);
			writer = socket.getOutputStream();
			inStream = new DataInputStream(socket.getInputStream());
		} else {
			ChatClient.println("Invalid server address in settings", ChatClient.colorError);
		}
	}

	private void handleMessage(String data) {
		Message received = null;
		try {
			JSONObject jsonObject = new JSONObject(data);
			received = MessageFactory.fromJSON(jsonObject);
		} catch (JSONException e) {
			e.printStackTrace();
			received = new ErrorMessage("Invalid JSON message from client");
		}
		dataProvider.handleReceived(received);
	}

	public void close() {
		running = false;
		if (null != socket) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
	}

	public void changeChannelTo(String channel, String topic) throws IOException {
		JoinMessage msg = new JoinMessage(channel, topic);
		String jsonObjectString = msg.toJSON();
		write(jsonObjectString);
	}
}
