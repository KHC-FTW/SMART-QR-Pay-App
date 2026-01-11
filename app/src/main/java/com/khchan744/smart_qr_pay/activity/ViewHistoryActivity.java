package com.khchan744.smart_qr_pay.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.khchan744.smart_qr_pay.R;
import com.khchan744.smart_qr_pay.network.NetworkClient;
import com.khchan744.smart_qr_pay.network.dto.HistoryRespDto;
import com.khchan744.smart_qr_pay.param.UserProfile;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewHistoryActivity extends AppCompatActivity {
    private TextView tvRecordCount;
    private ListView lvItem;

    /*
     * Custom adapter for history records
     */
    private static class HistoryRecordAdapter extends ArrayAdapter<HistoryRespDto.History> {
        private final LayoutInflater inflater;

        HistoryRecordAdapter(@NonNull AppCompatActivity activity, @NonNull List<HistoryRespDto.History> items) {
            super(activity, 0, items);
            this.inflater = activity.getLayoutInflater();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = inflater.inflate(R.layout.history_record_dropdown, parent, false);
            }

            TextView primary = row.findViewById(R.id.tvCreatedAt);
            TextView secondary = row.findViewById(R.id.tvDescription);

            HistoryRespDto.History item = getItem(position);
            if (item != null) {
                primary.setText(item.getCreatedAt());
                secondary.setText(item.getDescription());
            } else {
                primary.setText("");
                secondary.setText("");
            }

            return row;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvRecordCount = findViewById(R.id.tvRecordCount);
        lvItem = findViewById(R.id.lvItem);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        handleGetHistory();
    }

    private void handleGetHistory(){
        if(NetworkClient.IS_AVAILABLE){
            String bearerToken = "Bearer " + UserProfile.getInstance().getJwt();
            NetworkClient.getAuthApi().getHistory(bearerToken).enqueue(new Callback<HistoryRespDto>() {
                @Override
                public void onResponse(Call<HistoryRespDto> call, Response<HistoryRespDto> response) {
                    try{
                        if (!response.isSuccessful()) {
                            String errorMsg = "Request failed: HTTP " + response.code();
                            Toast.makeText(ViewHistoryActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        HistoryRespDto respBody = response.body();
                        if (respBody == null) {
                            String errorMsg = "Empty response from server.";
                            Toast.makeText(ViewHistoryActivity.this,errorMsg, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String message = respBody.getResponse() != null ? respBody.getResponse() : "";
                        List<HistoryRespDto.History> historyList = respBody.getHistory();
                        tvRecordCount.setText(message);
                        lvItem.setAdapter(new HistoryRecordAdapter(ViewHistoryActivity.this, historyList));
                    }catch (RuntimeException e){
                        Toast.makeText(ViewHistoryActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<HistoryRespDto> call, Throwable t) {
                    String failMsg = "Network error: " + (t.getMessage() != null ? t.getMessage() : "unknown");
                    Toast.makeText(ViewHistoryActivity.this, failMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            String errMsg = "Failed to fetch records due to network unavailability.";
            Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}