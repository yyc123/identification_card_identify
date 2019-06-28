package ylsoft.com.identification_card_identify;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;

import com.baidu.ocr.demo.IDCardActivity;
import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.IDCardParams;
import com.baidu.ocr.sdk.model.IDCardResult;
import com.baidu.ocr.sdk.utils.ExifUtil;
import com.baidu.ocr.ui.camera.CameraActivity;
import com.baidu.ocr.ui.camera.PermissionCallback;
import com.baidu.ocr.sdk.model.BankCardParams;
import com.baidu.ocr.sdk.model.BankCardResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import androidx.core.app.ActivityCompat;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;


/** IdentificationCardIdentifyPlugin */
public class IdentificationCardIdentifyPlugin implements MethodCallHandler{

  private static boolean hasGotToken = false;
  private static final int REQUEST_CODE_PICK_IMAGE_FRONT = 201;
  private static final int REQUEST_CODE_PICK_IMAGE_BACK = 202;
  private static final int REQUEST_CODE_PICK_IMAGE_BANK = 203;

  private static final int REQUEST_CODE_CAMERA = 102;
  private static final int PERMISSIONS_EXTERNAL_STORAGE = 801;
  private static Activity mContext;
  private static AlertDialog.Builder alertDialog;
  private static Result mResult;
  private static final String SAVE_REAL_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
  private static String picPath;
  private static String type;

         public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "identification_card_identify");
    channel.setMethodCallHandler(new IdentificationCardIdentifyPlugin());
    mContext = registrar.activity();
    registrar.addActivityResultListener(listener);
    registrar.addRequestPermissionsResultListener(permissionsResultListener);
  }

  private boolean checkGalleryPermission() {
    int ret = ActivityCompat.checkSelfPermission(mContext, Manifest.permission
            .WRITE_EXTERNAL_STORAGE);
    if (ret != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(mContext,
              new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
              PERMISSIONS_EXTERNAL_STORAGE);
      return false;
    }
    return true;
  }

