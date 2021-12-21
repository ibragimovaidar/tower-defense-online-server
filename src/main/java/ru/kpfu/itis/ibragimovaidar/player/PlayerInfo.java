package ru.kpfu.itis.ibragimovaidar.player;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerInfo {

	private Integer id;
	private String username;
	private Integer health;
}
