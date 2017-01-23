package fi.aalto.tshalaa1.inav.server;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.support.Base64;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;

import fi.aalto.tshalaa1.inav.entities.Building;
import fi.aalto.tshalaa1.inav.entities.Landmark;
import fi.aalto.tshalaa1.inav.entities.Point3D;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

/**
 * Created by kreutze1 on 8/20/14.
 */
public class LandmarkRequest extends SpringRequest<List<Landmark>> {

    public String keywords;
    public int buildingID;
    public LandmarkRequest(String keys, int buildID) {
        this.buildingID = buildID;
        keywords = keys;
    }

    private static class Response {
        List<Landmark> landmarks;
    }

    OkHttpClient client = new OkHttpClient();

   public String run(String url) throws IOException {
        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .addHeader("Accept-Language", Locale.getDefault().getLanguage())
                .url(url)
                .build();

        okhttp3.Response response = client.newCall(httpRequest).execute();
        return response.body().string();
    }

    @Override
    public List<Landmark> call() throws Exception {

        final String url;
        if(keywords.length() == 0) {
            url = getUrl("/buildings/"+buildingID + "/landmarks");
        } else {
            url = getUrl("/buildings/"+buildingID + "/landmarks?filter="+keywords);
        }
        //final String url = "http://130.233.193.222:8004/areas/"+areaID + "/landmarks?filter="+keywords;
        String result = run(url);
//        System.out.println("The response from the HTTPClient req " + result);

        String jsonBuild = result;
        Gson gson = new Gson();
        Type type = new TypeToken< Landmark[]>() {}.getType();
        Landmark[] lmList = gson.fromJson(jsonBuild, type);
        List<Landmark> response = Arrays.asList(lmList);
        System.out.println("landmark response: "+response);
        if(lmList.length == 0) {
            response = null;
        }

        return response;
    }



}
