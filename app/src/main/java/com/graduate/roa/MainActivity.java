package com.graduate.roa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.graduate.roa.network.APIClient;
import com.graduate.roa.network.APIInterface;
import com.graduate.roa.network.WavFile;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends Activity {
    private AudioReader audioReader;
    private int sampleRate = 16000;
    private int inputBlockSize = 256;
    private int sampleDecimate = 1;
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_DURATION_MS = 7000;
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    short[] audioData;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    APIInterface apiInterface;
    private CountDownTimer timer;
    private CountDownTimer timer2;
    private int value;
    Context context;
    private static final int REQUEST = 1;
    private static String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO};


    private int count=0;
    private Boolean flag=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, PERMISSIONS,REQUEST);
        apiInterface = APIClient.getClient().create(APIInterface.class);
        context=this;
        setButtonHandlers();

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING)*3;

        audioData = new short [bufferSize]; //short array that pcm data is put into.

        audioReader = new AudioReader();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        audioReader.startReader(sampleRate, inputBlockSize * sampleDecimate, new AudioReader.Listener()
        {
            @Override
            public final void onReadComplete(int dB)
            {
                Log.e("###", dB+" dB"+flag+"flag");
                if(dB>-20 && flag==false){
                    Log.i(">>>","");
                    NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

                    Intent notificationIntent = new Intent(context, MainActivity.class);
                    notificationIntent.putExtra("notificationId", "숫자"); //전달할 값
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK) ;
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent,  PendingIntent.FLAG_UPDATE_CURRENT);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "1001")
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.app_icon2)) //BitMap 이미지 요구
                            .setContentTitle("ROA")
                            .setContentText("큰 소리가 났어요. 아기가 우는지 확인해보세요")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent) // 사용자가 노티피케이션을 탭시 ResultActivity로 이동하도록 설정
                            .setAutoCancel(true);

                    //OREO API 26 이상에서는 채널 필요
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        builder.setSmallIcon(R.drawable.small_icon); //mipmap 사용시 Oreo 이상에서 시스템 UI 에러남
                        CharSequence channelName  = "노티페케이션 채널";
                        String description = "오레오 이상을 위한 것임";
                        int importance = NotificationManager.IMPORTANCE_HIGH;

                        NotificationChannel channel = new NotificationChannel("1001", channelName , importance);
                        channel.setDescription(description);

                        // 노티피케이션 채널을 시스템에 등록
                        assert notificationManager != null;
                        notificationManager.createNotificationChannel(channel);

                    }else builder.setSmallIcon(R.drawable.small_icon); // Oreo 이하에서 mipmap 사용하지 않으면 Couldn't create icon: StatusBarIcon 에러남

                    assert notificationManager != null;
                    notificationManager.notify(1234, builder.build()); // 고유숫자로 노티피케이션 동작시킴
                }
            }

            @Override
            public void onReadError(int error)
            {

            }
        });
    }

    private void setButtonHandlers() {
        ((ImageView)findViewById(R.id.start_stop)).setOnClickListener(btnClick);
    }

    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + "save" + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists())
            tempFile.delete();
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING,bufferSize);
        int i = recorder.getState();
        if (i==1) recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");

        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read = 0;
        if (null != os) {
            while(isRecording) {
                read = recorder.read(data, 0, bufferSize);
                if (read > 0){
                }

                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        if (null != recorder){
            isRecording = false;

            int i = recorder.getState();
            if (i==1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(),getFilename());
        deleteTempFile();
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            Log.d("debug","data+"+in.toString());

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1) {
                out.write(data);
                Log.d("data size", String.valueOf(data.length));
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,long totalDataLen, long longSampleRate, int channels,long byteRate) throws IOException
    {
        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);

        int mnMilliSecond=1000;
        int mnExitDelay=1;
        int delay=mnExitDelay*mnMilliSecond;
        timer2=new CountDownTimer(delay,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                readFile();
            }
        };
        timer2.start();

    }

    private void readFile(){
        if(recorder !=null){
            recorder.stop();
            recorder.release();
            recorder = null;
        }
        File file = new File(getFilename());
        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/*"), file);
        MultipartBody.Part uploadFile = MultipartBody.Part.createFormData("file", getFilename(), requestFile);

        Call<WavFile> call1 = apiInterface.registerFile(uploadFile);
        call1.enqueue(new Callback<WavFile>() {
            @Override
            public void onResponse(Call<WavFile> call, Response<WavFile> response) {
                final ImageView start_stop=findViewById(R.id.start_stop);
                Call<Object> call2 = apiInterface.getResult();
                call2.enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        Log.i("success2",response.body().toString());
                        flag=false;
                        count++;
                        final TextView main_text=findViewById(R.id.main_text);
                        String result=response.body().toString();
                        final String result_substring=result.substring(result.indexOf('=')+1,result.indexOf('=')+2);
                        Log.e("result_substring",result_substring);
                        switch (result_substring){
                            case "1":
                                main_text.setText("배가 아픈 것 같아요");
                                break;
                            case "2":
                                main_text.setText("트름이\n필요해보여요");
                                break;
                            case "3":
                                main_text.setText("어딘가\n불편해보이네요");
                                break;
                            case "4":
                                main_text.setText("배가 고픈가봐요");
                                break;
                            case "5":
                                main_text.setText("피곤한 것 같아요");
                                break;
                        }
                    }

                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        Log.i("fail2",t.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(Call<WavFile> call, Throwable t) {
                Log.i("fail",t.getMessage());
            }
        });
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.start_stop: {
                    flag = true;
                    final ImageView start_stop = findViewById(R.id.start_stop);
                    final TextView main_text = findViewById(R.id.main_text);
                    if (count % 2 == 0) {
                        start_stop.setImageResource(R.drawable.pause);
                        startRecording();
                        main_text.setText("듣고있어요");
                        int mnMilliSecond = 1000;
                        int mnExitDelay = 7;
                        int delay = mnExitDelay * mnMilliSecond;
                        value = 7;
                        count++;
                        timer = new CountDownTimer(delay, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                value--;
                                flag = true;
                                Log.i("value", String.valueOf(value));
                            }

                            @Override
                            public void onFinish() {
                                Log.i("value", "타이머 종료");
                                if (count % 2 != 0) {
                                    stopRecording();
                                    start_stop.setImageResource(R.drawable.start);
                                    main_text.setText("인식중");
                                }
                            }
                        };
                        timer.start();
                    } else {
                        start_stop.setImageResource(R.drawable.start);
                        stopRecording();
                        main_text.setText("인식중");
                    }

                    break;
                }
            }
        }
    };
}