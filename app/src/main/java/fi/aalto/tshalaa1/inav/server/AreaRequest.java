package fi.aalto.tshalaa1.inav.server;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import fi.aalto.tshalaa1.inav.entities.Area;
import fi.aalto.tshalaa1.inav.entities.Building;
import fi.aalto.tshalaa1.inav.entities.Landmark;

/**
 * Created by tshalaa1 on 7/22/16.
 */
public class AreaRequest extends SpringRequest<Area[]>  {

    private String TAG = "AreaRequest";
    int buildingID;

    public AreaRequest(int buildingID) {
        this.buildingID = buildingID;
        Log.d(TAG, "Inside AreaRequest");
    }

    @Override
    public Area[] call() throws Exception {

        final String url = getUrl("/buildings/" + buildingID + "/areas");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());

        //In the future we may get more than one area in the buildings request
        Area[] response = restTemplate.getForObject(url, Area[].class, request);

        Log.d(TAG, "Request is made and returned");
        return response;
    }
}
