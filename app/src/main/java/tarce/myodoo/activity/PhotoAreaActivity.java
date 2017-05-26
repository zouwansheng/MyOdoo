package tarce.myodoo.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.model.CropOptions;
import com.jph.takephoto.model.InvokeParam;
import com.jph.takephoto.model.TContextWrap;
import com.jph.takephoto.model.TImage;
import com.jph.takephoto.model.TResult;
import com.jph.takephoto.permission.PermissionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Response;
import tarce.api.MyCallback;
import tarce.api.RetrofitClient;
import tarce.api.api.InventoryApi;
import tarce.model.inventory.AreaMessageBean;
import tarce.myodoo.R;
import tarce.myodoo.adapter.processproduct.AreaMessageAdapter;
import tarce.myodoo.uiutil.TakePhotoDialog;
import tarce.support.ToastUtils;

/**
 * Created by rose.zou on 2017/5/25.
 * 用于产品的位置信息，拍照
 */

public class PhotoAreaActivity extends TakePhotoActivity {

    @InjectView(R.id.tv_one)
    TextView tvOne;
    @InjectView(R.id.tv_take_photo)
    TextView tvTakePhoto;
    @InjectView(R.id.edit_area_message)
    EditText editAreaMessage;
    @InjectView(R.id.recycler_area)
    RecyclerView recyclerArea;
    @InjectView(R.id.image_show_photo)
    ImageView imageShowPhoto;
    @InjectView(R.id.relative_click_use)
    RelativeLayout relativeClickUse;
    private InvokeParam invokeParam;
    private TakePhoto takePhoto;
    private InventoryApi inventoryApi;
    private AreaMessageAdapter adapter;
    private List<AreaMessageBean.ResultBean.ResDataBean> res_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_area);
        ButterKnife.inject(this);

        takePhoto = getTakePhoto();
        takePhoto.onCreate(savedInstanceState);
        setTitle("物料位置信息");
        // setRecyclerview(recyclerArea);
        recyclerArea.setLayoutManager(new LinearLayoutManager(PhotoAreaActivity.this));
        recyclerArea.addItemDecoration(new DividerItemDecoration(PhotoAreaActivity.this,
                DividerItemDecoration.VERTICAL));
        editListener();
    }

    /**
     * edittext的搜索监听
     */
    private void editListener() {
        editAreaMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    res_data = new ArrayList<>();
                    inventoryApi = RetrofitClient.getInstance(PhotoAreaActivity.this).create(InventoryApi.class);
                    HashMap<Object, Object> hashMap = new HashMap<>();
                    hashMap.put("condition", editAreaMessage.getText().toString());
                    Call<AreaMessageBean> areaMessage = inventoryApi.getAreaMessage(hashMap);
                    areaMessage.enqueue(new MyCallback<AreaMessageBean>() {
                        @Override
                        public void onResponse(Call<AreaMessageBean> call, Response<AreaMessageBean> response) {
                            if (response.body() == null) return;
                            res_data = response.body().getResult().getRes_data();
                            adapter = new AreaMessageAdapter(R.layout.adapter_area_message, res_data);
                            recyclerArea.setAdapter(adapter);
                            adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                                @Override
                                public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                                    editAreaMessage.setText(res_data.get(position).getArea_name());
                                }
                            });
                        }

                        @Override
                        public void onFailure(Call<AreaMessageBean> call, Throwable t) {
                            super.onFailure(call, t);
                        }
                    });
                    return true;
                }
                return false;
            }
        });
    }

    @OnClick(R.id.image_show_photo)
    void takePhoto(View view) {
        new TakePhotoDialog(PhotoAreaActivity.this)
                .setTakephoto(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        File file = new File(Environment.getExternalStorageDirectory(), "/linkloving/" + System.currentTimeMillis() + ".jpg");
                        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
                        Uri imageUri = Uri.fromFile(file);
                        CropOptions cropOptions = new CropOptions.Builder().setAspectX(1).setAspectY(1).setWithOwnCrop(true).create();
                        takePhoto.onPickFromCaptureWithCrop(imageUri, cropOptions);
                    }
                })
                .setSelectalbum(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takePhoto.onPickFromGallery();
                    }
                })
                .setCancel()
                .show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        takePhoto.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        takePhoto.onSaveInstanceState(outState);
    }

    @Override
    public void takeSuccess(TResult result) {
        super.takeSuccess(result);
        Bitmap bitmap = BitmapFactory.decodeFile(result.getImage().getCompressPath());
        ToastUtils.showCommonToast(PhotoAreaActivity.this, result.getImage().getCompressPath());
        TImage image = result.getImage();
        Glide.with(PhotoAreaActivity.this).load(result.getImage()).into(imageShowPhoto);
       // ToastUtils.showCommonToast(PhotoAreaActivity.this, "????????");
    }

    @Override
    public PermissionManager.TPermissionType invoke(InvokeParam invokeParam) {
        PermissionManager.TPermissionType type = PermissionManager.checkPermission(TContextWrap.of(this), invokeParam.getMethod());
        if (PermissionManager.TPermissionType.WAIT.equals(type)) {
            this.invokeParam = invokeParam;
        }
        return type;
    }

    /**
     * 空白区域点击事件
     * */
    @OnClick(R.id.recycler_area)
    void clickRela(View view){
        res_data = null;
        adapter.notifyDataSetChanged();
    }
    /**
     * 权限问题
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //以下代码为处理Android6.0、7.0动态权限所需
        PermissionManager.TPermissionType type = PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.handlePermissionsResult(this, type, invokeParam, this);
    }


}