package ru.kpfu.itis.ibragimovaidar;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.kpfu.itis.ibragimovaidar.message.Message;
import ru.kpfu.itis.ibragimovaidar.message.MessageType;
import ru.kpfu.itis.ibragimovaidar.player.Player;
import ru.kpfu.itis.ibragimovaidar.player.PlayerStatus;

@Slf4j
public class Game implements Runnable {

	@Getter
	@Setter
	private volatile GameCycleStage stage = GameCycleStage.WAITING_FOR_PLAYERS;

	private final Player player1;
	private final Player player2;


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
			Thread.sleep(200);
		}
		setStage(GameCycleStage.IN_PROGRESS);
		player1.notify();
		player2.notify();

		// ship generation
		while (!player1.getGameCycleStage().equals(GameCycleStage.END) &&
				!player2.getGameCycleStage().equals(GameCycleStage.END) &&
				!player1.isReadyToAcceptsGameStatus() &&
				!player2.isReadyToAcceptsGameStatus()
		){
			Message createShipMessage = new Message(MessageType.CREATE_SHIP, "1");
			player1.getConnection().writeMessage(createShipMessage);
			player2.getConnection().writeMessage(createShipMessage);
			player1.setReadyToAcceptsGameStatus(true);
			player2.setReadyToAcceptsGameStatus(true);
			Thread.sleep(1000);
		}

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