//  interface ServiceListener {
//    public void onResult(String result);
//  }

    public IdentificationCardIdentifyPlugin(){

    }


    static PluginRegistry.RequestPermissionsResultListener permissionsResultListener = new PluginRegistry.RequestPermissionsResultListener() {
      @Override
      public boolean onRequestPermissionsResult(int i, String[] strings, int[] ints) {
        switch (i) {
          case PERMISSIONS_EXTERNAL_STORAGE:
            if (ints.length > 0 && ints[0] == PackageManager.PERMISSION_GRANTED) {
              if("CardTypeIdCardFont".equals(type)){
                if(checkTokenStatus()){
                  getCardFront();
                }
              }else if("CardTypeIdCardBack".equals(type)){
                if(checkTokenStatus()){
                  getCardBack();
                }
              }else if("CardTypeBankCard".equals(type)){
                if(checkTokenStatus()){
                  getBankCard();
                }
              }
            } else {
              Toast.makeText(mContext.getApplicationContext(), R.string.storage_permission_required, Toast.LENGTH_LONG)
                      .show();
            }
            break;

        }
        return false;
      }
    };

  static PluginRegistry.ActivityResultListener listener = new PluginRegistry.ActivityResultListener() {
    @Override
    public boolean onActivityResult(int i, int i1, Intent intent) {
//      if (i == REQUEST_CODE_PICK_IMAGE_FRONT && i1 == Activity.RESULT_OK) {
//        Uri uri = intent.getData();
//        String filePath = getRealPathFromURI(uri);
//        recIDCard(IDCardParams.ID_CARD_SIDE_FRONT, filePath);
//      }
//
//      if (i == REQUEST_CODE_PICK_IMAGE_BACK && i1 == Activity.RESULT_OK) {
//        Uri uri = intent.getData();
//        String filePath = getRealPathFromURI(uri);
//        recIDCard(IDCardParams.ID_CARD_SIDE_BACK, filePath);
//      }
//      if (i == REQUEST_CODE_PICK_IMAGE_BACK && i1 == Activity.RESULT_OK) {
//        Uri uri = intent.getData();
//        String filePath = getRealPathFromURI(uri);
//        recIDCard(IDCardParams.ID_CARD_SIDE_BACK, filePath);
//      }






      if (i == REQUEST_CODE_CAMERA && i1 == Activity.RESULT_OK) {

        if (intent != null) {


          String contentType = intent.getStringExtra(CameraActivity.KEY_CONTENT_TYPE);
          String filePath = FileUtil.getSaveFile(mContext.getApplicationContext()).getAbsolutePath();

          if (!TextUtils.isEmpty(contentType)) {
            if (CameraActivity.CONTENT_TYPE_ID_CARD_FRONT.equals(contentType)) {
              recIDCard(IDCardParams.ID_CARD_SIDE_FRONT, filePath);
            } else if (CameraActivity.CONTENT_TYPE_ID_CARD_BACK.equals(contentType)) {
              recIDCard(IDCardParams.ID_CARD_SIDE_BACK, filePath);
            }  else if (CameraActivity.CONTENT_TYPE_BANK_CARD.equals(contentType)) {
//              recIDCard(IDCardParams.ID_CARD_b, filePath);
              recBankCard(mContext,filePath);
            }
          }
        }
      }
      return false;
    }
  };

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if(call.method.equals("Initialize")){
      initAccessTokenWithAkSk(call.argument("AK").toString(),call.argument("SK").toString());
    }else if(call.method.equals("IDCard_identify")){
      mResult = result;
      type = call.arguments.toString();
      if(checkGalleryPermission()){
        if("CardTypeIdCardFont".equals(type)){
          if(checkTokenStatus()){
            getCardFront();
          }
        }else if("CardTypeIdCardBack".equals(type)){
          if(checkTokenStatus()){
            getCardBack();

          }
        }else if("CardTypeBankCard".equals(type)){
          if(checkTokenStatus()){
            getBankCard();

          }
        }
      }else{
        return;
      }

    }else {
      result.notImplemented();
    }
  }

  /**
   * 用明文ak，sk初始化
   */
  private void initAccessTokenWithAkSk(String ak,String sk) {
    OCR.getInstance(mContext).initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
      @Override
      public void onResult(AccessToken result) {
        String token = result.getAccessToken();
        hasGotToken = true;
      }

      @Override
      public void onError(OCRError error) {
        error.printStackTrace();
        alertText("AK，SK方式获取token失败", error.getMessage());
      }
    }, mContext.getApplicationContext(),  ak, sk);
  }

  private static boolean checkTokenStatus() {
    if (!hasGotToken) {
      Toast.makeText(mContext.getApplicationContext(), "token还未成功获取", Toast.LENGTH_LONG).show();
    }
    return hasGotToken;
  }
  // 身份证正面扫描
  private static void getCardFront(){
    Intent intent = new Intent(mContext, CameraActivity.class);
    intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH,
            com.baidu.ocr.demo.FileUtil.getSaveFile(mContext.getApplication()).getAbsolutePath());
    intent.putExtra(CameraActivity.KEY_CONTENT_TYPE, CameraActivity.CONTENT_TYPE_ID_CARD_FRONT);
    mContext.startActivityForResult(intent, REQUEST_CODE_CAMERA);
  }
  // 身份证反面扫描
  private static void getCardBack(){
    Intent intent = new Intent(mContext, CameraActivity.class);
    intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH,
            com.baidu.ocr.demo.FileUtil.getSaveFile(mContext.getApplication()).getAbsolutePath());
    intent.putExtra(CameraActivity.KEY_CONTENT_TYPE, CameraActivity.CONTENT_TYPE_ID_CARD_BACK);
    mContext.startActivityForResult(intent, REQUEST_CODE_CAMERA);
  }
  // 银行卡扫描
  private static void getBankCard(){
    Intent intent = new Intent(mContext, CameraActivity.class);

    intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH,
            com.baidu.ocr.demo.FileUtil.getSaveFile(mContext.getApplication()).getAbsolutePath());
    intent.putExtra(CameraActivity.KEY_CONTENT_TYPE, CameraActivity.CONTENT_TYPE_BANK_CARD);
    mContext.startActivityForResult(intent, REQUEST_CODE_CAMERA);
  }


  private static String getRealPathFromURI(Uri contentURI) {
    String result;
    Cursor cursor = mContext.getContentResolver().query(contentURI, null, null, null, null);
    if (cursor == null) { // Source is Dropbox or other similar local file path
      result = contentURI.getPath();
    } else {
      cursor.moveToFirst();
      int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
      result = cursor.getString(idx);
      cursor.close();
    }
    return result;
  }

  private static void alertText(final String title, final String message) {
    if(alertDialog == null){
      alertDialog = new AlertDialog.Builder(mContext);
    }
    mContext.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        alertDialog.setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
      }
    });
  }

  private static void recBankCard(Context ctx, String filePath) {
    BankCardParams param = new BankCardParams();
    param.setImageFile(new File(filePath));
    savePic(param.getImageFile());

    OCR.getInstance(ctx).recognizeBankCard(param, new OnResultListener<BankCardResult>() {
      @Override
      public void onResult(BankCardResult result) {
        if (result != null){
          Map results = new HashMap();
          results.put("image",picPath);
          Map<String,String> resV = new HashMap<>();

          if (result.getBankCardNumber() == null ||result.getBankCardType() == null ||result.getBankName() ==null){
            resV.put("error","失败");
            resV.put("code","-1");
            mResult.success(resV);

            return;
          }
          String res = String.format("卡号：%s\n类型：%s\n发卡行：%s",
                  result.getBankCardNumber(),
                  result.getBankCardType().name(),
                  result.getBankName());

//          results.put("result",res);
          results.put("result",resV);

          resV.put("卡号",result.getBankCardNumber());
          resV.put("类型",result.getBankCardType().name());
          resV.put("发卡行",result.getBankName());
          Log.v("打印结果:",results.toString());

          mResult.success(results);
        }
      }

      @Override
      public void onError(OCRError error) {
        mResult.error(error.getMessage(),"","");
        alertText("", error.getMessage());
      }
    });
  }


  private static void recIDCard(final String idCardSide, String filePath) {
    IDCardParams param = new IDCardParams();
    param.setImageFile(new File(filePath));
    // 设置身份证正反面
    param.setIdCardSide(idCardSide);
    // 设置方向检测
    param.setDetectDirection(true);
    // 设置图像参数压缩质量0-100, 越大图像质量越好但是请求时间越长。 不设置则默认值为20
    param.setImageQuality(100);

    savePic(param.getImageFile());
    OCR.getInstance(mContext).recognizeIDCard(param, new OnResultListener<IDCardResult>() {
      @Override
      public void onResult(IDCardResult result) {
        if (result != null) {
          Map results = new HashMap();
          results.put("image",picPath);
          Map<String,String> resV = new HashMap<>();
          if (idCardSide.equals(IDCardParams.ID_CARD_SIDE_FRONT)){
            if (result.getName() == null ||result.getAddress() == null ||result.getIdNumber() ==null || result.getBirthday() == null){
              resV.put("error","失败");
              resV.put("code","-1");
              mResult.success(resV);

              return;
            }
            resV.put("姓名",result.getName().getWords());
            resV.put("户籍地",result.getAddress().getWords());
            resV.put("身份证号",result.getIdNumber().getWords());
            resV.put("生日",result.getBirthday().getWords());

          }else  if (idCardSide.equals(IDCardParams.ID_CARD_SIDE_BACK)){
            if (result.getIssueAuthority() == null ||result.getSignDate() == null ||result.getExpiryDate() ==null){
              resV.put("error","失败");
              resV.put("code","-1");
              mResult.success(resV);

              return;
            }
            resV.put("签发机关",result.getIssueAuthority().getWords());
            resV.put("签发日期",result.getSignDate().getWords());
           resV.put("有效期",result.getExpiryDate().getWords());

          }
          results.put("result",resV);
          mResult.success(results);
        }
      }

      @Override
      public void onError(OCRError error) {
        mResult.error(error.getMessage(),"","");
        alertText("", error.getMessage());
      }

    });
  }

  public static void savePic(File param) {
    // 首先保存图片
    File imageFile = param;
    File saveFile = new File(SAVE_REAL_PATH, "hsh");
    if(!saveFile.exists()){
      saveFile.mkdir();
    }
    String fileName = String.valueOf(System.currentTimeMillis())+".jpg";
    File saveImg = new File(saveFile, fileName);
    resize(imageFile.getAbsolutePath(), saveImg.getAbsolutePath(), 1280, 1280, 80);
    // 其次把文件插入到系统图库
    try {
      MediaStore.Images.Media.insertImage(mContext.getContentResolver(),
              saveImg.getAbsolutePath(), saveImg.getName(), null);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    // 最后通知图库更新
    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    Uri uri = Uri.fromFile(saveImg);
    intent.setData(uri);
    mContext.sendBroadcast(intent);
    picPath = saveImg.getAbsolutePath();
  }

  public static void resize(String inputPath, String outputPath, int dstWidth, int dstHeight, int quality) {
    try {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(inputPath, options);
      int inWidth = options.outWidth;
      int inHeight = options.outHeight;
      Matrix m = new Matrix();
      ExifInterface exif = new ExifInterface(inputPath);
      int rotation = exif.getAttributeInt("Orientation", 1);
      if (rotation != 0) {
        m.preRotate((float)ExifUtil.exifToDegrees(rotation));
      }

      int maxPreviewImageSize = Math.max(dstWidth, dstHeight);
      int size = Math.min(options.outWidth, options.outHeight);
      size = Math.min(size, maxPreviewImageSize);
      options = new BitmapFactory.Options();
      options.inSampleSize = calculateInSampleSize(options, size, size);
      options.inScaled = true;
      options.inDensity = options.outWidth;
      options.inTargetDensity = size * options.inSampleSize;
      Bitmap roughBitmap = BitmapFactory.decodeFile(inputPath, options);
      FileOutputStream out = new FileOutputStream(outputPath);
      try {
        roughBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
      } catch (Exception var25) {
        var25.printStackTrace();
      } finally {
        try {
          out.close();
        } catch (Exception var24) {
          var24.printStackTrace();
        }

      }
    } catch (IOException var27) {
      var27.printStackTrace();
    }

  }

  public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
    int height = options.outHeight;
    int width = options.outWidth;
    int inSampleSize = 1;
    if (height > reqHeight || width > reqWidth) {
      int halfHeight = height / 2;

      for(int halfWidth = width / 2; halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth; inSampleSize *= 2) {
        ;
      }
    }

    return inSampleSize;
  }

  }
