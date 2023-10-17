package com.example.onlinerssi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.onlinerssi.models.FingerPrint;
import com.example.onlinerssi.models.NearFP;
import com.example.onlinerssi.models.Point;
import com.example.onlinerssi.models.Router;
import com.example.onlinerssi.models.SamplePoint;
import com.example.onlinerssi.models.SampleRouter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;

    private ArrayList<Router> routers = new ArrayList<>();
    private ArrayList<SampleRouter> srs = new ArrayList<>();

    private ArrayList<Point> points = new ArrayList<>();
    private ArrayList<FingerPrint> fingerPrints = new ArrayList<>();
    private ArrayList<SamplePoint> spsTest = new ArrayList<>();

    private SamplePoint sp = null;


    private TextView dataEdt;
    private Button posBtn;
    private ImageView mapImgView;
    private EditText kInKnnEdt;

    private String fileName;

    private int kInKNN = 3;

    private float scaleY = 650 / 65;
    private float scaleX = 150 / 11;

    private boolean reScan = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check permissions we need
        checkPermissions();

        dataEdt = findViewById(R.id.data);
        posBtn = findViewById(R.id.getPositionBtn);
        kInKnnEdt = findViewById(R.id.kEdt);
        kInKnnEdt.setText("" + kInKNN);

        mapImgView = findViewById(R.id.mapImg);
        mapImgView.setBackgroundResource(R.drawable.planv);

        // Load Finger Prints
        for (int i = 1; i < 6; i++) {
            fileName = "fp" + i + ".json";
            getFingerPrints(fileName);
        }

//        showFingerPrint();

        // TEST
////         Read test points from json files
//        getTestJson("test3pnt.json");
//        for (int i = 0; i < spsTest.size(); i++) {
//            FingerPrint sp = findPoint(spsTest.get(i));
////            sp.setX(-0.5);
////            sp.setY(18.0);
//
//            float xSp = (float) Math.round((spsTest.get(i).getX() + 0.5) * 2);
//            float ySp = (float) Math.round((spsTest.get(i).getY() + 0.5) * 2);
//            float xFp = (float) Math.round((sp.getX() + 0.5) * 2);
//            float yFp = (float) Math.round((sp.getY() + 0.5) * 2);
//            drawPoint((xSp * scaleX) + 25, 650 - (ySp * scaleY),
//                    (xFp * scaleX) + 25, 650 - (yFp * scaleY), 10);
//
////            spsTest.remove(i);
////            spsTest.add(i, sp);
//        }

//        showFoundedPoint();

        // WIFI
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Wifi is disabled. You need to enable it", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }

