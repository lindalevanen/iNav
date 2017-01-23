package fi.aalto.tshalaa1.inav.server;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by noreikm1 on 4/15/14.
 */
public class ImageRequest extends SpringRequest<Bitmap> {

    private final String id;
    Context c;

    public ImageRequest(Context c, String imageId) {
        id = imageId;
        this.c = c;
    }

    @Override
    public Bitmap call() throws Exception {

        final String url = getUrl("/image");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("id", id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);


        RestTemplate restTemplate = new RestTemplate();
        //restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
        System.out.println(request.toString());
        byte[] imageBytes = restTemplate.postForObject(url, request, byte[].class);
        if (imageBytes == null) {
            return null;
        }
        Bitmap bmp = BitmapFactory.decodeStream(new ByteArrayInputStream(imageBytes));
//        Bitmap b = Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth() / 1.5f), (int)(bmp.getHeight() / 1.5f), true);
//        bmp.recycle();
        return bmp;
    }
}
