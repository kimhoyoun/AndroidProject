package com.example.loginproject.Health;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.loginproject.R;
import com.example.loginproject.user.UserAccount;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;

public class RunBicycle extends AppCompatActivity implements OnMapReadyCallback {

    // νμ¬μμΉ
    private GpsTracker gpsTracker;
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    // νμ΄μ΄λ² μ΄μ€
    private FirebaseAuth mFirebaseAuth;     // νμ΄μ΄λ² μ΄μ€ κ³μ  κ°μ Έμ€κΈ°
    private FirebaseUser firebaseUser;      // νμ¬ λ‘κ·ΈμΈ ν μ μ 
    private DatabaseReference mDatabaseRef; // μ€μκ° λ°μ΄ν°λ² μ΄μ€
    long timetime;

    // ν΄λ¦¬λΌμΈ μμ(λΉ¨κ°, κ²μ )
    private static final int COLOR_RED_ARGB = 0xffff0000; // -> μμμ μμ
    private static final int COLOR_BLACK_ARGB = 0xff000800; // -> μ’λ£μ μμ

    // μ²μ μλ, κ²½λ
    private double firstx, firsty;
    // λ§μ§λ§ μλ, κ²½λ
    private double lastx, lasty;

    // μ κ·Έλ¦¬κΈ°
    private PolylineOptions poloptions;
    // μμ, μ€μ§, μ’λ£ λ²νΌ
    private Button start, pause, stop;
    // μ€λ λ
    Thread thread;
    private boolean isThread = false;
    private boolean clearLo = true;

    // μμ λ²νΌ ν΄λ¦­ μ
    private int startCount = 0;

    // κ΅¬κΈλ§΅
    private GoogleMap gMap;

    // μλ, κ²½λ λ³κ²½ν΄λ³΄κΈ°
    double lati, longi;

    // λ μ§ μ΄κΈ°ν
    LocalDate date = LocalDate.now();

