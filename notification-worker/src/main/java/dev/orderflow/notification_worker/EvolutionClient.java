package dev.orderflow.notification_worker;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class EvolutionClient {


    private static final Logger log = LoggerFactory.getLogger(EvolutionClient.class);


    private final RestClient http;
    private final String instance;

    public EvolutionClient(EvolutionProperties properties) {
        this.instance = properties.instance();


        //Definindo versão do protocolo HTTP para melhor compatibilidade de chamadas entre servidores, juntamente ao timeout.
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(15));

        this.http = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.baseUrl())
                .defaultHeader("apiKey",properties.apiKey())
                .build();
    }

    public void sendText(String number, String text) {
        http.post()
                .uri("/message/sendText/{instance}")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("number",number, "text", text))
                .retrieve()
                .toBodilessEntity();

        log.info("Evolution API: mensagem enviada para {}", number);
    }



















}
