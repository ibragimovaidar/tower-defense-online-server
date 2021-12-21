package ru.kpfu.itis.ibragimovaidar;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.kpfu.itis.ibragimovaidar.player.Player;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

@Slf4j
public class TowerDefenseOnlineServer implements AutoCloseable {

	private final Queue<Player> players = new LinkedList<>();

	private final ServerSocket serverSocket;

	public TowerDefenseOnlineServer() {
		try {
			serverSocket = new ServerSocket(7777);
		} catch (IOException e){
			throw new RuntimeException(e);
		}
	}

	private boolean running;

	@SneakyThrows
	public void run() {
		running = true;
		while (running) {
			Socket socket = serverSocket.accept();
			Player player = new Player(socket);
			players.offer(player);
			new Thread(player).start();

			if (players.size() >= 2){
				new Thread(new Game(players.poll(), players.poll())).start();
			}
		}
	}

	public void stop(){
		running = false;
	}

	public static void main(String[] args) {
		new TowerDefenseOnlineServer().run();
	}

	@Override
	public void close() throws Exception {
		serverSocket.close();
	}
}