    // μλ κ²½λλ‘ κ±°λ¦¬κ΅¬νκΈ°
    String num; // -> κ±°λ¦¬
    //double totalkcal; // -> μΉΌλ‘λ¦¬
    public void changeLo(double lati, double longi) {
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lati, longi), 18));
    }

    // μμ μ’λ£ μκ° κ΅¬νκΈ°
    long firstStartTime;
    long startTime;
    long endTime;
    long pasueTime;
    long pasueStart; // -> μμνκ³  λ©μΆμμ λμ μκ°

    // κ±·κΈ°, μμ κ±° κ΅¬λΆνλ λ³μ
    int choice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_bicycle);

        Intent intent = getIntent();
        choice = intent.getIntExtra("choice",1);



        // νμ΄μ΄λ² μ΄μ€ λ‘κ·ΈμΈ μ λ³΄
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("DB");
        mFirebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = mFirebaseAuth.getCurrentUser();

        gpsTracker = new GpsTracker(RunBicycle.this);

        // μ μ κ·Έλ¦΄ μ μκ² νλ²λ νμ¬ μμΉμ κ²½λ, μλλ₯Ό λ€λ₯Έ λ³μμ λ΄μμ€
        lati = gpsTracker.getLatitude();
        longi = gpsTracker.getLongitude();
        lastx = lati;
        lasty = longi;

        // SupportMapFragmentμ ν΅ν΄ λ μ΄μμμ λ§λ  fragmentμ IDλ₯Ό μ°Έμ‘°νκ³  κ΅¬κΈλ§΅μ νΈμΆνλ€.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // μμλ²νΌ
        start = (Button) findViewById(R.id.start);

        start.setOnClickListener(new OnSingleClickListener() {

            @Override
            public void onSingleClick(View v) {
                if(startCount == 0){
                    firstStartTime = System.currentTimeMillis();
                }
                // startCountκ° 1μ΄λ©΄ λ©μΆκΈ° stopλ²νΌμ startCountλ₯Ό λ£μ΄μ£Όμλ€.
                if (startCount == 1) {return;}
                // μ΄ λμ΄κΈ° μμ -> μΉΌλ‘λ¦¬ κ³μ°μ μνμ¬ λ£μλ€.
                startTime = System.currentTimeMillis();
                System.out.println("startTime : "+startTime);
                // μ κ·Έλ¦¬κΈ° μμ μμΉ

                poloptions = new PolylineOptions();
                poloptions.add(new LatLng(lati, longi));

                // μμλ²νΌ ν΄λ¦­νμ νμΈνκ³  μ§λ μ§μ°κΈ°
                // μ€λ λ μμ
                isThread = true;
                thread = new Thread() {
                    public void run() {

                        while (isThread) {
                            try {
                                if (isThread == false) {
                                    return;
                                }
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        if (isThread == false) {
                                            return;
                                        }
                                        System.out.println("μ€λ λκ° λλλ€.");
                                        gpsTracker = new GpsTracker(RunBicycle.this);
                                        // κ²½λ, μλ λν΄μ£ΌκΈ°
//                                        lati += 0.0001;
//                                        longi += 0.0001;
                                        lati = gpsTracker.getLatitude();
                                        longi = gpsTracker.getLongitude();

                                        changeLo(lati, longi);
                                        // κ³μ add
                                        poloptions.add(new LatLng(lati, longi));
                                        // μ μ λ£μ΄μ£ΌκΈ°
                                        Polyline polyline1 = gMap.addPolyline(poloptions.clickable(true));
                                        polyline1.setColor(COLOR_RED_ARGB);
                                        // μλ, κ²½λ λ³νλ κ±° λ³΄μ¬μ£ΌκΈ°
                                        //tv1.setText("μλ, κ²½λ λ³ν / μμ ν΄λ¦­νμ : " + String.format("%.4f", lati) + ", " + String.format("%.4f", longi) + "/ " + startCount);
                                    }
                                });
                                // 1μ΄ κ±Έλ¦Ό
                                sleep(1000);
                            } catch (InterruptedException e) {
                                return;
                            }
                        }

                    }
                };
                thread.start();
            }
        });


        // μ μ§λ²νΌ
        pause = (Button) findViewById(R.id.pause);
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pasueTime = System.currentTimeMillis();
                pasueStart = pasueTime - startTime;
                timetime += pasueStart;
                Toast.makeText(RunBicycle.this, timetime+"", Toast.LENGTH_SHORT).show();
                System.out.println("pasueStart : "+pasueStart);
                Toast.makeText(getApplicationContext(), "μ μ λ©μΆκ² μ΅λλ€.", Toast.LENGTH_SHORT).show();
                isThread = false;
                startCount = 2;
            }
        });

        // μ’λ£λ²νΌ
        stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // μ΄ λμ΄κΈ° μ’λ£ -> μΉΌλ‘λ¦¬ κ³μ°μ μνμ¬ λ£μλ€.
                endTime = System.currentTimeMillis();
                Toast.makeText(RunBicycle.this, endTime+"", Toast.LENGTH_SHORT).show();
                System.out.println("endTime : "+endTime);
                System.out.println("startTime :" + startTime);
                isThread = false;
                gMap.clear();

                firstx = lati;
                firsty = longi;
                // μ  κ·Έλ¦¬κΈ°
