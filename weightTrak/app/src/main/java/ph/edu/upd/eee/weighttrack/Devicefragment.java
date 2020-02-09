package ph.edu.upd.eee.weighttrack;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
/**
 * Created by raymundrafael on 30/04/2018.
 */

public class Devicefragment extends Fragment {

    Switch switchButton, switchButton2;
    TextView textView, textView2;
    String switchOn = "Power Saving Mode is ON";
    String switchOff = "Power Saving Mode is OFF";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.device,container,false);
        switchButton = (Switch) v.findViewById(R.id.switchButton);
        textView = (TextView) v.findViewById(R.id.textView);

        SharedPreferences sharedPrefs = getActivity().getSharedPreferences("com.example.raymundrafael.weighttrak",Context.MODE_PRIVATE);
        switchButton.setChecked(sharedPrefs.getBoolean("switch1State",true));
        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean bChecked) {
                if (bChecked) {
                    SharedPreferences.Editor editor = getActivity().getSharedPreferences("com.example.raymundrafael.weighttrak", Context.MODE_PRIVATE).edit();
                    editor.putBoolean("switch1State",true);
                    editor.commit();
                    textView.setText(switchOn);
                } else {
                    SharedPreferences.Editor editor = getActivity().getSharedPreferences("com.example.raymundrafael.weighttrak", Context.MODE_PRIVATE).edit();
                    editor.putBoolean("switch1State",false);
                    editor.commit();
                    textView.setText(switchOff);
                }
            }
        });

        if (switchButton.isChecked()) {
            textView.setText(switchOn);
        } else {
            textView.setText(switchOff);
        }

        return v;
    }

}
