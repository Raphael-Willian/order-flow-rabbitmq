package dev.orderflow.notification_worker;

import com.orderflow.common.RabbitTopology;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ConfigurationPropertiesScan
@Import(RabbitTopology.class)
public class NotificationWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationWorkerApplication.class, args);
	}

}
