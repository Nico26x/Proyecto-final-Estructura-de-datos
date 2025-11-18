package co.edu.uniquindio.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada principal de la aplicación SyncUp.
 * <p>
 * Aplicación REST de streaming de música construida con Spring Boot.
 * Implementa funcionalidades de catálogo de canciones, redes sociales,
 * gestión de favoritos y recomendaciones personalizadas.
 * </p>
 * <p>
 * Características principales:
 * </p>
 * <ul>
 *   <li>Autenticación y autorización con JWT</li>
 *   <li>CRUD de usuarios y canciones</li>
 *   <li>Gestión de favoritos</li>
 *   <li>Redes sociales (seguir/dejar de seguir)</li>
 *   <li>Recomendaciones basadas en similitud</li>
 *   <li>Exportación de datos a CSV</li>
 *   <li>Métricas y análisis</li>
 * </ul>
 *
 * @author SyncUp
 * @version 1.0
 */
@SpringBootApplication
public class SyncUpApplication {

	/**
	 * Método principal que inicia la aplicación Spring Boot.
	 *
	 * @param args argumentos de línea de comandos (opcional)
	 */
	public static void main(String[] args) {
		SpringApplication.run(SyncUpApplication.class, args);
	}
}