//                poloptions = new PolylineOptions();
//                poloptions.add(new LatLng(lastx, lasty));
//                poloptions.add(new LatLng(firstx, firsty));
//                Polyline polyline1 = gMap.addPolyline(poloptions.clickable(true));
//                polyline1.setColor(COLOR_BLACK_ARGB);

                startCount = 1;

                mDatabaseRef.child("UserAccount").child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        UserAccount userAccount = snapshot.getValue(UserAccount.class);

                        AlertDialog.Builder dlg = new AlertDialog.Builder(RunBicycle.this);
                        dlg.setTitle("κ±°λ¦¬μ μλͺ¨ν μΉΌλ‘λ¦¬λ?");
                        dlg.setMessage("κ±°λ¦¬λ "+ getDistance(lastx, lasty, firstx, firsty) + "m μ΄κ³ ," + "\n" + "μλͺ¨ν μΉΌλ‘λ¦¬λ "
                                + String.format("%.2f", kcal(endTime, startTime, userAccount.getWeight() )) + "Kcal μλλ€.");
                        dlg.setPositiveButton("νμΈ", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

//                                long seq = 1;
//                                Record record = new Record((int)seq, userAccount.getIdToken(),1, num,
//                                        String.valueOf(date), String.format("%.2f", kcal(endTime, startTime, userAccount.getWeigth())) );
//                                mDatabaseRef.child("Record").child(seq+"").setValue(record);
//                                mDatabaseRef.child("Record").child("Sequence").setValue(seq +1);

                                mDatabaseRef.child("Record").child("Sequence").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                                        if(!task.isSuccessful()){
                                            System.out.println("μ€ν¨");
                                        }else{
                                            long seq = (Long)task.getResult().getValue();
                                            System.out.println("(Long)task.getResult().getValue()~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"+(long)seq);
                                                //Record record = new Record((int)seq, emailId, 1, num, String.valueOf(date), String.format("%.2f", kcal(endTime, startTime)));
                                                if(choice == 1){
                                                    Record record = new Record((int)seq, userAccount.getIdToken(),1, num,
                                                            String.valueOf(date), String.format("%.2f", kcal(endTime, startTime, userAccount.getWeight())) );
                                                    mDatabaseRef.child("Record").child(seq+"").setValue(record);
                                                    mDatabaseRef.child("Record").child("Sequence").setValue(seq +1);
                                                } else {
                                                    Record record = new Record((int)seq, userAccount.getIdToken(),2, num,
                                                            String.valueOf(date), String.format("%.2f", kcal(endTime, startTime, userAccount.getWeight())) );
                                                    mDatabaseRef.child("Record").child(seq+"").setValue(record);
                                                    mDatabaseRef.child("Record").child("Sequence").setValue(seq +1);
                                            }
                                        }
                                    }
                                });

                                Toast.makeText(RunBicycle.this, "νμΈμ λλ μ΅λλ€", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RunBicycle.this,HealthMain.class);
                                startActivity(intent);
                                finish();
                                dialog.cancel();
                            }
                        }).setCancelable(false);
                        AlertDialog alertDialog = dlg.create();
                        alertDialog.show();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

    } // end of onCreate


    // μΉΌλ‘λ¦¬ κ΅¬νκΈ°
    public double kcal(long endTime, long startTime, String weight){
        // κ±·κΈ°λ 3.5 mets μ΄λ€.
        double runmets;
        // 3.5mets = 12.25ml/kg/min;
        long centerTime = timetime/1000;
        long whatTime = (endTime - firstStartTime)/1000;

        long KcalTime = whatTime/60;
        long restTime = centerTime/60;

        System.out.println("centerTime : "+centerTime);
        System.out.println("whatTime : "+whatTime );
        System.out.println("first : " + firstStartTime );
        System.out.println("KcalTime : "+KcalTime + "μ¬κΈ° μμ΄μμ€μ€μ€μ€μ€μ€μ€μ€μ€μ€");
        System.out.println("restTime : "+restTime + "μ¬κΈ° μμ΄μμ€μ€μ€μ€μ€μ€μ€μ€μ€μ€");

        // κ±·κΈ° μΉΌλ‘λ¦¬ κ³μ°νκΈ° ->> firebaseμ μλ μ μ  λͺΈλ¬΄κ² κ°μ Έμμ κ³μ°
        double userWeight = Double.parseDouble(weight);
        //μΌλ°μ μΈ κ±·κΈ° => 3.5mets = 12.25ml/kg/min

        if(choice == 1) {
            runmets = 12.25 * userWeight * (KcalTime-restTime); // 4μ΄λ©΄ 2450.0
        } else {
            runmets = 14 * userWeight * (KcalTime-restTime);
        }

        double onekcal = runmets * 0.005; // 1μΉΌλ‘λ¦¬λ 12.25
        double totalkcal = onekcal * (KcalTime-restTime); // 4μ΄λ°λ©΄ 49 μΉΌλ‘λ¦¬
        System.out.println("totalKcal " + totalkcal + "μ¬κΈ° μμ΄μμ€μ€μ€μ€μ€μ€μ€μ€μ€");

        return totalkcal;
    }

    // κ²½λ, μλλ‘ κ±°λ¦¬ κ΅¬νκΈ°
    public String getDistance(double firstx, double firsty, double lastx, double lasty) {
        double distance;
        Location locationA = new Location("point A");
        locationA.setLatitude(firstx);
        locationA.setLongitude(firsty);
        Location locationB = new Location("point B");
        locationB.setLatitude(lastx);
        locationB.setLongitude(lasty);
        distance = locationA.distanceTo(locationB);
        if (distance > 1000) {
            distance = 500;
        } else if (distance == 0) {
            Random randomGenerator = new Random();
            int start = 1;
            int end = 20;
            double range = end - start + 1;
            int randomInt5to10 = (int) (randomGenerator.nextDouble() * range + start);
            distance = randomInt5to10 + randomGenerator.nextDouble();
        }
        if (distance == 500) {
            num = String.format("%.0f", distance) + "+";
            return num;
        } else {
            num = String.format("%.1f", distance);
            return num;
        }
    } // κ²½λ, μλλ‘ κ±°λ¦¬ κ΅¬νκΈ°


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.gMap = googleMap;
        if (ContextCompat.checkSelfPermission(RunBicycle.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            checkLocationPermissionWithRationale();
        }
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermissionWithRationale() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("μμΉμ λ³΄")
                        .setMessage("μ΄ μ±μ μ¬μ©νκΈ° μν΄μλ μμΉμ λ³΄μ μ κ·Όμ΄ νμν©λλ€. μμΉμ λ³΄ μ κ·Όμ νμ©νμ¬ μ£ΌμΈμ.")
                        .setPositiveButton("νμΈ", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(RunBicycle.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);

                            }
                        }).create().show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }


    // μμ λ²νΌ λλΈν΄λ¦­ λ°©μ§
    public abstract class OnSingleClickListener implements View.OnClickListener {
        //μ€λ³΅ν΄λ¦­μκ°μ°¨μ΄
        private static final long MIN_CLICK_INTERVAL = 1000;

        //λ§μ§λ§μΌλ‘ ν΄λ¦­ν μκ°
        private long mLastClickTime;

        public abstract void onSingleClick(View v);

        @Override
        public final void onClick(View v) {
            //νμ¬ ν΄λ¦­ν μκ°
            long currentClickTime = SystemClock.uptimeMillis();
            //μ΄μ μ ν΄λ¦­ν μκ°κ³Ό νμ¬μκ°μ μ°¨μ΄
            long elapsedTime = currentClickTime - mLastClickTime;
            //λ§μ§λ§ν΄λ¦­μκ° μλ°μ΄νΈ
            mLastClickTime = currentClickTime;

            //λ΄κ° μ ν μ€λ³΅ν΄λ¦­μκ° μ°¨μ΄λ₯Ό μλμμΌλ©΄ ν΄λ¦­μ΄λ²€νΈ λ°μλͺ»νκ² return
            if (elapsedTime <= MIN_CLICK_INTERVAL)
                return;
            //μ€λ³΅ν΄λ¦­μκ° μλλ©΄ μ΄λ²€νΈ λ°μ
            onSingleClick(v);
        }
    } // μμ λ²νΌ λλΈν΄λ¦­ λ°©μ§

    /* ActivityCompat.requestPermissionsλ₯Ό μ¬μ©ν νΌλ―Έμ μμ²­μ κ²°κ³Όλ₯Ό λ¦¬ν΄λ°λ λ©μλμλλ€. */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, @NonNull String[] permissions, @NonNull int[] grandResults) {
        super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults);
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // μμ²­ μ½λκ° PERMISSIONS_REQUEST_CODE μ΄κ³ , μμ²­ν νΌλ―Έμ κ°μλ§νΌ μμ λμλ€λ©΄
            boolean check_result = true;
            // λͺ¨λ  νΌλ―Έμμ νμ©νλμ§ μ²΄ν¬ν©λλ€.
            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }
            if (check_result) {
                //μμΉ κ°μ κ°μ Έμ¬ μ μμ ;
            } else {
                // κ±°λΆν νΌλ―Έμμ΄ μλ€λ©΄ μ±μ μ¬μ©ν  μ μλ μ΄μ λ₯Ό μ€λͺν΄μ£Όκ³  μ±μ μ’λ£ν©λλ€.2 κ°μ§ κ²½μ°κ° μμ΅λλ€.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {
                    Toast.makeText(RunBicycle.this, "νΌλ―Έμμ΄ κ±°λΆλμμ΅λλ€. μ±μ λ€μ μ€ννμ¬ νΌλ―Έμμ νμ©ν΄μ£ΌμΈμ.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(RunBicycle.this, "νΌλ―Έμμ΄ κ±°λΆλμμ΅λλ€. μ€μ (μ± μ λ³΄)μμ νΌλ―Έμμ νμ©ν΄μΌ ν©λλ€. ", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    void checkRunTimePermission() {
        //λ°νμ νΌλ―Έμ μ²λ¦¬
        // 1. μμΉ νΌλ―Έμμ κ°μ§κ³  μλμ§ μ²΄ν¬ν©λλ€.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(RunBicycle.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(RunBicycle.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. μ΄λ―Έ νΌλ―Έμμ κ°μ§κ³  μλ€λ©΄
            // ( μλλ‘μ΄λ 6.0 μ΄ν λ²μ μ λ°νμ νΌλ―Έμμ΄ νμμκΈ° λλ¬Έμ μ΄λ―Έ νμ©λ κ±Έλ‘ μΈμν©λλ€.)

            // 3.  μμΉ κ°μ κ°μ Έμ¬ μ μμ

        } else {  //2. νΌλ―Έμ μμ²­μ νμ©ν μ μ΄ μλ€λ©΄ νΌλ―Έμ μμ²­μ΄ νμν©λλ€. 2κ°μ§ κ²½μ°(3-1, 4-1)κ° μμ΅λλ€.
            // 3-1. μ¬μ©μκ° νΌλ―Έμ κ±°λΆλ₯Ό ν μ μ΄ μλ κ²½μ°μλ
            if (ActivityCompat.shouldShowRequestPermissionRationale(RunBicycle.this, REQUIRED_PERMISSIONS[0])) {
                // 3-2. μμ²­μ μ§ννκΈ° μ μ μ¬μ©μκ°μκ² νΌλ―Έμμ΄ νμν μ΄μ λ₯Ό μ€λͺν΄μ€ νμκ° μμ΅λλ€.
                Toast.makeText(RunBicycle.this, "μ΄ μ±μ μ€ννλ €λ©΄ μμΉ μ κ·Ό κΆνμ΄ νμν©λλ€.", Toast.LENGTH_LONG).show();
                // 3-3. μ¬μ©μκ²μ νΌλ―Έμ μμ²­μ ν©λλ€. μμ²­ κ²°κ³Όλ onRequestPermissionResultμμ μμ λ©λλ€.
                ActivityCompat.requestPermissions(RunBicycle.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            } else {
                // 4-1. μ¬μ©μκ° νΌλ―Έμ κ±°λΆλ₯Ό ν μ μ΄ μλ κ²½μ°μλ νΌλ―Έμ μμ²­μ λ°λ‘ ν©λλ€.
                // μμ²­ κ²°κ³Όλ onRequestPermissionResultμμ μμ λ©λλ€.
                ActivityCompat.requestPermissions(RunBicycle.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }
    }
    //μ¬κΈ°λΆν°λ GPS νμ±νλ₯Ό μν λ©μλλ€
    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(RunBicycle.this);
        builder.setTitle("μμΉ μλΉμ€ λΉνμ±ν");
        builder.setMessage("μ±μ μ¬μ©νκΈ° μν΄μλ μμΉ μλΉμ€κ° νμν©λλ€.\n"
                + "μμΉ μ€μ μ μμ νμ€λμ?");
        builder.setCancelable(true);
        builder.setPositiveButton("μ€μ ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("μ·¨μ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                //μ¬μ©μκ° GPS νμ± μμΌ°λμ§ κ²μ¬
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS νμ±ν λμμ");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
        }
    }
    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}