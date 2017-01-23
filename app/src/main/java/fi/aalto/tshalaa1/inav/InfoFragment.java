package fi.aalto.tshalaa1.inav;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import fi.aalto.tshalaa1.inav.entities.Landmark;

/**
 * InfoFragment contains information of the landmark the user has pressed on the minimap.
 */
public class InfoFragment extends Fragment {

    private static final String ARGUMENT_TITLE = "title";
    private static final String ARGUMENT_WHOLE_DESCRIPTION = "wholeDescription";
    private static final String ARGUMENT_FAVE = "fave";
    private static final String ARGUMENT_ID = "id";

    OnInfoButtonPressed mCallback;
    static Landmark mLandmark;

    // Container Activity must implement this interface (MainActivity)
    public interface OnInfoButtonPressed {
        public void onButtonPressed(View button, Landmark landmark);
        public void onInfoPressed();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnInfoButtonPressed) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnInfoButtonPressed");
        }
    }

    public static InfoFragment newInstance(Landmark landmark, String wholeDescription, Boolean fave) {
        final Bundle args = new Bundle();

        args.putString(ARGUMENT_TITLE, landmark.getNicerTitle());
        args.putString(ARGUMENT_WHOLE_DESCRIPTION, wholeDescription);
        args.putBoolean(ARGUMENT_FAVE, fave);
        args.putInt(ARGUMENT_ID, landmark.getID());

        mLandmark = landmark;

        final InfoFragment fragment = new InfoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public InfoFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_markerinfo, container, false);

        final TextView titleTextView = (TextView) view.findViewById(R.id.title);
        final TextView descriptionTextView = (TextView) view.findViewById(R.id.description);
        final ImageButton favButton = (ImageButton) view.findViewById(R.id.button_favorite);

        final Bundle args = getArguments();

        titleTextView.setText(args.getString(ARGUMENT_TITLE));
        descriptionTextView.setText(args.getString(ARGUMENT_WHOLE_DESCRIPTION));

        Boolean on = args.getBoolean(ARGUMENT_FAVE);
        setHeartColor(favButton, on);

        initListeners(view);
        return view;
    }

    /**
     * Sets the correct color to the favorite button according to whether it's on favorite or not.
     * @param favButton the favorite ImageButton
     * @param on true -> is favorite (red heart), false -> not favorite (gray heart)
     */

    public void setHeartColor(ImageButton favButton, boolean on) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(on) favButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_heartred,
                    ((MainActivity) getContext()).getTheme()));
            else favButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_heart3,
                    ((MainActivity) getContext()).getTheme()));
        } else {
            if(on) favButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_heartred));
            else favButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_heart3));
        }
    }

    /**
     * Initializes all the listeners on the fragment.
     * @param view the main view of the fragment
     */

    public void initListeners(View view) {
        final ImageButton navigateButton = (ImageButton) view.findViewById(R.id.button_navigate);
        final ImageButton favoriteButton = (ImageButton) view.findViewById(R.id.button_favorite);
        final RelativeLayout errorButton = (RelativeLayout) view.findViewById(R.id.errorLO);
        final RelativeLayout editButton = (RelativeLayout) view.findViewById(R.id.editLO);
        final RelativeLayout lo = (RelativeLayout) view.findViewById(R.id.markerInfo);

        setButtonListener(navigateButton);
        setButtonListener(favoriteButton);
        setButtonListener(errorButton);
        setButtonListener(editButton);

        lo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onInfoPressed();
            }
        });
    }

    /**
     * Sets a button listener to a single button, that on pressed calls a method on the fragment's interface.
     * @param button the button to set the listener to
     */

    public void setButtonListener(View button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onButtonPressed(v, mLandmark);
            }
        });

    }
}
