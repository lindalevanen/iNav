package fi.aalto.tshalaa1.inav;


import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.ListPopupWindowCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListPopupWindow;
import android.widget.Toast;

/**
 * Created by tshalaa1 on 8/2/16.
 */
public class ListPopupWindowActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener{
    public ListPopupWindow listPopupWindow = new ListPopupWindow(this);

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.starting_listview);

        final View imgButton = (View) new ImageButton(this);//findViewById(R.id.button);
        imgButton.setBackgroundResource(R.drawable.ic_markerred);
        imgButton.setOnClickListener(this);

        final List<String> data = Arrays.asList("Navigate Here", "Save this landmark", "Piyo", "Fuga");
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data);
        listPopupWindow = new ListPopupWindow(this);
        listPopupWindow.setAdapter(adapter);
        listPopupWindow.setOnItemClickListener(this);
        listPopupWindow.setAnchorView(imgButton);

        // KitKat未満では、createDragToOpenListenerはnullを返します。
        imgButton.setOnTouchListener(ListPopupWindowCompat.createDragToOpenListener(listPopupWindow, imgButton)); //can be removed later
    }

    @Override
    public void onClick(final View v) {
        listPopupWindow.show(); // 普通にクリックされたらポップアップ表示
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        Toast.makeText(this, (String) parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show();
        listPopupWindow.dismiss();
    }
}
