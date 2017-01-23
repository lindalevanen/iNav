package fi.aalto.tshalaa1.inav.server;

import android.util.Log;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import fi.aalto.tshalaa1.inav.entities.Building;

/**
 * Created by tshalaa1 on 7/22/16.
 */
public class BuildingsRequest  extends SpringRequest<Building[]> {

    private String TAG = "BuildingsRequest";

    public BuildingsRequest() {
        Log.d(TAG, "Inside BuildingsRequest");
    }

    @Override
    public Building[] call() throws Exception {

        final String url = getUrl("/buildings");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());


        Building[] response = restTemplate.getForObject(url, Building[].class, request);
        Log.d(TAG, "response as linkedhash..." + Arrays.toString(response));

       // Building b = new Building();

        Log.d(TAG, "Request is made and returned");
        return response;
    }
}
