package ru.kpfu.itis.ibragimovaidar.player;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;
import ru.kpfu.itis.ibragimovaidar.message.Message;
import ru.kpfu.itis.ibragimovaidar.message.MessageType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@Slf4j
@Getter
public class PlayerConnection implements AutoCloseable {

	private final Socket socket;
	private final ObjectInputStream inputStream;
	private final ObjectOutputStream outputStream;

	@SneakyThrows
	public PlayerConnection(Socket socket) {
		this.socket = socket;
		this.outputStream = new ObjectOutputStream(socket.getOutputStream());
		this.inputStream = new ObjectInputStream(socket.getInputStream());
	}

	@SneakyThrows
	public Message readMessage(){
		Message message = (Message) inputStream.readObject();
		log.info("Message received: {}", message);
		return message;
	}

	public Message readMessage(MessageType messageType){
		Message message = readMessage();
		if (!messageType.equals(message.getMessageType())){
			log.error("Message does not match the given message type: message:{}, messageType:{}", message, messageType);
			throw new MessageTypeException();
		}
		return message;
	}

	@SneakyThrows
	public void writeMessage(Message message){
		outputStream.writeObject(message);
		log.info("Message sent: {}", message);
	}

	@Override
	public void close() throws Exception {
		inputStream.close();
		outputStream.close();
		socket.close();
	}

	@StandardException
	public static class MessageTypeException extends RuntimeException {}
}
