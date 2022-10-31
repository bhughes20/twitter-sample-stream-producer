package io.confluent.developer;

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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

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

    private static void produceToTopic (HttpEntity entity) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader((entity.getContent())));
        String line = reader.readLine();
        ObjectMapper objectMapper = new ObjectMapper();
        while (line != null) {
            JsonNode jsonNode = objectMapper.readTree(line);
            System.out.println(jsonNode.get("data"));
            line = reader.readLine();
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        Dotenv dotenv = Dotenv.load();
        String bearerToken = dotenv.get("BEARER_TOKEN");
        String uri = "https://api.twitter.com/2/tweets/sample/stream?tweet.fields=created_at&expansions=author_id";

        if (!ObjectUtils.isEmpty(bearerToken)) {
            HttpResponse response = getTwitterSampleStreamResponse(bearerToken, uri);
            HttpEntity responseEntity = getResponseEntity(response);
            if (!ObjectUtils.isEmpty(responseEntity)) {
                produceToTopic(responseEntity);
            }
        } else {
            System.out.println("Please provide a valid bearer token to access the Twitter API");
        }
    }
}

