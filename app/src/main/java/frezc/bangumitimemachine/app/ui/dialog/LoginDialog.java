package frezc.bangumitimemachine.app.ui.dialog;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import frezc.bangumitimemachine.app.MyApplication;
import frezc.bangumitimemachine.app.R;
import frezc.bangumitimemachine.app.db.DB;
import frezc.bangumitimemachine.app.entity.BaseAuth;
import frezc.bangumitimemachine.app.entity.LoginUser;
import frezc.bangumitimemachine.app.network.http.BasicAuth;
import frezc.bangumitimemachine.app.network.http.NetWorkTool;

/**
 * Created by freeze on 2015/4/26.
 */
public class LoginDialog extends DialogFragment
    implements View.OnClickListener, Response.Listener<LoginUser>
                ,Response.ErrorListener{
    private EditText etEmail,etPassword;
    private TextView tvLoginFail;
    private Button loginButton;
    private ProgressBar loginProgress;
    private CheckBox autoLogin;

    private NetWorkTool netWorkTool;
    private BasicAuth basicAuth;

    private String username;
    private String password;

    private OnLoginSuccessListener onLoginSuccessListener;

    public void setOnLoginSuccessListener(OnLoginSuccessListener onLoginSuccessListener) {
        this.onLoginSuccessListener = onLoginSuccessListener;
    }

    public static LoginDialog newInstance(BaseAuth auth){
        LoginDialog loginDialog = new LoginDialog();
        if(auth != null) {
            Bundle bundle = new Bundle();
            bundle.putString("username", auth.getUsername());
            bundle.putString("password", auth.getPassword());
            loginDialog.setArguments(bundle);
        }
        return loginDialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setStyle(STYLE_NO_TITLE,0);
        netWorkTool = NetWorkTool.getInstance(getActivity());
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_login,container, false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        etEmail = (EditText) v.findViewById(R.id.login_email);
        etPassword = (EditText) v.findViewById(R.id.login_password);
        etEmail.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        tvLoginFail = (TextView) v.findViewById(R.id.login_fail);
        loginButton = (Button) v.findViewById(R.id.login_comfirm);
        loginButton.setOnClickListener(this);
        loginProgress = (ProgressBar) v.findViewById(R.id.login_wait);
        autoLogin = (CheckBox) v.findViewById(R.id.login_auto);
        
        Bundle bundle = getArguments();
        if(bundle != null){
            etEmail.setText(bundle.getString("username"));
            etPassword.setText(bundle.getString("password"));
            autoLogin.setChecked(true);
            loginButton.performClick();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.login_comfirm:
                if(!checkAvailable()){
                    setError("用户名或密码格式不正确");
                    return;
                }

                if(basicAuth == null){
                    basicAuth = new BasicAuth(username,password, this, this);
                }else {
                    basicAuth.setUsernameAndPassword(username,password);
                }

                basicAuth.sendRequest(netWorkTool);

                setWait();
                break;
        }
    }

    //进入等待状态
    private void setWait() {
        loginButton.setVisibility(View.INVISIBLE);
        loginProgress.setVisibility(View.VISIBLE);
    }

    //本地简单检查合法性
    private boolean checkAvailable() {
        username = etEmail.getText().toString();
        password = etPassword.getText().toString();
        if(username == null || password == null || username.isEmpty() || password.length()<8){
            return false;
        }else {
            return true;
        }
    }

    //登录成功
    @Override
    public void onResponse(LoginUser loginUser) {
        if(loginUser.getAuth() == null) {
            setError("用户名和密码不能为空");
        }else {
            Toast.makeText(getActivity(), "登录成功 " + loginUser.getNickname(), Toast.LENGTH_SHORT).show();
            MyApplication.setLoginUser(loginUser);
            if(autoLogin.isChecked()){
                BaseAuth auth = new BaseAuth();
                auth.setUsername(username);
                auth.setPassword(password);
                new DB(getActivity()).saveAuth(auth);
            }else {
                new DB(getActivity()).deleteAuth();
            }
            if(onLoginSuccessListener != null){
                onLoginSuccessListener.onLogin(loginUser);
            }
            dismiss();
        }
    }

    //登录失败
    @Override
    public void onErrorResponse(VolleyError volleyError) {
        setError("网络错误");
    }

    private void setError(String errorMsg) {
        loginProgress.setVisibility(View.INVISIBLE);
        loginButton.setVisibility(View.VISIBLE);
        loginButton.setText("重试");
        tvLoginFail.setVisibility(View.VISIBLE);
        tvLoginFail.setText(errorMsg);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if(basicAuth != null) {
            basicAuth.cancel();
        }
        super.onDismiss(dialog);
    }

    public interface OnLoginSuccessListener{
        void onLogin(LoginUser user);
    }
}
