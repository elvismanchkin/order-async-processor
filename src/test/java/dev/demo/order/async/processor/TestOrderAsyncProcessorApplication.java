package dev.demo.order.async.processor;

import org.springframework.boot.SpringApplication;

public class TestOrderAsyncProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.from(OrderAsyncProcessorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
