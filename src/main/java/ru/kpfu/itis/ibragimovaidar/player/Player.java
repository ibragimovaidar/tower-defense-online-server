package ru.kpfu.itis.ibragimovaidar.player;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.kpfu.itis.ibragimovaidar.Game;
import ru.kpfu.itis.ibragimovaidar.GameCycleStage;
import ru.kpfu.itis.ibragimovaidar.message.Message;
import ru.kpfu.itis.ibragimovaidar.message.MessageType;

import java.net.Socket;
import java.util.Objects;
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
	private volatile boolean isReadyToAcceptsGameStatus = false;

	//game lifecycle
	@SneakyThrows
	@Override
	public void run() {
		log.info("Player thread started, player:{}", this);
		// waiting for player
		while (playerStatus.equals(PlayerStatus.WAITING_FOR_PLAYER)){
			Message message = connection.readMessage(MessageType.JOIN_REQUEST);
			String username = message.getPayload();
			playerInfo = new PlayerInfo(availableId.incrementAndGet(), username, 1000);

			Message joinResponseMessage = new Message(MessageType.JOIN_RESPONSE, playerInfo.getId().toString());
			connection.writeMessage(joinResponseMessage);
		}

		synchronized (this){
			playerStatus = PlayerStatus.READY_TO_START;
			while (!gameCycleStage.equals(GameCycleStage.IN_PROGRESS)){
				wait();
			}
		}

		// start game
		Message startGameMessage = new Message(MessageType.START_GAME, null);
		connection.writeMessage(startGameMessage);
		playerStatus = PlayerStatus.IN_PROCESS;

		while (gameCycleStage.equals(GameCycleStage.IN_PROGRESS)) {
			synchronized (this){
				while (!isReadyToAcceptsGameStatus){
					wait();
				}
				Message message = connection.readMessage(MessageType.SEND_GAME_STATUS);
				int health = Integer.parseInt(message.getPayload());
				playerInfo.setHealth(health);
				if (health <= 0){
					playerStatus = PlayerStatus.END;
					gameCycleStage = GameCycleStage.END;
				}
				isReadyToAcceptsGameStatus = false;
			}
		}
	}
}
