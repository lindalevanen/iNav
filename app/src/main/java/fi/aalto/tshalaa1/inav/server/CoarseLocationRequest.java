package fi.aalto.tshalaa1.inav.server;

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
import java.util.LinkedHashMap;

import fi.aalto.tshalaa1.inav.entities.Point3D;

/**
 * This class was supposed to be used in StartingActivity when finding the closest building to user,
 * but found a better way to do this instead. This class can be used in finding the coarse location
 * of the user inside the building instead.
 */
public class CoarseLocationRequest extends SpringRequest<Point3D> {

    private String sensors = null;

    public CoarseLocationRequest(String sensorsInfo) {
        sensors = sensorsInfo;
    }

    @Override
    public Point3D call() throws Exception {

        final String url = getUrl("/location/coarse");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        // headers.setContentType(MediaType.APPLICATION_JSON);

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("sensors", sensors);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map, headers);
        System.out.println("Location request body: " + request.getBody());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
//        restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
//        System.out.println("what is the request " + request.toString() + " what are the headers " + request.getHeaders() + " request body " + request.getBody());
        ResponseEntity<LinkedHashMap> response = restTemplate.exchange(url, HttpMethod.POST, request, LinkedHashMap.class);
        Point3D responsePoint = restTemplate.postForObject(url, request, Point3D.class);
        System.out.println("the converted reponse point" + response);

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
