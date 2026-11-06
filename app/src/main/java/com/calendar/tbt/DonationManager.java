package com.calendar.tbt;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;

import java.util.Collections;
import java.util.List;

/**
 * Ủng hộ tự nguyện qua Google Play Billing (one-time products).
 * App vẫn miễn phí; không có tính năng nào bị khóa.
 */
public final class DonationManager implements PurchasesUpdatedListener {

    public static final String[] PRODUCT_IDS = {
            "donate_tea", "donate_coffee", "donate_month_coffee"
    };

    public static final String[] OPTION_LABELS = {
            "Mời 1 tách trà",
            "Mời 1 cốc cà phê",
            "Mời 1 tháng cà phê"
    };

    private final Activity activity;
    private BillingClient billingClient;
    private AlertDialog dialog;
    private boolean connecting;
    private String pendingProductId;

    public DonationManager(Activity activity) {
        this.activity = activity;
    }

    public void showDialog() {
        if (activity.isFinishing()) return;
        ensureConnection(null);

        View content = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_donation, null, false);
        Spinner options = content.findViewById(R.id.donation_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, OPTION_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        options.setAdapter(adapter);

        dialog = new AlertDialog.Builder(activity)
                .setTitle("Mời mình một ly?")
                .setView(content)
                .setPositiveButton("Ủng hộ", null)
                .setNegativeButton("Để sau", null)
                .create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    int position = options.getSelectedItemPosition();
                    if (position < 0 || position >= PRODUCT_IDS.length) return;
                    String productId = PRODUCT_IDS[position];
                    dialog.dismiss();
                    purchase(productId);
                }));
        dialog.show();
    }

    private void purchase(String productId) {
        pendingProductId = productId;
        if (billingClient == null || !billingClient.isReady()) {
            ensureConnection(productId);
            toast("Đang kết nối Google Play…");
            return;
        }
        queryAndLaunch(productId);
    }

    private void ensureConnection(@Nullable String productId) {
        if (productId != null) pendingProductId = productId;
        if (billingClient == null) {
            billingClient = BillingClient.newBuilder(activity)
                    .setListener(this)
                    // Billing 8.x bắt buộc khai báo rõ loại sản phẩm cho giao dịch chờ.
                    .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                            .enableOneTimeProducts()
                            .build())
                    .build();
        }
        if (billingClient.isReady()) {
            if (pendingProductId != null) queryAndLaunch(pendingProductId);
            return;
        }
        if (connecting) return;
        connecting = true;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                connecting = false;
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    if (pendingProductId != null) queryAndLaunch(pendingProductId);
                } else {
                    toast("Google Play Billing chưa sẵn sàng trên thiết bị này.");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                connecting = false;
            }
        });
    }

    private void queryAndLaunch(String productId) {
        QueryProductDetailsParams.Product product =
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build();
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(product))
                .build();
        billingClient.queryProductDetailsAsync(params, (result, queryResult) -> {
            List<ProductDetails> detailsList = queryResult == null
                    ? null : queryResult.getProductDetailsList();
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK
                    || detailsList == null || detailsList.isEmpty()) {
                activity.runOnUiThread(() ->
                        toast("Sản phẩm ủng hộ chưa được cấu hình trên Google Play."));
                return;
            }
            ProductDetails details = detailsList.get(0);
            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(Collections.singletonList(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(details)
                                    .build()))
                    .build();
            activity.runOnUiThread(() -> {
                if (!activity.isFinishing()) billingClient.launchBillingFlow(activity, flowParams);
            });
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult result, @Nullable List<Purchase> purchases) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            return;
        }
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK
                || purchases == null) {
            toast("Không thể hoàn tất thanh toán. Vui lòng thử lại.");
            return;
        }
        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                consume(purchase);
            } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                toast("Khoản ủng hộ đang chờ Google Play xử lý.");
            }
        }
    }

    private void consume(Purchase purchase) {
        ConsumeParams params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.consumeAsync(params, (result, purchaseToken) -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                toast("Cảm ơn bạn rất nhiều!");
            } else {
                toast("Đã ghi nhận ủng hộ. Cảm ơn bạn!");
            }
        });
    }

    private void toast(String message) {
        if (TextUtils.isEmpty(message) || activity.isFinishing()) return;
        activity.runOnUiThread(() ->
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
    }

    public void destroy() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        if (billingClient != null) {
            billingClient.endConnection();
            billingClient = null;
        }
    }
}
