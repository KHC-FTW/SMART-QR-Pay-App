package com.khchan744.smart_qr_pay.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.khchan744.smart_qr_pay.R;
import com.khchan744.smart_qr_pay.network.NetworkClient;
import com.khchan744.smart_qr_pay.network.dto.LoginRespDto;
import com.khchan744.smart_qr_pay.network.dto.StandardRespDto;
import com.khchan744.smart_qr_pay.param.UserProfile;

import java.util.concurrent.ThreadLocalRandom;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainMenuActivity extends AppCompatActivity {

    private TextView tvUsername;
    private TextView tvCurrentBalanceValue;
    private UserProfile userProfile;


    @ExperimentalGetImage
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvUsername = findViewById(R.id.tvUsername);
        tvCurrentBalanceValue = findViewById(R.id.tvCurrentBalanceValue);

        userProfile = UserProfile.getInstance();
        tvUsername.setText(userProfile.getUsername());
        getBalance();

        findViewById(R.id.payIcon).setOnClickListener(v -> {
            startActivity(new Intent(this, MakePaymentActivity.class));
        });
        findViewById(R.id.btnPay).setOnClickListener(v -> {
            startActivity(new Intent(this, MakePaymentActivity.class));
        });
        findViewById(R.id.receiveIcon).setOnClickListener(v -> {
            startActivity(new Intent(this, ReceivePaymentActivity.class));
        });
        findViewById(R.id.btnReceive).setOnClickListener(v -> {
            startActivity(new Intent(this, ReceivePaymentActivity.class));
        });
        findViewById(R.id.historyIcon).setOnClickListener(v -> {
            startActivity(new Intent(this, ViewHistoryActivity.class));
        });
        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, ViewHistoryActivity.class));
        });
        findViewById(R.id.topUpIcon).setOnClickListener(v -> {
            startActivity(new Intent(this, TopUpActivity.class));
        });
        findViewById(R.id.btnTopUp).setOnClickListener(v -> {
            startActivity(new Intent(this, TopUpActivity.class));
        });

        findViewById(R.id.btnSetting).setOnClickListener(this::handleSettingClick);
    }

    @Override
    protected void onResume(){
        super.onResume();
        getBalance();
    }

    @ExperimentalGetImage
    private void handleSettingClick(View v){
        PopupMenu settingPopup = new PopupMenu(this, v, Gravity.END);
        settingPopup.getMenuInflater().inflate(R.menu.menu_main, settingPopup.getMenu());
        settingPopup.setOnMenuItemClickListener( item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_log_out){
                userProfile.reset();
                finish();
                return true;
            }else if (itemId == R.id.action_developer_mode){
                startActivity(new Intent(this, DeveloperModeActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        });
        settingPopup.show();
    }

    private void getBalance(){
        if(NetworkClient.IS_AVAILABLE){
            String bearerToken = "Bearer " + userProfile.getJwt();
            NetworkClient.getAuthApi().getBalance(bearerToken).enqueue(new Callback<StandardRespDto>() {
                @Override
                public void onResponse(Call<StandardRespDto> call, Response<StandardRespDto> response) {
                    if (!response.isSuccessful()) {
                        String errorMsg = "Request failed: HTTP " + response.code();
                        Toast.makeText(MainMenuActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    StandardRespDto respBody = response.body();
                    if (respBody == null) {
                        String errorMsg = "Empty response from server.";
                        Toast.makeText(MainMenuActivity.this,errorMsg, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String message = respBody.getResponse() != null ? respBody.getResponse() : "";
                    if(respBody.isSuccess()){
                        tvCurrentBalanceValue.setText(message);
                    }else{
                        Toast.makeText(MainMenuActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<StandardRespDto> call, Throwable t) {
                    String failMsg = "Network error: " + (t.getMessage() != null ? t.getMessage() : "unknown");
                    Toast.makeText(MainMenuActivity.this, failMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            // no network -> randomly create a balance value
            String randomBalance = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10001));
            tvCurrentBalanceValue.setText(randomBalance);
        }
    }
}