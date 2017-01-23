package fi.aalto.tshalaa1.inav.server;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.util.Log;
import com.larvalabs.svgandroid.*;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import fi.aalto.tshalaa1.inav.entities.Bounds;
import fi.aalto.tshalaa1.inav.entities.Minimap;

/**
 * Created by tshalaa1 on 6/17/16.
 */
public class MinimapRequest extends SpringRequest<Minimap> {

    private String TAG = "MinimapRequest";
    private String areaID;

    public MinimapRequest(String areaID) {
        this.areaID = areaID;
        Log.d(TAG, "Inside MinimapRequest");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Minimap call() throws Exception {

        final String url = getUrl("/areas/"+areaID+"/map");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));

        headers.set("Content-Type", "image/png;q=0.8,image/svg+xml");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        RestTemplate restTemplate = new RestTemplate();

        restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());

        //Byte array below can be set as parameter for minimap setImageData method (url gets map in PNG form)
        byte[] bytes = restTemplate.getForObject(url ,byte[].class, request);

        //test svg file "http://upload.wikimedia.org/wikipedia/commons/e/e8/Svg_example3.svg"
        //byte[] imgBytes = restTemplate.getForObject(url, byte[].class);
        //System.out.println("should be an svg byte array? " + Arrays.toString(response.getBody()));
        //Bitmap imgBmp = BitmapFactory.decodeStream(new ByteArrayInputStream(imgBytes));

        Minimap m = new Minimap();
        /*
        ///The code below can request and parse simple SVG files and set is as a Drawable to the minimap.
        try {

            //https://upload.wikimedia.org/wikipedia/commons/0/02/SVG_logo.svg -> more complex svg file
            final URL urlSVG = new URL("https://upload.wikimedia.org/wikipedia/commons/0/02/SVG_logo.svg");
            HttpURLConnection urlConnection = (HttpURLConnection) urlSVG.openConnection();
            InputStream inputStream = urlConnection.getInputStream();
            SVG svg = SVGParser. getSVGFromInputStream(inputStream);
            Drawable drawable = svg.createPictureDrawable();
            m.setDrawable(drawable);
        } catch (Exception e) {
            Log.e("Getting SVG failed", e.getMessage(), e);
        }
        ///
        */

        m.setImageData(bytes);

        Log.d(TAG, "Request is made and returned");
        return m;
    }
}
