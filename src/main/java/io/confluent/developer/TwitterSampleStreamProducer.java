package io.confluent.developer;

import io.confluent.developer.avro.Tweet;
import org.apache.commons.lang3.SerializationException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.Producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class TwitterSampleStreamProducer {

    private static HttpResponse getTwitterSampleStreamResponse(String bearerToken, String uri) throws IOException, URISyntaxException {
        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        URIBuilder uriBuilder = new URIBuilder(uri);

        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.setHeader("Authorization", String.format("Bearer %s", bearerToken));
        HttpResponse response = httpClient.execute(httpGet);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
            throw new HttpResponseException(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
        }

        return response;
    }

    private static HttpEntity getResponseEntity(HttpResponse response) {
        HttpEntity entity = response.getEntity();
        return entity;
    }

    private static void produceToTopic (HttpEntity entity, Producer<String, Tweet> producer, String topic) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader((entity.getContent())));
        String line = reader.readLine();
        ObjectMapper objectMapper = new ObjectMapper();

        while (!ObjectUtils.isEmpty(line)) {
            JsonNode jsonNode = objectMapper.readTree(line);
            String key = jsonNode.path("data").path("id").asText();
            JsonNode record = jsonNode.path("data");
            Tweet value = Tweet.newBuilder()
                    .setId(record.path("id").asText())
                    .setConversationId(record.path("conversation_id").asText())
                    .setAuthorId(record.path("author_id").asText())
                    .setCreatedAt(record.path("created_at").asText())
                    .setText(record.path("text").asText())
                    .setPossiblySensitive(record.path("possibly_sensitive").asBoolean())
                    .build();
            try {
                producer.send(new ProducerRecord<>(topic, key, value), (metadata, e) -> {
                    if (!ObjectUtils.isEmpty(e)) {
                        e.printStackTrace();
                    } else {
                        System.out.printf("Produced record to topic %s partition [%d] @ offset %d%n", metadata.topic(), metadata.partition(), metadata.offset());
                    }
                });
                line = reader.readLine();
            } catch (SerializationException e) {
                e.printStackTrace();
            }
        }
    }

    public static Properties loadProperties(String fileName) throws IOException {
        if (!Files.exists(Paths.get(fileName))) {
            throw new IOException(fileName + " not found.");
        }
        final Properties props = new Properties();
        final FileInputStream input = new FileInputStream(fileName);
        props.load(input);
        input.close();
        return props;
    }


    public static void main(String[] args) throws IOException, URISyntaxException {
        if(args.length != 1){
            throw new IllegalArgumentException("Please provide the configuration/properties file path as a command line argument");
        }

        final Properties props = TwitterSampleStreamProducer.loadProperties(args[0]);
        final String topic = props.getProperty("output.topic.name");
        final Producer<String, Tweet> producer = new KafkaProducer<>(props);

        Dotenv dotenv = Dotenv.load();
        String bearerToken = dotenv.get("BEARER_TOKEN");
        String uri = "https://api.twitter.com/2/tweets/sample/stream?tweet.fields=id,conversation_id,created_at,author_id,text,possibly_sensitive";

        if (!ObjectUtils.isEmpty(bearerToken)) {
            HttpResponse response = getTwitterSampleStreamResponse(bearerToken, uri);
            HttpEntity responseEntity = getResponseEntity(response);
            if (!ObjectUtils.isEmpty(responseEntity)) {
                produceToTopic(responseEntity, producer, topic);
            }
        } else {
            System.out.println("Please set a valid bearer token in a .env file to access the Twitter API");
        }
    }
}

