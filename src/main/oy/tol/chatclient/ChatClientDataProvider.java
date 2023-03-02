package oy.tol.chatclient;

import oy.tol.chat.Message;

public interface ChatClientDataProvider {
	String getServer();
	String getUsername();
	String getPassword();
	String getNick();
	String getEmail();
	void handleReceived(Message message);
}
