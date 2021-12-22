package ru.kpfu.itis.ibragimovaidar;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.kpfu.itis.ibragimovaidar.net.message.Message;
import ru.kpfu.itis.ibragimovaidar.net.message.MessageType;
import ru.kpfu.itis.ibragimovaidar.player.Player;
import ru.kpfu.itis.ibragimovaidar.player.PlayerStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

@Slf4j
public class Game implements Runnable {

	@Getter
	@Setter
	private volatile GameCycleStage stage = GameCycleStage.WAITING_FOR_PLAYERS;

	private final Player player1;
	private final Player player2;

	private Instant startGameInstant;
	private final Random random = new Random();

	public Game(Player player1, Player player2) {
		this.player1 = player1;
		this.player2 = player2;
	}

	@SneakyThrows
	@Override
	public void run() {
		log.info("Game lifecycle started");
		while (!player1.getPlayerStatus().equals(PlayerStatus.READY_TO_START) ||
				!player2.getPlayerStatus().equals(PlayerStatus.READY_TO_START)){
			log.info((Thread.currentThread() + " " + player1.getPlayerStatus()));
			log.info(Thread.currentThread() + " " + player2.getPlayerStatus());
			Thread.sleep(500);
		}
		setStage(GameCycleStage.IN_PROGRESS);
		startGameInstant = Instant.now();
		// ship generation
		while (!player1.getGameCycleStage().equals(GameCycleStage.END) &&
				!player2.getGameCycleStage().equals(GameCycleStage.END)){
			int shipsCount = (int) (Duration.between(startGameInstant, Instant.now()).toSeconds() / 15) + 1;
			log.info(shipsCount + "");
			for (int i = 0; i < shipsCount; i++) {
				Message createShipMessage = new Message(MessageType.CREATE_SHIP, random.nextInt(2) + "/" + (random.nextInt(4) + 1));
				player1.getMessagesToWrite().offer(createShipMessage);
				player2.getMessagesToWrite().offer(createShipMessage);
			}
			player1.setHasMessagesToWrite(true);
			player2.setHasMessagesToWrite(true);
			// Message createShipMessage = new Message(MessageType.CREATE_SHIP, "1/3");

			Thread.sleep(3000);
		}
		player1.setGameCycleStage(GameCycleStage.END);
		player2.setGameCycleStage(GameCycleStage.END);
		Message endGameMessage = null;
		if (player1.getPlayerInfo().getHealth() <= 0){
			endGameMessage = new Message(MessageType.END_GAME, player2.getPlayerInfo().getId().toString());
		} else {
			endGameMessage = new Message(MessageType.END_GAME, player1.getPlayerInfo().getId().toString());
		}
		player1.getConnection().writeMessage(endGameMessage);
		player2.getConnection().writeMessage(endGameMessage);
	}

	private void setStage(GameCycleStage stage){
		this.stage = stage;
		player1.setGameCycleStage(stage);
		player2.setGameCycleStage(stage);
	}
}
