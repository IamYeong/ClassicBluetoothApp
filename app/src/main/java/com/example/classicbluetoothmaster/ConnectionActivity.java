package com.example.classicbluetoothmaster;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ConnectionActivity extends AppCompatActivity implements OnLogAddedListener {

    private Handler handler;
    private Thread chartThread;
    private ConnectThread connectThread;
    //private AcceptThread acceptThread;

    private TextView tv_thermo, tv_humidity, tv_pressure, tv_rotate, tv_log, tv_connecting;
    private FrameLayout frameLayout;
    private Button btn_connect;

    private ImageView img_signal;
    private Button btn_fvc;
    //private InputStream inputStream;
    private boolean isChartInit = false;
    private boolean isStart = false;

    private LineChart lineChart;
    private LineDataSet lineDataSet;
    private LineData lineData;
    private List<Entry> entries;

    private String thermometer, humidity, pressure, rotate;
    private int count = 0;

    private Intent intent;
    private BluetoothDevice device;

    private RxTxThread txRxThread;
    private TRData trData;

    private Paint paint;
    private Path path;

    private Handler handlerCallback = new Handler(Looper.myLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {

            switch(msg.what) {

                case RxTxThread.MESSAGE_DATA :

                    trData = (TRData) msg.obj;

                    //tv_log.append("\n" + trData.getThermometer() + trData.getHumidity() + trData.getPressure() + trData.getRotate());


                    tv_thermo.setText(trData.getThermometer().toString());
                    tv_humidity.setText(trData.getHumidity().toString());
                    tv_pressure.setText(trData.getPressure().toString());
                    tv_rotate.setText(trData.getRotate().toString());

                    if (isChartInit) {
                        addEntry(trData.getRotate().toString(), count);
                    }


                    break;

                case RxTxThread.MESSAGE_WRITE :

                    tv_log.append("\nWrite Success! : " + msg.arg1);

                    break;

                case RxTxThread.MESSAGE_START:
                    isStart = true;
                    img_signal.setBackgroundColor(Color.GREEN);
                    tv_log.append("\n" + "onStartRead_S");
                    break;


                case RxTxThread.MESSAGE_END:
                    txRxThread.stopReadThread();
                    chartThread.interrupt();
                    connectThread.cancel();

                    img_signal.setBackgroundColor(Color.RED);

                    btn_connect.setVisibility(View.VISIBLE);
                    break;


                case RxTxThread.MESSAGE_OTHER :

                    tv_log.append("\n" + msg.arg1);

                    break;

            }

            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        //?????? ????????? ?????????
        tv_thermo = findViewById(R.id.tv_thermometer_fvc);
        tv_humidity = findViewById(R.id.tv_humidity_fvc);
        tv_pressure = findViewById(R.id.tv_pressure_fvc);
        tv_rotate = findViewById(R.id.tv_rotate_fvc);
        tv_connecting = findViewById(R.id.tv_connecting);

        //?????? ?????????
        tv_log = findViewById(R.id.tv_log_connection);
        tv_log.setText("log : ");

        frameLayout = findViewById(R.id.frame_connection);

        //?????? ??????
        btn_connect = findViewById(R.id.btn_connection);
        btn_connect.setVisibility(View.INVISIBLE);

        //??????, ?????? ?????????
        img_signal = findViewById(R.id.img_sgnal_connection);
        lineChart = findViewById(R.id.line_chart_fvc);
        btn_fvc = findViewById(R.id.btn_fvc);

        //???????????? ????????????
        intent = getIntent();
        device = intent.getParcelableExtra("DEVICE");

        //acceptSocket();
        connectSocket();
        createChart();
        tv_log.append("\n createChart(), connectSocket()");

        btn_fvc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txRxThread.writeStart();
            }
        });

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //acceptSocket();
                connectSocket();
            }
        });

    }//onCreate

    private void connectSocket() {

        btn_connect.setVisibility(View.INVISIBLE);
        img_signal.setBackgroundColor(Color.WHITE);
        tv_log.setVisibility(View.VISIBLE);
        //????????????????????? ????????? ????????? ????????? ???????????? ?????? ???????????? ??????.
        handler = new Handler();

        connectThread = new ConnectThread(device, this, handler);
        connectThread.start();

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void createChart() {

        //final Handler handler = new Handler();
        chartThread = new Thread() {
            @Override
            public void run() {

                initChart();

            }
        };

        chartThread.start();

    }

    private void addEntry(String y, int x) {

        if (y != null && !y.equals("")) {

            float yValue = (float) Integer.parseInt(y);
            float xValue = (float) x;

            entries.add(new Entry(xValue, yValue));

        /*
        if (entries.size() > 500) {
            entries.remove(0);
        }

         */

            lineDataSet.notifyDataSetChanged();
            lineData.notifyDataChanged();
            lineChart.notifyDataSetChanged();

            lineChart.invalidate();

            count++;
        }

    }

    private void initViewChart(float initX, float initY) {
        path = new Path();
        paint = new Paint();

        path.moveTo(initX, initY);


    }

    private void addPath(String value, int count) {

        float x = (float) Integer.parseInt(value);
        float y = (float) count;


        //1. ????????? ??????
        //2. ????????? ??????
        //3. ????????? ??? ??????
        //4. ????????????

        path.lineTo(x, y);
        LineView line = new LineView(ConnectionActivity.this, paint, path);

        frameLayout.removeAllViews();
        frameLayout.addView(line);

    }

    private void initChart() {

        //????????? ?????? ?????? ????????? ???
        XAxis xAxis = lineChart.getXAxis();

        entries = new ArrayList<>();
        lineDataSet = new LineDataSet(entries, "??????");
        lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);

        YAxis yRight = lineChart.getAxisRight();
        YAxis yLeft = lineChart.getAxisLeft();

        xAxis.setEnabled(false);
        yLeft.setEnabled(false);
        yRight.setEnabled(false);


        /*
        xAxis.setAxisMaximum(1000f);
        yRight.setAxisMaximum(1000f);
        yLeft.setAxisMaximum(1000f);

         */

        //xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        lineChart.setDragEnabled(true);
        lineChart.setAutoScaleMinMaxEnabled(true);

        lineChart.getDescription().setEnabled(false);
        //lineChart.setVisibleXRangeMaximum(5);
        //lineChart.setDrawMarkers(false);
        //lineChart.setDrawGridBackground(false);
        lineChart.getLegend().setEnabled(false);
        //lineChart.setDrawMarkers(false);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);
        lineDataSet.setColor(Color.RED);
        lineDataSet.setLineWidth(1f);

        isChartInit = true;


    }


    @Override
    public void onLogAdded(String log) {

        if (log.equals(ConnectThread.SOCKET_CONNECT_SUCCESS)) {

            tv_log.append("\n" + log);

            tv_connecting.setVisibility(View.INVISIBLE);
            BluetoothSocket socket = connectThread.getSocket();
            tv_log.append("\n" + socket.toString());

            //handler = new Handler();
            txRxThread = new RxTxThread(handlerCallback, this, socket);
            txRxThread.readStart();

            //connectThread.cancel();
            
            //?????????????????? ????????? ?????? ??? ?????????
            //thread.writeStart();

        }

        if (log.equals(ConnectThread.SOCKET_CREATE_FAIL)) {
            //?????? ???????????? ??????.\
            tv_log.append("\n" + log);
            btn_connect.setVisibility(View.VISIBLE);
            img_signal.setBackgroundColor(Color.RED);
            tv_connecting.setVisibility(View.INVISIBLE);

        } else if (log.equals(ConnectThread.SOCKET_CONNECT_FAIL)) {
            tv_log.append("\n" + log);
            btn_connect.setVisibility(View.VISIBLE);
            img_signal.setBackgroundColor(Color.RED);
            tv_connecting.setVisibility(View.INVISIBLE);

        } else if (log.equals(ConnectThread.SOCKET_RETURN_FAIL)) {
            tv_log.append("\n" + log);
            btn_connect.setVisibility(View.VISIBLE);
            img_signal.setBackgroundColor(Color.RED);
            tv_connecting.setVisibility(View.INVISIBLE);

        } else if (log.equals(ConnectThread.SOCKET_CLOSE_FAIL)) {
            tv_log.append("\n" + log);
            btn_connect.setVisibility(View.VISIBLE);
            img_signal.setBackgroundColor(Color.RED);
            tv_connecting.setVisibility(View.INVISIBLE);

        } else {
            tv_log.append("\n" + log);
        }

        //???????????? ?????? ???????????? ?????? ??????


    }
}