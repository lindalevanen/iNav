package fi.aalto.tshalaa1.inav.server;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.gson.Gson;

import org.json.JSONObject;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import fi.aalto.tshalaa1.inav.entities.Area;
import fi.aalto.tshalaa1.inav.entities.PathPoint;
import fi.aalto.tshalaa1.inav.entities.Point3D;
import fi.aalto.tshalaa1.inav.entities.Route;
import fi.aalto.tshalaa1.inav.entities.SimplePoint;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RouteRequest extends SpringRequest<Route>{

    private Route route = null;
    private Point3D start, destination;
    private int buildingID, startAreaID, endAreaID;

    public RouteRequest(Point3D start, Point3D destination, int startAreaID, int endAreaID, int buildingID) {
        this.start = start;
        this.destination = destination;
        this.startAreaID = startAreaID;
        this.endAreaID = endAreaID;
        this.buildingID = buildingID;
    }

    public class RRequest {
        @JsonProperty
        int startAreaID;
        @JsonProperty
        int destinationAreaID;
        @JsonProperty
        SimplePoint startPosition;
        @JsonProperty
        SimplePoint destinationPosition;
        @JsonProperty
        int buildingID;
    }

    @Override
    public Route call() throws Exception {

        final String url = getUrl("/route");
        //final String url = "http://130.233.97.187:5004/route";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        RRequest rr = new RRequest();
        rr.startAreaID = this.startAreaID;
        rr.destinationAreaID = this.endAreaID;
        rr.buildingID = this.buildingID;
        rr.destinationPosition = destination.parameters();
        rr.startPosition = start.parameters();

        HttpEntity<RRequest> request = new HttpEntity<>(rr, headers);


        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

        //System.out.println("what is in the request " + request.getBody().startPosition.x);
        Route route = restTemplate.postForObject(url, request, Route.class);
        System.out.println("route instructions: " + route.getInstructions());

        System.out.println("ROUTE found " + route);
        List<HashMap> path = new ArrayList<HashMap>();
        List<Point3D> p = new ArrayList<Point3D>();
        List<Integer> areaIds = new ArrayList<Integer>();
        System.out.println("waypoints " + route.waypoints);
        for(HashMap o : route.waypoints) {
            path.add((HashMap) o.get("point_position"));
            areaIds.add((int) o.get("areaID"));
        }
        int j = 0;
        for(HashMap i : path) {
            Point3D point = new Point3D((Double) i.get("x"),(Double) i.get("y"), (Double) i.get("z"));
            point.areaID = areaIds.get(j);
            p.add(point);
            j++;
        }
        route.setPath(p);
        return route;
    }
}
