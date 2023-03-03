package oy.tol.chatclient;

import oy.tol.chat.Message;

public interface ChatClientDataProvider {
	String getServer();
	String getNick();
	boolean handleReceived(Message message);
	void connectionClosed();
}
