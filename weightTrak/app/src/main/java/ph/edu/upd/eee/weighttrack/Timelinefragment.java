package ph.edu.upd.eee.weighttrack;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class Timelinefragment extends Fragment {

    GraphView graphView;
    LineGraphSeries<DataPoint> series;
    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd\nh a");

    private FirebaseAuth fireAuth;
    private DatabaseHelper myDb;
    private DatabaseReference userEntriesRef;
    String weight_string;
    TextView textview;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.timeline,container,false);
        fireAuth = FirebaseAuth.getInstance();
        myDb = new DatabaseHelper(getContext());

        userEntriesRef = FirebaseDatabase.getInstance().getReference().child("user-entries").child(fireAuth.getCurrentUser().getUid());
        sync();

        graphView = v.findViewById(R.id.graph);
        graphView.setTitle("Weight Timeline");
        graphView.setTitleTextSize(70);
        textview = v.findViewById(R.id.textview);
        series = new LineGraphSeries<DataPoint>();
        getDataPoint();
        graphView.addSeries(series);

        graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if( isValueX ){
                    return sdf.format(new Date((long) value));
                } else {
                    return super.formatLabel(value, isValueX);
                }
            }
        });
        graphView.getGridLabelRenderer().setHumanRounding(false);
//        graphView.getGridLabelRenderer().setNumHorizontalLabels(3); // only 4 because of the space

//        graphView.getViewport().setMinX(d1.getTime());
//        graphView.getViewport().setMaxX(d3.getTime());
//        graphView.getViewport().setXAxisBoundsManual(true);

        graphView.getViewport().setScalable(true); // enables horizontal scrolling

        textview.setText("Current Weight: "+weight_string+" kg");

        return v;
    }

    private void sync() {
        ValueEventListener entriesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("TimelineFragment","onDataChange" );
                String datetime, weight;
                for (DataSnapshot datetimeSnapshot: dataSnapshot.getChildren()) {
                    datetime = datetimeSnapshot.getKey();
                    weight = (String) datetimeSnapshot.getValue();
                    Log.d("TimelineFragment","onDataChange:"+datetime+","+weight );
                    boolean isInserted = myDb.insertData( datetime, weight );
                    Log.d("TimelineFragment","onDataChange:isInserted:"+isInserted);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("TimelineFragment", "onCancelled", databaseError.toException());
            }
        };
        userEntriesRef.addValueEventListener(entriesListener);
    }

    @SuppressLint("SimpleDateFormat")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void getDataPoint() {
        Date x = null;
        double y;
        Cursor res = myDb.getAllData();
        if(res.getCount()==0){
            Log.d("TimelineFragment","Cursor: nothing found");
            return;
        }
        while(res.moveToNext()){
            try {
                x = new SimpleDateFormat("E MMM dd hh:mm:ss z yyyy").parse(res.getString(0));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            y = Double.parseDouble(res.getString(1));
            Log.d("TimelineFragment","Cursor:"+x+","+y);
            series.appendData(new DataPoint(x, y), true, 100);
            weight_string = res.getString(1).toString();
        }

    }

}
