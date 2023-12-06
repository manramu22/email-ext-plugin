package hudson.plugins.emailext.webhooks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import jenkins.model.Jenkins;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class WebhooksUtil {

    public void sendMessage(String content, String jobBaseName) {
        String projectId = extractProjectId(jobBaseName);
        String webhookURL = getWebhookURL(projectId);
        String normalizedContent = content.replaceAll("[\\n\\r\\t]", "");
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("text", normalizedContent);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = null;
        try {
            jsonString = objectMapper.writeValueAsString(requestMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost postRequest = new HttpPost(webhookURL);
            StringEntity stringEntity = new StringEntity(jsonString);
            postRequest.addHeader("content-type", "application/json");
            postRequest.setEntity(stringEntity);
            httpClient.execute(postRequest);
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    private String extractProjectId(String jobBaseName) {
        try {
            String jobNameWithoutPrefix = jobBaseName.split("-")[1];
            return jobNameWithoutPrefix.split("_")[0];
        } catch (ArrayIndexOutOfBoundsException exception) {
            return "default";
        }
    }

    private String getWebhookURL(String jenkinsView) {
        // Pass CSV file path from Jenkins email-template folder
        Map<String, String> map = null;
        map = readHooksConfigFile();
        if (map.containsKey(jenkinsView)) {
            return map.get(jenkinsView);
        } else {
            return map.get("default");
        }
    }

    private HashMap<String, String> readHooksConfigFile() {
        String hooksConfigPath = Jenkins.get().getRootDir().getPath() + "/email-templates/hooks-config.csv";
        HashMap<String, String> map = new HashMap<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(hooksConfigPath, StandardCharsets.UTF_8));

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length == 2) {
                    map.put(values[0], values[1]);
                }
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }
}
