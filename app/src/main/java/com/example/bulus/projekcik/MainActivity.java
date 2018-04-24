package com.example.bulus.projekcik;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //zmienne
    private Button button, buttSave;
    private LocationManager locMan;
    private LocationListener locList;
    private TelephonyManager telMan;
    public TextView locLabelx, locLabely, operatorLabel, systemLabel, numerLabel, snrLabel, rssiLabel, rsrpLabel, numerText, snrText, rssiText, rsrpText, levelLabel;
    private String tmpOperator, tmpIdn, tmpStr, tmpSnr, tmpRsrp, tmpRssi, tmplevel, lastWord, tmpSystem, tmpx, tmpy;
    private int netType, mInterval = 5000;
    public String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/test", text;
    private Handler mHandler;

    SignalStrengthListener signalStrengthListener;
    TelephonyManager tm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        //przypisanie interfejsu
        operatorLabel=(TextView)findViewById(R.id.operator);
        systemLabel=(TextView)findViewById(R.id.system);
        numerLabel=(TextView)findViewById(R.id.numerStacji);
        snrLabel=(TextView)findViewById(R.id.snrValue);
        rssiLabel=(TextView)findViewById(R.id.rssiValue);
        rsrpLabel=(TextView)findViewById(R.id.rsrpValue);
        locLabelx=(TextView) findViewById(R.id.latitude);
        locLabely=(TextView) findViewById(R.id.longitude);
        numerText=(TextView) findViewById(R.id.textnumer);
        snrText=(TextView) findViewById(R.id.textsnr);
        rssiText=(TextView) findViewById(R.id.textrssi);
        rsrpText=(TextView) findViewById(R.id.textrsrp);
        levelLabel=(TextView) findViewById(R.id.poziomValue);
        button=(Button) findViewById(R.id.button);
        buttSave=(Button) findViewById(R.id.buttSave);

        //parametry sygnału odbieranego
        signalStrengthListener = new SignalStrengthListener();
        ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).listen(signalStrengthListener, SignalStrengthListener.LISTEN_SIGNAL_STRENGTHS);
        //tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telMan = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

        //zapis
        File dir = new File(path);
        dir.mkdirs();


        //GPS localization
        locMan = (LocationManager) getSystemService(LOCATION_SERVICE);
        locList = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                tmpx=location.getLatitude()+" ";
                tmpy=location.getLongitude()+" ";
                locLabelx.setText(tmpx.substring(0,10));
                locLabely.setText(tmpy.substring(0,10));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };

        //permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.INTERNET,
            },15);
            return;
        }else{
            locationButton();
            signalstr();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            },12);
            return;
        }else{
            saveButt();
        }
    }


    //request permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case 15:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    locationButton();
                    signalstr();
                return;
            case 12:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    saveButt();
                return;
        }
    }

    //oprogramowanie przycisku save
    private void saveButt() {
        buttSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Rozpoczęto zapis", Toast.LENGTH_SHORT).show();
                mHandler = new Handler();
                startRepeatingTask();

            }
        });
    }

    //uruchom cykliczny zapis
    void startRepeatingTask() {
        mStatusChecker.run();
    }

    //zarzadzanie strumieniem
    public static void Save(File file, String data){
        try{
            FileOutputStream fos = new FileOutputStream(file,true);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fos);
            myOutWriter.append(data+"\n");
            myOutWriter.close();
            fos.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    //timer
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                File file = new File(path+"/savedFile.txt");
                if(tmpSystem.equals("4G/LTE")){
                    text = "Oper:"+tmpOperator+" Sys:"+tmpSystem+" Nr.kom:"+tmpIdn+" CQI:"+tmpSnr+" RSRQ:"+tmpRssi+" RSRP:"+tmpRsrp+" Syg.lvl:"+tmplevel;
                }else{
                    text = "Oper:"+tmpOperator+" Sys:"+tmpSystem+" Nr.kom:"+tmpIdn+" SNR:"+tmpSnr+" RSSI:"+tmpStr+" RSRP:"+lastWord+" Syg.lvl:"+tmplevel;
                }
                Save(file, text);
            } finally {
                mHandler.postDelayed(this, mInterval);
            }
        }
    };

    //oprogramowanie przycisku lokalizacji
    private void locationButton() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Lokalizowanie", Toast.LENGTH_SHORT).show();
                locMan.requestLocationUpdates("gps", 5000, 0, locList);
            }
        });
    }


    //identyfikacja systemu
    public void signalstr(){
        try {
            List<CellInfo> cellInfos = telMan.getAllCellInfo();
            if (cellInfos != null) {
                for (int i = 0; i < cellInfos.size(); i++) {
                    if (cellInfos.get(i).isRegistered()) {
                        if (cellInfos.get(i) instanceof CellInfoWcdma) {
                            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) telMan.getAllCellInfo().get(0);
                            CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
                            CellIdentityWcdma cellIdentityWcdma = cellInfoWcdma.getCellIdentity();
                            tmpStr = String.valueOf(cellSignalStrengthWcdma.getDbm());
                            tmpSnr = String.valueOf(cellSignalStrengthWcdma.toString());
                            lastWord = tmpSnr.substring(tmpSnr.lastIndexOf("=")+1);
                            tmpIdn = String.valueOf(cellIdentityWcdma.getPsc());
                            tmplevel= String.valueOf(cellSignalStrengthWcdma.getLevel())+"/4";
                            rsrpLabel.setText(lastWord);
                            rssiLabel.setText(tmpStr);
                            numerLabel.setText(tmpIdn);
                            levelLabel.setText(tmplevel);
                        } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                            CellInfoGsm cellInfogsm = (CellInfoGsm) telMan.getAllCellInfo().get(0);
                            CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                            CellIdentityGsm cellIdentityGsm = cellInfogsm.getCellIdentity();
                            tmpSnr = String.valueOf(cellSignalStrengthGsm.toString());
                            lastWord = tmpSnr.substring(33,36);
                            tmpStr = String.valueOf(cellSignalStrengthGsm.getDbm());
                            tmpIdn = String.valueOf(cellIdentityGsm.getCid());
                            tmplevel = String.valueOf(cellSignalStrengthGsm.getLevel())+"/4";
                            rsrpLabel.setText(lastWord);
                            rssiLabel.setText(tmpStr);
                            numerLabel.setText(tmpIdn);
                            levelLabel.setText(tmplevel);
                        } else if (cellInfos.get(i) instanceof CellInfoLte) {
                            CellInfoLte cellInfoLte = (CellInfoLte) telMan.getAllCellInfo().get(0);
                            CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                            CellIdentityLte cellIdentityLte = cellInfoLte.getCellIdentity();
                            //tmpStr = String.valueOf(cellSignalStrengthLte.getDbm());
                            tmpIdn = String.valueOf(cellIdentityLte.getPci());
                            tmplevel = String.valueOf(cellSignalStrengthLte.getLevel())+"/4";
                            numerLabel.setText(tmpIdn);
                            levelLabel.setText(tmplevel);
                        }
                    }
                }
            }
        }catch (Exception e){
            tmpIdn = "exp";
            tmpStr = "exp";
        }
    }

    //rodzaj sieci
    private void rodzajSieci() {
        try{
            netType = telMan.getNetworkType();
            switch (netType)
            {
                case 7: tmpSystem = "2G/1xRTT"; break;
                case 4: tmpSystem = "2G/CDMA"; break;
                case 2: tmpSystem = "2G/EDGE"; break;
                case 14: tmpSystem = "2G/eHRPD"; break;
                case 5: tmpSystem = "3G/EVDO rev. 0"; break;
                case 6: tmpSystem = "3G/EVDO rev. A"; break;
                case 12: tmpSystem = "3G/EVDO rev. B"; break;
                case 1: tmpSystem = "2G/GPRS"; break;
                case 8: tmpSystem = "3G/HSDPA"; break;
                case 10: tmpSystem = "3G/HSPA"; break;
                case 15: tmpSystem = "3G/HSPA+"; break;
                case 9: tmpSystem = "3G/HSUPA"; break;
                case 11: tmpSystem = "2G/iDen"; break;
                case 13: tmpSystem = "4G/LTE"; break;
                case 3: tmpSystem = "3G/UMTS"; break;
                case 0: tmpSystem = "Unknown"; break;
            }
            systemLabel.setText(tmpSystem);
        }catch(Exception e){
            systemLabel.setText("exp");
        }
    }

    //nazwa operatora
    private void operator(){
        try{
            tmpOperator = tm.getNetworkOperatorName();
            operatorLabel.setText(tmpOperator);
        }catch(Exception e){
            operatorLabel.setText("exp");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //update interfejsu
    private class SignalStrengthListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(android.telephony.SignalStrength signalStrength) {

            //aktualizacja rodzaju sieci, operatora, parametrow sygnalu
            signalstr();
            rodzajSieci();
            operator();

            //element nasluchujacy zmian sygnalu
            ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).listen(signalStrengthListener, SignalStrengthListener.LISTEN_SIGNAL_STRENGTHS);
            tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

            try{
                String x = "2147483647";
                String ltestr = signalStrength.toString();
                String[] parts = ltestr.split(" ");

                 if (!parts[11].equals(x)){
                     rsrpText.setText("RSRP [dBM]");
                     rssiText.setText("RSRQ [dB] :");
                     snrText.setText("CQI :");
                     tmpSnr = parts[12];
                     tmpRsrp = parts[9];
                     tmpRssi = parts[10];
                     rsrpLabel.setText(String.valueOf(tmpRsrp));
                     snrLabel.setText(String.valueOf(tmpSnr));
                     rssiLabel.setText(String.valueOf(tmpRssi));
                }else{
                     snrText.setText("Ec/Io [dB*10] :");
                     rssiText.setText("RSSI[dBm] :");
                     rsrpText.setText("BER :");
                     tmpSnr = String.valueOf(signalStrength.getEvdoSnr());
                     snrLabel.setText(tmpSnr);
                }
            }catch(Exception e){
                rsrpLabel.setText("exp");
                snrLabel.setText("exp");
            }
            super.onSignalStrengthsChanged(signalStrength);
            //++++++++++++++++++++++++++++++++++++
            /*
            part[0] = "Signalstrength:"   title
            parts[1] = GsmSignalStrength  T
            parts[2] = GsmBitErrorRate    N
            parts[3] = CdmaDbm            T
            parts[4] = CdmaEcio           N
            parts[5] = EvdoDbm            T
            parts[6] = EvdoEcio           N
            parts[7] = EvdoSnr            N
            parts[8] = LteSignalStrength
            parts[9] = LteRsrp
            parts[10] = LteRsrq
            parts[11] = LteRssnr
            parts[12] = LteCqi
            parts[13] = gsm|lte|cdma
            parts[14] = _not really sure what this number is_
            */
        }
    }
}