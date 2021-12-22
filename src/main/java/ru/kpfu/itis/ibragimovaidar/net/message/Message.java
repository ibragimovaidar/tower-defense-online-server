package ru.kpfu.itis.ibragimovaidar.net.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@Builder
public class Message implements Serializable {

	private static final long serialVersionUID = 1L;

	private final MessageType messageType;
	private final String payload;
}
