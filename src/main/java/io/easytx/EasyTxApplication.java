package io.easytx;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import io.easytx.service.TransactionService;

@SpringBootApplication
@EnableAspectJAutoProxy
public class EasyTxApplication {

	public static void main(String[] args) {
		SpringApplication.run(EasyTxApplication.class, args);
	}

	@Bean
	public CommandLineRunner demo(TransactionService transactionService) {
		/*
		 * return args -> {
		 * transactionService.writeTransactionExample("Hello transactional world!");
		 * transactionService.readTransactionExample().forEach(System.out::println); };
		 */
		return args -> {
		};
	}

}
