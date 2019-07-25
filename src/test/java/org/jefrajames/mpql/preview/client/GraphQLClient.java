package org.jefrajames.mpql.preview.client;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.jefrajames.mpql.preview.client.GraphQLException;

/**
 * Minimalistic GraphQL client for MicroProfile. Based on JSON-P.
 * 
 * Inspired from https://github.com/ejoebstl/graphql-java-client. 
 */
public class GraphQLClient {

    private URL endpoint;
    private Map<String, String> headers;

    /**
     * Creates a new instance of this class for the given GraphQL endpoint.
     *
     * @param endpoint The endpoint to use.
     */
    public GraphQLClient(URL endpoint) {
        this(endpoint, new HashMap<String, String>());
    }

    /**
     * Creates a new instance of this class for the given GraphQL endpoint and
     * request headers.
     *
     * @param endpoint The endpoint to use.
     * @param headers The headers to use for each request.
     */
    public GraphQLClient(URL endpoint, Map<String, String> headers) {
        this.endpoint = endpoint;
        this.headers = headers;
    }
    

    /**
     * Executes the given query or mutation.
     *
     * @param query The query or mutation to execute.
     * @param mapper A function object to convert the payload from JSON to an
     * actual object.
     * @param <T> The desired return object type.
     * @return The result of the query, with the mapper function applied.
     * @throws IOException Thrown in case of connection errors.
     * @throws GraphQLException Thrown in case of errors set in the response
     * body.
     */
    public <T> T execute(String query, Function<JsonObject, T> mapper) throws IOException, GraphQLException {
        return execute(query, Json.createObjectBuilder().build(), mapper);
    }

    /**
     * Executes the given query or mutation.
     *
     * @param query The query or mutation to execute.
     * @param variables The variables to pass to the query or mutation.
     * @param mapper A function object to convert the payload from JSON to an
     * actual object.
     * @param <T> The desired return object type.
     * @return The result of the query, with the mapper function applied.
     * @throws IOException Thrown in case of connection errors.
     * @throws GraphQLException Thrown in case of errors set in the response
     * body.
     */
    public <T> T execute(String query, JsonObject variables, Function<JsonObject, T> mapper) throws IOException, GraphQLException {
        JsonObject body = Json.createObjectBuilder().add("query", query).add("variables", variables).build();

        String responseString = execute(endpoint.toString(), body.toString(), headers);

        JsonReader jsonReader = Json.createReader(new StringReader(responseString));

        JsonObject response = jsonReader.readObject();

        if (response.containsKey("error")) {
            throw new GraphQLException(response.get("error").toString());
        }

        if (response.containsKey("errors")) {
            StringBuilder message = new StringBuilder();
            JsonArray errors = response.getJsonArray("errors");
            for (int i = 0; i < errors.size(); i++) {
                JsonObject error = errors.getJsonObject(i);
                message.append(error.toString());
            }
            throw new GraphQLException(message.toString());
        }

        // Convert the JSON data payload
        return mapper.apply(response.get("data").asJsonObject());
    }

    private static String execute(String url, String jsonEntity, Map<String, String> httpHeaders) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        if (httpHeaders != null) {
            for (Map.Entry<String, String> header : httpHeaders.entrySet()) {
                httpPost.addHeader(header.getKey(), header.getValue());
            }
        }

        StringEntity entity = new StringEntity(jsonEntity, ContentType.APPLICATION_JSON);

        httpPost.setEntity(entity);

        //Execute and get the response.
        HttpResponse response = httpClient.execute(httpPost);

        InputStream contentStream = response.getEntity().getContent();

        String contentString = IOUtils.toString(contentStream, "UTF-8");

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new HttpResponseException(response.getStatusLine().getStatusCode(), "The server responded with" + contentString);
        }

        return contentString;
    }
}
