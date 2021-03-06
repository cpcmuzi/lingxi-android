package me.cl.lingxi.module.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.cl.library.base.BaseFragment;
import me.cl.lingxi.R;
import me.cl.lingxi.common.config.Api;
import me.cl.lingxi.common.config.Constants;
import me.cl.lingxi.common.okhttp.OkUtil;
import me.cl.lingxi.common.okhttp.ResultCallback;
import me.cl.lingxi.common.result.Result;
import me.cl.lingxi.common.util.ContentUtil;
import me.cl.lingxi.common.util.SPUtil;
import me.cl.lingxi.common.util.Utils;
import me.cl.lingxi.dialog.LogoutDialog;
import me.cl.lingxi.entity.UserInfo;
import me.cl.lingxi.module.member.LoginActivity;
import me.cl.lingxi.module.mine.PersonalInfoActivity;
import me.cl.lingxi.module.mine.RelevantActivity;
import me.cl.lingxi.module.setting.AboutActivity;
import okhttp3.Call;

/**
 * 我的界面
 */
public class MineFragment extends BaseFragment {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.user_img)
    ImageView mUserImg;
    @BindView(R.id.user_name)
    TextView mUserName;
    @BindView(R.id.user_description)
    TextView mUserDescription;
    @BindView(R.id.user_body)
    LinearLayout mUserBody;
    @BindView(R.id.mine_top)
    RelativeLayout mMineTop;
    @BindView(R.id.mine_reply)
    TextView mMineReply;
    @BindView(R.id.mine_relevant)
    TextView mMineRelevant;
    @BindView(R.id.mine_setting)
    TextView mMineSetting;
    @BindView(R.id.mine_about)
    TextView mMineAbout;
    @BindView(R.id.mine_sign_out)
    TextView mMineSignOut;

    private String mUserId;
    private OperateBroadcastReceiver receiver;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mine_fragment, container, false);
        ButterKnife.bind(this, view);
        init(view);
        initReceiver();
        return view;
    }

    private void init(View view) {
        setupToolbar(mToolbar, R.string.nav_mine, 0, null);
        mUserId = SPUtil.build().getString(Constants.SP_USER_ID);
        // 获取用户信息
        postUserInfo(mUserId);
    }


    /**
     * 广播接收者
     * 用于更新用户信息
     */
    private final class OperateBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case Constants.UPDATE_USER_IMG:
                        postUserInfo(mUserId);
                        break;
                }
            }
        }
    }

    private void initReceiver() {
        receiver = new OperateBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.UPDATE_USER_IMG);
        getActivity().registerReceiver(receiver, filter);
    }

    private void postUserInfo(String id) {
        OkUtil.post()
                .url(Api.userInfo)
                .addParam("id", id)
                .execute(new ResultCallback<Result<UserInfo>>() {

                    @Override
                    public void onSuccess(Result<UserInfo> response) {
                        initUser(response.getData());
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        initUser(null);
                    }

                    @Override
                    public void onFinish() {
                        initUser(null);
                    }
                });
    }

    private void initUser(UserInfo userInfo) {
        String username = getString(R.string.app_name);
        String avatar = "";
        if (userInfo != null) {
            username = userInfo.getUsername();
            avatar = userInfo.getAvatar();
        }
        mUserName.setText(username);
        ContentUtil.loadUserAvatar(mUserImg, avatar);
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        ContentUtil.setMoreBadge(mMineRelevant);
        if (Constants.isRead) {
            ((MainActivity) getActivity()).goneBadge();
        }
    }

    @OnClick({R.id.user_body, R.id.mine_reply, R.id.mine_relevant, R.id.mine_setting, R.id.mine_about, R.id.mine_sign_out})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.user_body:
                gotoPersonal();
                break;
            case R.id.mine_reply:
                gotoRelevant(Constants.REPLY_MINE);
                break;
            case R.id.mine_relevant:
                gotoRelevant(Constants.REPLY_RELEVANT);
                break;
            case R.id.mine_setting:
                boolean isJoin = Utils.joinQQGroup(getContext(), "U6BT7JHlX9bzMdCNWjkIjwu5g3Yt_Wi9");
                if (!isJoin) {
                    showToast("未安装手Q或安装的版本不支持");
                }
                break;
            case R.id.mine_about:
                gotoAbout();
                break;
            case R.id.mine_sign_out:
                showLogoutDialog();
                break;
        }
    }

    /**
     * 展示登出Dialog
     */
    private void showLogoutDialog() {
        String tag = "logout";
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        // 清除已经存在的，同样的fragment
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        transaction.addToBackStack(null);
        // 展示dialog
        LogoutDialog logoutDialog = LogoutDialog.newInstance();
        logoutDialog.show(transaction, tag);
        logoutDialog.setLogoutListener(new LogoutDialog.LogoutListener() {
            @Override
            public void onLogout() {
                gotoLogin();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        // 解决Activity has leaked window that was originally added here
        // 如果dialog存在或显示，dismiss
    }

    // 前往信息修改
    private void gotoPersonal() {
        Intent goPerson = new Intent(getActivity(), PersonalInfoActivity.class);
        startActivity(goPerson);
    }

    // 前往关于
    private void gotoAbout() {
        Intent goAbout = new Intent(getActivity(), AboutActivity.class);
        startActivity(goAbout);
    }

    // 前往与我相关
    private void gotoRelevant(String type) {
        Intent goRelevant = new Intent(getActivity(), RelevantActivity.class);
        goRelevant.putExtra(Constants.REPLY_TYPE, type);
        startActivity(goRelevant);
    }

    // 前往登录
    private void gotoLogin() {
        MainActivity activity = (MainActivity) getActivity();
        SPUtil.build().putBoolean(Constants.SP_BEEN_LOGIN, false);
        Intent intent = new Intent(activity, LoginActivity.class);
        startActivity(intent);
        if (activity != null) {
            activity.finish();
        }
    }

}
