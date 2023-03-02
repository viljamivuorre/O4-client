package oy.tol.chatclient;

import oy.tol.chat.Message;

public interface ChatClientDataProvider {
	String getServer();
	String getNick();
	void handleReceived(Message message);
}
