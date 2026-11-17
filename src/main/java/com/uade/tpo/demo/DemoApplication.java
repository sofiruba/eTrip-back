package com.uade.tpo.demo;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		// Las fechas de las experiencias son hora argentina: así la regla de 48 h y "ya empezó" no dependen
		// de la zona horaria de la máquina donde corre el back
		TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
		SpringApplication.run(DemoApplication.class, args);
	}

}
