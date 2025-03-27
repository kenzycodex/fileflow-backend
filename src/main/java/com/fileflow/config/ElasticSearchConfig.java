package com.fileflow.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.io.IOException;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.fileflow.repository.search")
@Profile("elasticsearch")
@Slf4j
public class ElasticSearchConfig {

    @Value("${elasticsearch.host:localhost}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private int port;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Bean(destroyMethod = "close")
    public RestHighLevelClient elasticsearchClient() {
        log.info("Configuring Elasticsearch client with host: {}, port: {}", host, port);

        // Configure credentials provider if authentication is needed
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        if (!username.isEmpty() && !password.isEmpty()) {
            log.info("Using basic authentication for Elasticsearch");
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password)
            );
        }

        // Build the REST client
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, "http"))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    if (!username.isEmpty() && !password.isEmpty()) {
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                    return httpClientBuilder
                            .setSSLHostnameVerifier((s, sslSession) -> true) // For dev HTTPS
                            .setMaxConnTotal(100)
                            .setMaxConnPerRoute(10);
                })
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(5000)
                                .setSocketTimeout(60000));

        RestHighLevelClient client = new RestHighLevelClient(builder);

        // Test connection with proper RequestOptions
        try {
            boolean pingSuccess = client.ping(RequestOptions.DEFAULT);
            log.info("Elasticsearch connection test successful: {}", pingSuccess);
        } catch (IOException e) {
            log.warn("Could not connect to Elasticsearch: {}", e.getMessage());
            log.debug("Connection error details", e);
        }

        return client;
    }
}