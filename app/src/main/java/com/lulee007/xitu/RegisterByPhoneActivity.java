package com.lulee007.xitu;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.EditText;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.lulee007.xitu.base.XTBaseActivity;
import com.lulee007.xitu.models.LeanCloudError;
import com.lulee007.xitu.presenter.RegisterByPhonePresenter;
import com.lulee007.xitu.view.IRegisterByPhoneView;
import com.mikepenz.materialize.MaterializeBuilder;

import java.util.concurrent.TimeUnit;

import cn.pedant.SweetAlert.SweetAlertDialog;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func3;


public class RegisterByPhoneActivity extends XTBaseActivity implements IRegisterByPhoneView {


    RegisterByPhonePresenter registerByPhonePresenter;
    private Button registerPhone;
    private SweetAlertDialog sweetAlertDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_by_phone);
        new MaterializeBuilder().withActivity(this).build();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.register_by_phone);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final TextInputLayout phoneInputLayout = (TextInputLayout) findViewById(R.id.phone_InputLayout);
        final EditText phoneNumber = (EditText) findViewById(R.id.phone);
        final TextInputLayout userNameInputLayout = (TextInputLayout) findViewById(R.id.user_name_InputLayout);
        final EditText userName = (EditText) findViewById(R.id.user_name);
        final TextInputLayout pwdInputLayout = (TextInputLayout) findViewById(R.id.pwd_InputLayout);
        final EditText pwd = (EditText) findViewById(R.id.pwd);
        registerPhone = (Button) findViewById(R.id.register_phone);

        registerByPhonePresenter = new RegisterByPhonePresenter(this);

        Observable<Boolean> phoneNumberOb = RxTextView.textChanges(phoneNumber).skip(1)
                .map(new Func1<CharSequence, Boolean>() {
                    @Override
                    public Boolean call(CharSequence charSequence) {
                        return charSequence.length() == 11;
                    }
                })
                .distinctUntilChanged()
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        phoneInputLayout.setError(aBoolean ? "" : "手机号不正确");
                    }
                });

        Observable<Boolean> userNameOb = RxTextView.textChanges(userName).skip(1)
                .map(new Func1<CharSequence, Boolean>() {
                    @Override
                    public Boolean call(CharSequence charSequence) {
                        return charSequence.length() > 0 && charSequence.length() < 12;
                    }
                })
                .distinctUntilChanged()
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        userNameInputLayout.setError(aBoolean ? "" : "用户名不正确");
                    }
                });

        Observable<Boolean> pwdOb = RxTextView.textChanges(pwd).skip(1)
                .map(new Func1<CharSequence, Boolean>() {
                    @Override
                    public Boolean call(CharSequence charSequence) {
                        return charSequence.length() >= 6 && charSequence.length() < 20;
                    }
                })
                .distinctUntilChanged()
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        pwdInputLayout.setError(aBoolean ? "" : "密码不正确");

                    }
                });

        Subscription validSubscription = Observable
                .combineLatest(phoneNumberOb, userNameOb, pwdOb, new Func3<Boolean, Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean, Boolean aBoolean2, Boolean aBoolean3) {
                        return aBoolean && aBoolean2 && aBoolean3;
                    }
                })
                .distinctUntilChanged()
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        registerPhone.setEnabled(aBoolean);
                    }
                });
        registerByPhonePresenter.addSubscription(validSubscription);

        RxView.clicks(registerPhone)
                .throttleFirst(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        registerPhone.setEnabled(false);
                        registerByPhonePresenter.registerByPhone(phoneNumber.getText().toString(),
                                userName.getText().toString(),
                                pwd.getText().toString());
                        sweetAlertDialog = new SweetAlertDialog(RegisterByPhoneActivity.this, SweetAlertDialog.PROGRESS_TYPE);
                        sweetAlertDialog.setTitleText("正在注册中...");
                        sweetAlertDialog.setCancelable(false);
                        sweetAlertDialog.show();
                    }
                });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        registerByPhonePresenter.unSubscribeAll();
        registerByPhonePresenter = null;
    }

    @Override
    public void onRegisterSuccess(final String phone) {
        registerPhone.setEnabled(true);
        sweetAlertDialog
                .setTitleText("注册成功")
                .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
        Observable.timer(1000, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .doOnNext(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        if (sweetAlertDialog.isShowing())
                            sweetAlertDialog.dismissWithAnimation();
                    }
                })
                .delay(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        startActivity(VerifyPhoneActivity.class);
                    }
                });
    }

    @Override
    public void onRegisterError(LeanCloudError error) {
        if (error != null) {
            String toastText = null;
            switch (error.getCode()) {
                case 100:
                    toastText = "服务器异常";
                    break;
                case 124:
                    toastText = "连接服务器超时";
                    break;
                case 127:
                    toastText = "手机号码无效";
                    break;
                case 202:
                    toastText = "用户名已被占用";
                    break;
                case 214:
                    toastText = "手机号码已被注册";
                    break;
                default:
                    toastText = "未知错误";
                    break;
            }
            sweetAlertDialog.setTitleText(toastText)
                    .setConfirmText("确认")
                    .setConfirmClickListener(null)
                    .changeAlertType(SweetAlertDialog.ERROR_TYPE);
        }
        registerPhone.setEnabled(true);
    }


}