//        registerReceiver(wifiReciever, new IntentFilter(wifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // get position button
        posBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    kInKNN = Integer.parseInt(kInKnnEdt.getText().toString());
                    startScan();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Check K input", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    // just for debug
    private void showFingerPrint() {
        String dataTxt = "";
        for (FingerPrint fp : fingerPrints) {
            dataTxt += fp.getNumber() + " - " + fp.getX() + " - " + fp.getY() + " - ";
            for (Point p : fp.getPoints()) {
//                for (Router r : p.getWifiList()
//                ) {
//                    dataTxt += r.getMeanRSSI() + " - ";
//                }
                dataTxt += p.getWifiList().size() + " - ";

            }
            dataTxt += "\n\n\n";
        }
        dataEdt.setText(dataTxt);
    }

    // just for debug
    private void showFoundedPoint() {
        String dataTxt = "";
        for (SamplePoint sp : spsTest) {
            dataTxt += sp.getX() + " - " + sp.getY() + " - ";
            for (SampleRouter sr : sp.getWifiList()
            ) {
                dataTxt += "[ " + sr.getSSID() + " : " + sr.getRSSI() + "] - ";
            }
            dataTxt += "\n\n\n";
        }
        dataEdt.setText(dataTxt);
    }

    // read json string and create finger prints list
    private void getFingerPrints(String fileName) {

        try {
            JSONObject obj = new JSONObject(loadJsonFromAssets(fileName));
            JSONArray fingerPrintsJA = obj.getJSONArray("finger_prints");
//            ArrayList<HashMap<String, String>> formList = new ArrayList<HashMap<String, String>>();
//            HashMap<String, String> m_li;

            for (int i = 0; i < fingerPrintsJA.length(); i++) {
                JSONObject fpObject = fingerPrintsJA.getJSONObject(i);
                Integer n = Integer.parseInt(fpObject.getString("n"));
                Double x = Double.parseDouble(fpObject.getString("x"));
                Double y = Double.parseDouble(fpObject.getString("y"));
                FingerPrint fp = new FingerPrint(n, x, y);

                JSONArray pointsJA = fpObject.getJSONArray("points");
                for (int j = 0; j < pointsJA.length(); j++) {
                    JSONObject pObject = pointsJA.getJSONObject(j);
                    String dir = pObject.getString("dir");
                    Point p = new Point(dir);

                    JSONArray routersJA = pObject.getJSONArray("routers");
                    for (int k = 0; k < routersJA.length(); k++) {
                        JSONObject rObject = routersJA.getJSONObject(k);
                        String SSID = rObject.getString("SSID");
                        String BSSID = rObject.getString("BSSID");
                        Double meanRSSI = Double.parseDouble(rObject.getString("meanRSSI"));
                        Router r = new Router(SSID, BSSID);
                        r.setMeanRSSI(meanRSSI);

                        JSONArray rssiJA = rObject.getJSONArray("RSSI");
                        for (int l = 0; l < rssiJA.length(); l++) {
                            r.addRSSI(rssiJA.getInt(l));
                        }

                        routers.add(r);
                    }

                    p.setWifiList(routers);
                    routers.clear();
                    points.add(p);
                }
                fp.setPoints(points);
                fingerPrints.add(fp);

                points.clear();

//                //Add your values in your `ArrayList` as below:
//                m_li = new HashMap<String, String>();
//                m_li.put("formule", formula_value);
//                m_li.put("url", url_value);
//
//                formList.add(m_li);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // for Test
    private void getTestJson(String fileName) {
        try {
            JSONObject obj = new JSONObject(loadJsonFromAssets(fileName));
            JSONArray fingerPrintsJA = obj.getJSONArray("finger_prints");

            for (int i = 0; i < fingerPrintsJA.length(); i++) {
                JSONObject fpObject = fingerPrintsJA.getJSONObject(i);

                JSONArray pointsJA = fpObject.getJSONArray("points");
                for (int j = 0; j < pointsJA.length(); j++) {
                    JSONObject pObject = pointsJA.getJSONObject(j);
                    SamplePoint sp = new SamplePoint();

                    srs.clear();
                    JSONArray routersJA = pObject.getJSONArray("routers");
                    for (int k = 0; k < routersJA.length(); k++) {
                        JSONObject rObject = routersJA.getJSONObject(k);
                        String SSID = rObject.getString("SSID");
                        String BSSID = rObject.getString("BSSID");
                        Integer meanRSSI = (int) Double.parseDouble(rObject.getString("meanRSSI"));
                        SampleRouter r = new SampleRouter(SSID, BSSID);
                        r.setRSSI(Integer.parseInt("" + meanRSSI));
                        srs.add(r);
                    }

                    sp.setWifiList(srs);
                    srs.clear();
                    spsTest.add(sp);
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // load json file as string
    private String loadJsonFromAssets(String fileName) {
        String jsonString;
        try {
            InputStream is = getApplicationContext().getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonString = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return jsonString;
    }

    // Scan wifi
    private void scanWifi() {

        dataEdt.setText("Scanning wifi...");
        dataEdt.setTextColor(Color.rgb(255, 0, 0));

        registerReceiver(wifiReciever, new IntentFilter(wifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
//        Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
    }

    // WIFI Reciever
    BroadcastReceiver wifiReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // If we have new signals
            if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) || !reScan) {


                SampleRouter r = null;
                sp = new SamplePoint();
                reScan = true;

                List<ScanResult> results = wifiManager.getScanResults();
                unregisterReceiver(this);


                for (ScanResult scanResult : results) {
                    r = new SampleRouter(scanResult.SSID, scanResult.BSSID);
                    r.setRSSI(scanResult.level * -1);
                    srs.add(r);
                }
                sp.setWifiList(srs);
                srs.clear();
                FingerPrint fpMin = findPoint(sp);
                float xFp = (float) Math.round((fpMin.getX() + 0.5) * 2);
                float yFp = (float) Math.round((fpMin.getY() + 0.5) * 2);
                float xSp = (float) Math.round((sp.getX() + 0.5) * 2);
                float ySp = (float) Math.round((sp.getY() + 0.5) * 2);
                String dataTxt = "Calculated point(Green) : \n" + "\tX=" + sp.getX() + ", Y=" + sp.getY();
                dataTxt += "\n\nNearest point(Red) : \n" + "\tX=" + fpMin.getX() + ", Y=" + fpMin.getY();

                dataEdt.setText(dataTxt);
                dataEdt.setTextColor(Color.rgb(0, 180, 0));

                drawPoint((xSp * scaleX) + 25, 650 - (ySp * scaleY),
                        (xFp * scaleX) + 25, 650 - (yFp * scaleY), 10);

//                scanWifi();
            } else if (reScan) {
                dataEdt.setText("Can't find new data!\nPlease wait and try again.\n");
                dataEdt.setTextColor(Color.rgb(255, 0, 0));
                unregisterReceiver(this);

                reScan = false;
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
//                        scanWifi();
                        dataEdt.setText("Try again...");
                        dataEdt.setTextColor(Color.rgb(255, 0, 0));
                    }
                }, 10000);
            }


        }
    };

    // draw points on plan
    private void drawPoint(float x, float y, float xfp, float yfp, float radius) {
        Bitmap bitmap = Bitmap.createBitmap(150, 650, Bitmap.Config.ARGB_8888);

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);

        Paint paintFp = new Paint();
        paintFp.setColor(Color.RED);
        paintFp.setAntiAlias(true);
        paintFp.setStyle(Paint.Style.FILL);
        paintFp.setStrokeWidth(1);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawCircle(x, y, radius, paint);
        canvas.drawCircle(xfp, yfp, radius, paintFp);

        mapImgView.setImageBitmap(bitmap);


    }

    // TODO: when user click on a button ? yes.
    private void startScan() {
//        unregisterReceiver(wifiReciever);
        // just for sure
        srs.clear();
        scanWifi();
    }

    // find nearest point and calculate position
    // according to k nearest point
    private FingerPrint findPoint(SamplePoint sp_) {
        ArrayList<Double> deltaRSSI = new ArrayList<>();
        ArrayList<Double> rmseForEachDir = new ArrayList<>();
        HashMap<Double, FingerPrint> fpRMSE;
        fpRMSE = new HashMap<Double, FingerPrint>();

        HashMap<Double, FingerPrint> knnFPs;
        knnFPs = new HashMap<Double, FingerPrint>();


        Double fpMinVal = 10e4;
        FingerPrint fpMin = new FingerPrint(0, 0.0, 0.0);
        boolean foundRouter = false;

//        ArrayList<NearFP> nfps = new ArrayList<>();

        // beine tamame finger print ha loop mizanim
        // va har 4 point ke dar har finger print darim baresi mikonim
        // har point dar direction haye motafavet (u-r-d-l)
        // shamele router haye motafavet hastan
        for (FingerPrint fp : fingerPrints
        ) {
            for (Point p : fp.getPoints()) {
                // inja router haye noghte morede nazar ra
                // ba router haye point p barresi mikonim
                for (SampleRouter sr : sp_.getWifiList()
                ) {
                    for (Router r : p.getWifiList()
                    ) {
                        // dar soorate moshtarak budan BSSID
                        // ekhtelap RSSI point morede nazar va mean RSSI p dar fp
                        // mohasebe mishavad
                        if (r.getBSSID().equals(sr.getBSSID())) {

                            Double deltaVal = Math.abs(r.getMeanRSSI() - sr.getRSSI());
                            deltaRSSI.add(deltaVal);
                            foundRouter = true;
                            break;
                        }

                    }
                    // dar soorati ke BSSID mojud nabud
                    // khode RSSI vare ezafe mishavad
                    if (!foundRouter) {
                        deltaRSSI.add(Double.parseDouble("" + sr.getRSSI()));
                    }
                    foundRouter = false;

                }
                // hasel jame deltaRSSI hesab mishavad
                Double sum = 0.0;
                for (Double d : deltaRSSI
                ) {
                    sum += (d * d);
                }
                // aval az RMSE estefade shod vali bad tasmim gereftim az MSE estefade konim
                // name variable hamchenan RMSE baghi mande ast
                Double RMSE = Math.sqrt(sum);
                deltaRSSI.clear();

                rmseForEachDir.add(RMSE);

            }
            Double foundedMinRMSE = findMinRMSE(rmseForEachDir);


            fpRMSE.put(foundedMinRMSE, fp);

            // peida kardan finger printi ke daraye kamtarin MSE ast
            rmseForEachDir.clear();
            if (fpMinVal > foundedMinRMSE) {
                fpMin = fp;
                fpMinVal = foundedMinRMSE;

            }

        }

        // sort kardan finger print ha bar asase MSE ha
        // baraye peida kardn k point nazdik
        ArrayList<Double> rmseValsForSort = new ArrayList<>(fpRMSE.keySet());
        Collections.sort(rmseValsForSort);

        ArrayList<FingerPrint> fpForFindMin = new ArrayList<>();

        // dar soorati ke Router haye moshtarake kamtari az k dashte basahim
        // in shart check mishavad
        // ta az crash kardan app jelogiri shavad
        if (rmseValsForSort.size() > kInKNN) {
            for (int i = 0; i < kInKNN; i++) {
                knnFPs.put(rmseValsForSort.get(i), fpRMSE.get(rmseValsForSort.get(i)));
                fpForFindMin.add(fpRMSE.get(rmseValsForSort.get(i)));
            }
        } else {
            for (int i = 0; i < rmseValsForSort.size(); i++) {
                knnFPs.put(rmseValsForSort.get(i), fpRMSE.get(rmseValsForSort.get(i)));
                fpForFindMin.add(fpRMSE.get(rmseValsForSort.get(i)));
            }
        }

        // another approach to find nearest finger print
//        nfps = findNearestPoint(fpForFindMin, sp);

        calcPointCoordinate(sp_, knnFPs);


        return fpMin;
    }


    // another approach to find nearest finger print
    // but it didn't use
    private ArrayList<NearFP> findNearestPoint(ArrayList<FingerPrint> fps, SamplePoint sp) {
        ArrayList<NearFP> nfps = new ArrayList<>();
        NearFP nfp;
        boolean findNFP = false;

        for (FingerPrint fp : fps
        ) {
            for (Point p : fp.getPoints()) {
                for (SampleRouter sr : sp.getWifiList()
                ) {
                    for (Router r : p.getWifiList()
                    ) {
                        if (r.getBSSID().equals(sr.getBSSID())) {

                            Double deltaVal = Math.abs(r.getMeanRSSI() - sr.getRSSI());
                            for (NearFP n : nfps
                            ) {
                                if (n.getBssid().equals(sr.getBSSID())) {

                                    if (n.getMinVal() > deltaVal) {
                                        n.setMinVal(deltaVal);
                                        n.setNumber(fp.getNumber());
                                    }
                                    findNFP = true;
                                    break;
                                }
                            }
                            if (!findNFP) {
                                nfp = new NearFP();
                                nfp.setBssid(sr.getBSSID());
                                nfp.setNumber(fp.getNumber());
                                nfp.setMinVal(Math.abs(r.getMeanRSSI() - sr.getRSSI()));
                                nfps.add(nfp);
                            }
                            findNFP = false;
                            break;
                        }

                    }

                }

            }

        }

        return nfps;
    }

    // calculate point coordinate according to k nearest neighbour
    private void calcPointCoordinate(SamplePoint sp, HashMap<Double, FingerPrint> fps) {
        Double coordX = 0.0;
        Double coordY = 0.0;
        Double sumWeights = 0.0;

        for (Double d : fps.keySet()) {
            coordX += ((1 / d) * fps.get(d).getX());
            coordY += ((1 / d) * fps.get(d).getY());
            sumWeights += (1 / d);
        }
        coordX /= sumWeights;
        coordY /= sumWeights;
        sp.setX(coordX);
        sp.setY(coordY);

    }

    // find min MSE between 4 direction (u-r-d-l)
    private Double findMinRMSE(ArrayList<Double> rmseFED) {
        Double min = rmseFED.get(0);

        for (int i = 1; i < rmseFED.size(); i++) {
            if (min > rmseFED.get(i)) {
                min = rmseFED.get(i);
            }
        }

        return min;
    }

    // get permissions
    private boolean checkPermissions() {
        int fineLocPrms = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocPrms = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int accessWifi = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE);
        int changeWifi = ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE);
        int externalWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int externalRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);


        List<String> listPermission = new ArrayList<>();
        if (fineLocPrms != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (coarseLocPrms != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (accessWifi != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if (changeWifi != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(Manifest.permission.CHANGE_WIFI_STATE);
        }
        if (externalWrite != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (externalRead != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!listPermission.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermission.toArray(new String[listPermission.size()]), 1);
        }

        return true;
    }
}