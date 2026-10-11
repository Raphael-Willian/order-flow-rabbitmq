package dev.orderflow.payment_worker;

import com.orderflow.common.RabbitTopology;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RabbitTopology.class)
public class PaymentWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentWorkerApplication.class, args);
	}

}
