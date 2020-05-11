package com.dji.GSDemo.GaodeMap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private View.OnClickListener clickListener = v -> {
        switch (v.getId()) {
            case R.id.btn_waypoint1:
                startActivity(MainActivity.this, Waypoint1Activity.class);
                break;
            case R.id.btn_waypoint2:
                startActivity(MainActivity.this, Waypoint2Activity.class);
                break;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_waypoint1).setOnClickListener(clickListener);
        findViewById(R.id.btn_waypoint2).setOnClickListener(clickListener);
    }

    public static void startActivity(Context context, Class activity) {
        Intent intent = new Intent(context, activity);
        context.startActivity(intent);
    }
}
