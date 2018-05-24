package com.msc.ics.ocr4cam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    SurfaceView cameraView;
    CameraSource cameraSource;

    String sFilename = "mylastpic";
    boolean bSaveFileToggle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = (SurfaceView)findViewById(R.id.surfaceView);
        textView = (TextView) findViewById(R.id.textView);

        bSaveFileToggle = false;

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if(!textRecognizer.isOperational()) {
            Toast.makeText(this,"Not available!", Toast.LENGTH_SHORT).show();
        }
        else {

            cameraSource = new CameraSource.Builder(getApplicationContext(),textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setAutoFocusEnabled(true)
                    .build();

            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    try {
                        cameraSource.start(cameraView.getHolder());
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }
            });
        }

        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                final SparseArray<TextBlock> items = detections.getDetectedItems();
                if (items.size() != 0) {
                    textView.post(new Runnable() {
                        @Override
                        public void run() {
                            String sData = "";
                            StringBuilder stringBuilder = new StringBuilder();
                            for(int i=0;i<items.size();i++) {
                                TextBlock item = items.valueAt(i);
                                sData = getFormNumber( item.getValue());

                                if (isThaiNID(sData)) //(sData.length() == 17)
                                {
                                    //stringBuilder.append(sData + " [" + sData.length()+"]\n");
                                    String sThaiID = sData.replace(" ","");
                                    stringBuilder.append("\nThai ID: " + sThaiID);

                                    if (!bSaveFileToggle) //(!sFilename.equals(sThaiID))
                                    {
                                        sFilename = sThaiID;
                                        cameraSource.takePicture(null, pictureCallback);
                                    }

                                    bSaveFileToggle = !bSaveFileToggle;
                                }
                            }

                            if (stringBuilder.length() > 0)
                                textView.setText(stringBuilder.toString());
                        }
                    });
                }
            }
        });
    }


    private String getFormNumber(String sData) {
        String sTmp = "";
        byte[] arrTmp = sData.getBytes();
        for (byte iDat: arrTmp) {
            //>> keep number & space
            if (iDat>=0x30 && iDat<=0x39 || iDat==0x20) {
                sTmp += (char)iDat;
                continue;
            }

            //>> skip "-"
            if (iDat==0x2D) {
                sTmp = ""; //"Has minus";
                break;
            }
        }

        //>> chk has space contains
        if (sTmp.length()>0 && !sTmp.contains(" "))
            sTmp = ""; //"No space";

        return  sTmp.trim();
    }

    private boolean isThaiNID(String sData) {
        //>> Thai ID format = "N NNNN NNNNN NN N"
        String[] arrData = sData.split(" ");

        if (arrData.length != 5)
            return false;

        return
                arrData[0].length()==1
                && arrData[1].length()==4
                && arrData[2].length()==5
                && arrData[3].length()==2
                && arrData[4].length()==1;
    }

    //>> Callback for 'takePicture'
    CameraSource.PictureCallback pictureCallback = new CameraSource.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes) {
            File file_image = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES+"/pics");
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes , 0, bytes .length);
            if(bitmap!=null){
                if(!file_image.isDirectory())
                    file_image.mkdir();

                file_image=new File(file_image,sFilename + ".jpg");
                try{
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    bitmap = Bitmap.createBitmap(
                            bitmap ,
                            0,
                            0,
                            bitmap .getWidth(),
                            bitmap .getHeight(),
                            matrix,
                            true);

                    FileOutputStream fileOutputStream=new FileOutputStream(file_image);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,80, fileOutputStream);

                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
                catch(Exception exception) {
                    Toast.makeText(getApplicationContext(),"Error saving: "+ exception.toString(),Toast.LENGTH_LONG).show();
                }
            }
        }
    };

}
