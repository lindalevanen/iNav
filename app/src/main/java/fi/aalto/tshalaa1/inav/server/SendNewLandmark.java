package fi.aalto.tshalaa1.inav.server;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Locale;

import fi.aalto.tshalaa1.inav.entities.Landmark;
import fi.aalto.tshalaa1.inav.entities.SimplePoint;

/**
 * Created by Linda on 26/08/16.
 */
public class SendNewLandmark extends SpringRequest<String> {

    private Landmark landmark;
    private String type;

    public SendNewLandmark(Landmark lm, String type) {
        this.landmark = lm;
        this.type = type;
    }

    @Override
    public String call() throws Exception {

        final String url = getUrl("/landmarks");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();

        map.add("buildingID", Integer.toString(landmark.buildingID));
        map.add("areaID", Integer.toString(landmark.areaID));

        map.add("name", landmark.name);
        if(!landmark.description.isEmpty()) map.add("description", landmark.description);

        map.add("imageData", new ByteArrayResource(landmark.getImageData()) {
            @Override
            public String getFilename() {
                return "photo";
            }
        });

        map.add("type", type);

        SimplePoint point = new SimplePoint(landmark.getLocation().getX(), landmark.getLocation().getY(),landmark.getLocation().getZ());
        String pointStr = new ObjectMapper().writeValueAsString(point);
        map.add("position", pointStr);

        String langCode = Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
        map.add("languageCode", langCode);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        System.out.println("response: "+response.getBody());

        if(response.getBody().equals("true")) return "success";
        return "failure";
    }
}
