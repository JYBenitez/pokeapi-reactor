package skaro.trainer.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import skaro.trainer.domain.Trainer;
import skaro.trainer.persistence.TrainerRepository;

@Configuration
public class TrainerConfiguration {

	public static final String CONFIGURATION_PROPERTIES_PREFIX = "skaro.trainer";

	@Bean
	@ConfigurationProperties(CONFIGURATION_PROPERTIES_PREFIX)
	public TrainerConfigurationProperties trainerConfigurationProperties() {
		return new TrainerConfigurationProperties();
	}

	// Siembra el único Trainer de esta entrega (Restricciones de la spec:
	// "Entrenador fijo/único... constante de configuración o seed inicial
	// de la DB") si todavía no existe en el archivo H2.
	@Bean
	public CommandLineRunner seedDefaultTrainer(TrainerRepository trainerRepository,
			TrainerConfigurationProperties properties) {
		return args -> {
			Long defaultTrainerId = properties.getDefaultTrainerId();
			if (!trainerRepository.existsById(defaultTrainerId)) {
				trainerRepository.save(new Trainer(defaultTrainerId));
			}
		};
	}

}
