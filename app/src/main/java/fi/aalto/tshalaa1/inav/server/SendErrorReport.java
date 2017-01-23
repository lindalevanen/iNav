package fi.aalto.tshalaa1.inav.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

import fi.aalto.tshalaa1.inav.entities.Landmark;
import fi.aalto.tshalaa1.inav.entities.Point3D;
import fi.aalto.tshalaa1.inav.entities.ServerErrorResponse;
import fi.aalto.tshalaa1.inav.entities.SimplePoint;

/**
 * Created by noreikm1 on 4/15/14.
 */

public class SendErrorReport extends SpringRequest<String> {

    private int landmarkID;
    private String name;
    private String description;
    private Point3D location;
    private byte[] imageBytes;

    public SendErrorReport(Landmark landmark, Point3D newLocation) {
        landmarkID = landmark.getID();
        name = landmark.getName();
        description = landmark.getDescription();
        location = newLocation;
        imageBytes = landmark.getImageData();
    }

    @Override
    public String call() throws Exception {

        final String url = getUrl("/landmarks/"+landmarkID+"/reports");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();

        map.add("landmarkID", Integer.toString(landmarkID));

        if(name != null) { map.add("name", name); }
        if(description != null) { map.add("description", description); }
        if(imageBytes != null) {
            map.add("imageData", new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "photo";
                }
            });
        }

        if(location != null) {
            SimplePoint point = new SimplePoint(location.getX(), location.getY(),location.getZ());
            String pointStr = new ObjectMapper().writeValueAsString(point);
            map.add("position", pointStr);
        }

        String langCode = Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
        map.add("languageCode", langCode);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
        ResponseEntity<ServerErrorResponse> response =
                restTemplate.exchange(url, HttpMethod.POST, request, ServerErrorResponse.class);

        System.out.println("response id: "+response.getBody().getID());

        if(response.getBody().getID() == (long)landmarkID) return "success";
        return "failure";
    }
}
