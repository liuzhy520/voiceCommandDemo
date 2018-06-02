package liuzhy.voicecmmmanddemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.RequestListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.util.FileDownloadListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.speech.WakeuperResult;
import com.iflytek.speech.aidl.IWakeuper;
import com.iflytek.sunflower.FlowerCollector;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import liuzhy.voicecmmmanddemo.util.JsonParser;

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
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=5b12269d");
        setWakeup();
        setIat();
    }

    private String getResource() {
        final String resPath = ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "ivw/"+getString(R.string.app_id)+".jet");
//        final String resPath = ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "asr/common.jet");
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
            mIvw.setParameter( SpeechConstant.NOTIFY_RECORD_DATA, "1" );

            // 启动唤醒
            mIvw.startListening(mWakeuperListener);
        }
    }

    ////////// wakeup //////////
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
                setSwitches(object.optString("id"));

            } catch (JSONException e) {
                resultString = "结果解析出错";
                e.printStackTrace();
            }

//            Toast.makeText(MainActivityActivity.this, "蕉迟但到！", Toast.LENGTH_SHORT).show();
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
        public void onEvent(int eventType, int i1, int i2, Bundle obj) {
            switch( eventType ){
                // EVENT_RECORD_DATA 事件仅在 NOTIFY_RECORD_DATA 参数值为 真 时返回
                case SpeechEvent.EVENT_RECORD_DATA:
                     audio = obj.getByteArray( SpeechEvent.KEY_EVENT_RECORD_DATA );
//                    Log.i( TAG, "ivw audio length: "+audio.length );
                    if(!isLoading){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                isLoading = true;
                                if(!mIat.isListening()){
                                    mIat.startListening(mRecognizerListener);
                                }
                                mIat.writeAudio(audio, 0, audio.length);
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mIat.stopListening();
                                isLoading = false;
                            }
                        }
                        );
                    }

//                    mIat.stopListening();
                    break;
            }
        }

        @Override
        public void onBeginOfSpeech() {
            Log.e("wakeup","begin");
        }


        @Override
        public void onVolumeChanged(int volume) {

        }

    };

    private void setSwitches(String id){
        TextView light = findViewById(R.id.light_tv);
        TextView ac = findViewById(R.id.ac_tv);
        switch (id){
            case "4":
                showTip("蕉迟但到！");
                break;
            case "2":
                showTip("客厅开灯！");
                light.setText("客厅主灯 \n on");
                light.setTextColor(getColor(R.color._eeeeee));
                light.setBackgroundColor(getColor(R.color.colorPrimary));
                break;
            case "0":
                showTip("客厅关灯！");
                light.setText("客厅主灯 \n off");
                light.setTextColor(getColor(R.color.colorPrimary));
                light.setBackgroundColor(getColor(R.color._eeeeee));
                break;
            case "3":
                showTip("客厅开风扇！");
                ac.setText("客厅风扇 \n on");
                ac.setTextColor(getColor(R.color._eeeeee));
                ac.setBackgroundColor(getColor(R.color.colorPrimary));
                break;
            case "1":
                showTip("客厅关风扇！");
                ac.setText("客厅风扇 \n off");
                ac.setTextColor(getColor(R.color.colorPrimary));
                ac.setBackgroundColor(getColor(R.color._eeeeee));
                break;




        }
    }

    ///////// wakeup //////////

    boolean isLoading = false;
    byte[] audio;
    //////// speech ///////////
    private boolean mTranslateEnable = false;
    // 语音听写对象
    private SpeechRecognizer mIat;
    int ret = 0; // 函数调用返回值
    private void setIat(){
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
        mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
        // 也可以像以下这样直接设置音频文件路径识别（要求设置文件在sdcard上的全路径）：
        // mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-2");
        // mIat.setParameter(SpeechConstant.ASR_SOURCE_PATH, "sdcard/XXX/XXX.pcm");
        ret = mIat.startListening(mRecognizerListener);
    }

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            if(mTranslateEnable && error.getErrorCode() == 14002) {
                showTip( error.getPlainDescription(true)+"\n请确认是否已开通翻译功能" );
            } else {
                showTip(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            if( mTranslateEnable ){
                printTransResult( results );
            }else{
                printResult(results);
            }
                showTip(resultString + " continue ");
            if (isLast) {
                // TODO 最后的结果
                showTip(resultString);
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据：" + data.length);

        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code);
            }
        }
    };

    private void showTip(String content){
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
    }

    private String TAG = "demo";
    private void printResult (RecognizerResult content){
        Log.e("result", content.getResultString() + "");
    }

    private void printTransResult (RecognizerResult results) {
        String trans  = JsonParser.parseTransResult(results.getResultString(),"dst");
        String oris = JsonParser.parseTransResult(results.getResultString(),"src");

        if( TextUtils.isEmpty(trans)||TextUtils.isEmpty(oris) ){
            showTip( "解析结果失败，请确认是否已开通翻译功能。" );
        }else{
            showTip( "原始语言:\n"+oris+"\n目标语言:\n"+trans );
        }

    }
    /////// speech ///////////


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIat.stopListening();
        mIvw.stopListening();
    }

    @Override
    protected void onResume() {
        // 开放统计 移动数据统计分析
        FlowerCollector.onResume(this);
        FlowerCollector.onPageStart(TAG);
        super.onResume();
    }

    @Override
    protected void onPause() {
        // 开放统计 移动数据统计分析
        FlowerCollector.onPageEnd(TAG);
        FlowerCollector.onPause(this);
        super.onPause();
    }
}
