package io.ymon.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@Slf4j
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		new SpringApplicationBuilder()
				.sources(Application.class)
				.bannerMode(Mode.OFF)
				.run(args);
	}
}