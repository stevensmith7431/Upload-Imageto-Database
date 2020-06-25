package com.example.uploadimagetodb;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.MarshalBase64;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String NAMESPACE = "http://tempuri.org/";

    Button request;
    Button btn_capture;
    RadioButton male;
    RadioButton female;
    RadioButton transgender;
    RadioGroup radioGroup;
    Spinner spinner;
    String s1, s2, s3,s,str_spinner;
    Bitmap bitmap;
    public  static ImageView img_preview  = null;
    static final int REQUEST_PICTURE_CAPTURE = 1;
    private String pictureFilePath;
    public static Context ctx;
    ArrayList<String> arrayList;
    ArrayAdapter<String> adapter;
    public JSONArray jsonArray=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new
                StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        request = findViewById(R.id.requestid);
        male = findViewById(R.id.radio1);
        female = findViewById(R.id.radio2);
        transgender = findViewById(R.id.radio3);
        radioGroup = findViewById(R.id.groupid);
        spinner=findViewById(R.id.spins);

        ctx = this;

        img_preview=findViewById(R.id.img_preview);
        btn_capture=findViewById(R.id.btn_capture);

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            btn_capture.setEnabled(false);
        }

        getWindow().setSoftInputMode(

                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        (new getSpinner()).execute();

        request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (male.isChecked()) {
                    s1 = male.getText().toString();
                    s = male.getText().toString();
                } else if (female.isChecked()) {
                    s2 = female.getText().toString();
                    s = female.getText().toString();
                } else if (transgender.isChecked()) {
                    s3 = transgender.getText().toString();
                    s = transgender.getText().toString();
                }
                str_spinner = spinner.getSelectedItem().toString();

                if (radioGroup.getCheckedRadioButtonId() == -1) {
                    Toast.makeText(MainActivity.this, "Please select the gender", Toast.LENGTH_SHORT).show();

                }  else if (male.isChecked() && bitmap == null || female.isChecked() && bitmap == null
                        || transgender.isChecked() && bitmap == null){

                    Toast.makeText(MainActivity.this, "Please take a photo", Toast.LENGTH_SHORT).show();
                }

                else {
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    startActivity(intent);
                    String[] res = {s,"remarks","P",str_spinner,"number"};
                    (new InsertwithImage()).execute(res);
                }
            }
        });

        btn_capture.setOnClickListener(capture);

    }


    private View.OnClickListener capture = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
                sendTakePictureIntent();
            }
        }
    };
    private void sendTakePictureIntent() {

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File pictureFile = null;
            try {
                pictureFile = getPictureFile();
            } catch (IOException ex) {
                Toast.makeText(this,
                        "Photo file can't be created, please try again",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (pictureFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.imageupload.provider",
                        pictureFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                    cameraIntent.setClipData(ClipData.newRawUri("", photoURI));
                    cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                startActivityForResult(cameraIntent, REQUEST_PICTURE_CAPTURE);
            }
        }
    }

    private File getPictureFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String pictureFile = "AppName" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(pictureFile,  ".jpg", storageDir);
        pictureFilePath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICTURE_CAPTURE && resultCode == RESULT_OK) {
            File imgFile = new File(pictureFilePath);
            if (imgFile.exists()) {
                img_preview.setImageURI(Uri.fromFile(imgFile));
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(ctx.getContentResolver(), Uri.fromFile(imgFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public class getSpinner extends AsyncTask<String, String, String> {

        ProgressDialog bar;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bar = new ProgressDialog(MainActivity.this);
            bar.setCancelable(false);
            bar.setMessage("Please Wait");
            bar.setIndeterminate(true);
            bar.setCanceledOnTouchOutside(false);
            bar.show();
        }

        @Override
        protected String doInBackground(String... strings) {

            String methodname = "";
            String URL = "http://file/Service/Service.asmx";
            return WebService.WebServiceCall(null,null,methodname, NAMESPACE, URL);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            bar.dismiss();
            arrayList = new ArrayList<String>();

            try {
                jsonArray = new JSONArray(result);
                for(int i=0;i<jsonArray.length();i++) {

                    JSONObject jsonObject1=new JSONObject(jsonArray.get(i).toString());
                    String co=jsonObject1.getString("UnitName");
                    arrayList.add(co);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            adapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1,arrayList);
            spinner.setAdapter(adapter);

        }
    }


    public class InsertwithImage extends AsyncTask<String, String, String>{

        byte[] data;
        ProgressDialog bar;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bar = new ProgressDialog(MainActivity.this);
            bar.setCancelable(false);
            bar.setMessage("Upload Image");
            bar.setIndeterminate(true);
            bar.setCanceledOnTouchOutside(false);
            bar.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String URL = "http://File/Service/Service.asmx";

            String responsetring = "";
            try {
                Bitmap bm = bitmap;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                data = baos.toByteArray();
            }
            catch(Exception er)
            {

            }
            try {
                SoapObject request = new SoapObject(NAMESPACE, "methodname");
                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                MarshalBase64 marshal=new MarshalBase64();
                marshal.register(envelope);
                envelope.dotNet = true;
                envelope.setOutputSoapObject(request);
                request.addProperty("f", data);
                request.addProperty("gender", params[0]);
                request.addProperty("remarks", params[1]);
                request.addProperty("status", params[2]);
                request.addProperty("name", params[3]);
                request.addProperty("studno", params[4]);
                HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
                try {
                    androidHttpTransport.call(NAMESPACE + "methodname", envelope);
                } catch (IOException | XmlPullParserException e) {
                    e.printStackTrace();

                }

                SoapPrimitive response;
                try {
                    response = (SoapPrimitive) envelope.getResponse();
                    responsetring = response.toString();
                } catch (SoapFault e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return responsetring;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            bar.dismiss();
            finish();
        }
    }
}
