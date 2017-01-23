package fi.aalto.tshalaa1.inav.server;

import org.json.JSONObject;
import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import fi.aalto.tshalaa1.inav.entities.Point3D;

/**
 * Created by noreikm1 on 4/15/14.
 */

public class LocationRequest extends SpringRequest<Point3D> {

    private byte[] imageBytes;
    private String sensors = null;
    private int buildingID;

    public LocationRequest(byte[] imgBytes, String sensorsInfo, int buildingID) {
        imageBytes = imgBytes;
        sensors = sensorsInfo;
        this.buildingID = buildingID;

    }

    @Override
    public Point3D call() throws Exception {

        final String url = getUrl("/location/fine");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("sensors", sensors);

        map.add("image", new ByteArrayResource(imageBytes){
            @Override
            public String getFilename() {
                return "photo";
            }
        });

        map.add("buildingID", Integer.toString(buildingID));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
        ResponseEntity<LinkedHashMap> response = restTemplate.exchange(url, HttpMethod.POST, request, LinkedHashMap.class);

        LinkedHashMap pos = (LinkedHashMap) response.getBody().get("position");
        LinkedHashMap rot = (LinkedHashMap) response.getBody().get("direction");
        Point3D p = new Point3D((Double) pos.get("x"), (Double)pos.get("y"),(Double) pos.get("z"));
        p.areaID = (Integer) response.getBody().get("areaID");
        p.setRotX((Double) rot.get("x"));
        p.setRotY((Double) rot.get("y"));
        p.setRotZ((Double) rot.get("z"));
        p.timestamp = (long) response.getBody().get("timestamp");

        //System.out.println("what is the body: " + response.getBody().get("position") + " Point3D " +  p);

        return p;
    }
}
