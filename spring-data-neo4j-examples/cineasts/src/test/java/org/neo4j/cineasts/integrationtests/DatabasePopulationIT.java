package org.neo4j.cineasts.integrationtests;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DatabasePopulationIT {
    @Test
    public void shouldPopulateDatabase() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://localhost:8080/populate");

        HttpResponse response = httpclient.execute(httpget);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
        assertThat(getContent(response), Matchers.containsString("Neo"));
    }

    private String getContent(HttpResponse response) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        response.getEntity().writeTo(buffer);
        return buffer.toString();
    }
}
