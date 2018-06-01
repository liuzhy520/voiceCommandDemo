package liuzhy.voicecmmmanddemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.RequestListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.util.FileDownloadListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.speech.WakeuperResult;
import com.iflytek.speech.aidl.IWakeuper;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class MainActivityActivity extends AppCompatActivity {
    // 语音唤醒对象
    private VoiceWakeuper mIvw;

    private final static int MAX = 3000;
    private final static int MIN = 0;
    private int curThresh = 1450;
    private String threshStr = "门限值：";
    private String keep_alive = "1";
    private String ivwNetMode = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int permission = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(permission!= PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[] {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.LOCATION_HARDWARE,Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.WRITE_SETTINGS,Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS},0x0010);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=5b0eadc9");
        setWakeup();
    }

    private String getResource() {
        final String resPath = ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "ivw/"+getString(R.string.app_id)+".jet");
        Log.e( "Demo", "resPath: "+resPath );
        return resPath;
    }

    private void setWakeup(){
        mIvw = VoiceWakeuper.createWakeuper(this, null);
        mIvw = VoiceWakeuper.getWakeuper();
        if(mIvw != null) {


            // 清空参数
            mIvw.setParameter(SpeechConstant.PARAMS, null);
            // 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
            mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:"+ curThresh);
            // 设置唤醒模式
            mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
            // 设置持续进行唤醒
            mIvw.setParameter(SpeechConstant.KEEP_ALIVE, keep_alive);
            // 设置闭环优化网络模式
            mIvw.setParameter(SpeechConstant.IVW_NET_MODE, ivwNetMode);
            // 设置唤醒资源路径
            mIvw.setParameter(SpeechConstant.IVW_RES_PATH, getResource());
            // 设置唤醒录音保存路径，保存最近一分钟的音频
            mIvw.setParameter( SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath()+"/msc/ivw.wav" );
            mIvw.setParameter( SpeechConstant.AUDIO_FORMAT, "wav" );
            // 如有需要，设置 NOTIFY_RECORD_DATA 以实时通过 onEvent 返回录音音频流字节
            //mIvw.setParameter( SpeechConstant.NOTIFY_RECORD_DATA, "1" );

            // 启动唤醒
            mIvw.startListening(mWakeuperListener);
        }
    }
    
    // 唤醒结果内容
    private String resultString;
    private WakeuperListener mWakeuperListener = new WakeuperListener() {

        @Override
        public void onResult(com.iflytek.cloud.WakeuperResult result) {
            Log.d("wakeup", "onResult");
            if (!"1".equalsIgnoreCase(keep_alive)) {
                Toast.makeText(MainActivityActivity.this, "keep alive", Toast.LENGTH_SHORT).show();
            }
            try {
                String text = result.getResultString();
                JSONObject object;
                object = new JSONObject(text);
                StringBuffer buffer = new StringBuffer();
                buffer.append("【RAW】 " + text);
                buffer.append("\n");
                buffer.append("【操作类型】" + object.optString("sst"));
                buffer.append("\n");
                buffer.append("【唤醒词id】" + object.optString("id"));
                buffer.append("\n");
                buffer.append("【得分】" + object.optString("score"));
                buffer.append("\n");
                buffer.append("【前端点】" + object.optString("bos"));
                buffer.append("\n");
                buffer.append("【尾端点】" + object.optString("eos"));
                resultString = buffer.toString();


            } catch (JSONException e) {
                resultString = "结果解析出错";
                e.printStackTrace();
            }
            Toast.makeText(MainActivityActivity.this, "蕉迟但到！", Toast.LENGTH_SHORT).show();
            //Toast.makeText(MainActivityActivity.this, resultString, Toast.LENGTH_SHORT).show();
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
            wakeLock.acquire();

        }

        @Override
        public void onError(SpeechError error) {
            Toast.makeText(MainActivityActivity.this, error + "", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }

        @Override
        public void onBeginOfSpeech() {
        }


        @Override
        public void onVolumeChanged(int volume) {

        }

    };

}
