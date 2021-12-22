package ru.kpfu.itis.ibragimovaidar.player;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.kpfu.itis.ibragimovaidar.GameCycleStage;
import ru.kpfu.itis.ibragimovaidar.net.message.Message;
import ru.kpfu.itis.ibragimovaidar.net.message.MessageType;

import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Player implements Runnable {

	private static final AtomicInteger availableId = new AtomicInteger(1);

	@Getter
	private final PlayerConnection connection;

	public Player(Socket socket) {
		this.connection = new PlayerConnection(socket);
	}

	@Getter
	private PlayerInfo playerInfo;

	@Getter
	private volatile PlayerStatus playerStatus = PlayerStatus.WAITING_FOR_PLAYER;

	@Getter
	@Setter
	private volatile GameCycleStage gameCycleStage = GameCycleStage.WAITING_FOR_PLAYERS;

	@Getter
	@Setter
	private volatile boolean hasMessagesToWrite = false;

	@Getter
	private final Queue<Message> messagesToWrite = new LinkedList<>();

	@Getter
	private volatile boolean readyToWriteMessages = false;

	//game lifecycle
	@SneakyThrows
	@Override
	public void run() {
		log.info("Player thread started, player:{}", this);
		// waiting for player
		if (playerStatus.equals(PlayerStatus.WAITING_FOR_PLAYER)){
			Message message = connection.readMessage(MessageType.JOIN_REQUEST);
			String username = message.getPayload();
			playerInfo = new PlayerInfo(availableId.incrementAndGet(), username, 1000);
			Message joinResponseMessage = new Message(MessageType.JOIN_RESPONSE, playerInfo.getId().toString());
			connection.writeMessage(joinResponseMessage);
		}
		playerStatus = PlayerStatus.READY_TO_START;
		while (!gameCycleStage.equals(GameCycleStage.IN_PROGRESS)) {
			Thread.sleep(200);
		}
		log.debug(Thread.currentThread() + ", game stage: " + gameCycleStage);
		// start game
		Message startGameMessage = new Message(MessageType.START_GAME, null);
		connection.writeMessage(startGameMessage);
		playerStatus = PlayerStatus.IN_PROCESS;

		while (gameCycleStage.equals(GameCycleStage.IN_PROGRESS)) {
			while (messagesToWrite.size() > 0) {
				Message message = messagesToWrite.poll();
				connection.writeMessage(message);
			}
			Message requestGameStatusMessage = new Message(MessageType.REQUEST_GAME_STATUS, null);
			connection.writeMessage(requestGameStatusMessage);
			Message responseGameStatusMessage = connection.readMessage(MessageType.RESPONSE_GAME_STATUS);
			int health = Integer.parseInt(responseGameStatusMessage.getPayload());
			playerInfo.setHealth(health);
			if (health <= 0) {
				playerStatus = PlayerStatus.END;
				gameCycleStage = GameCycleStage.END;
			}
			Thread.sleep(100);
		}
	}
}
