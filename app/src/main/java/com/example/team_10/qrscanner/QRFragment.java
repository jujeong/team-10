package com.example.team_10.qrscanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.room.Room;

import com.example.team_10.R;
import com.example.team_10.seat.SeatFragment;
import com.example.team_10.seat.seatDB.SeatDao;
import com.example.team_10.seat.seatDB.SeatDatabase;
import com.example.team_10.seat.seatDB.SeatEntity;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class QRFragment extends Fragment {

    private static final String TAG = "QRCodeScanner";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    private SurfaceView surfaceView;
    private CameraSource cameraSource;

    private SeatDatabase seatDatabase;
    private SeatDao seatDao;
    private static final String DATABASE_NAME = "seats-database";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.qr_fragment, container, false);
        surfaceView = view.findViewById(R.id.surfaceView);
        // Create a barcode detector
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        // Create a camera source
        cameraSource = new CameraSource.Builder(getContext(), barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true)
                .build();

        // Add surface view callback
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                // Check camera permission
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    startCamera();
                } else {
                    // Request camera permission
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                }
            }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                cameraSource.stop();
            }
        });
        // Set barcode processor
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }
            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    String qrCodeValue = barcodes.valueAt(0).rawValue;

                    Log.d(TAG, "QR Code Value: " + qrCodeValue);
                    showToast(qrCodeValue);
                    // Create an instance of the SeatFragment
                    SeatFragment seatFragment = new SeatFragment();
                    // Get the FragmentManager
                    FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                    // Start a FragmentTransaction
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    // Replace the current fragment with the SeatFragment
                    fragmentTransaction.replace(R.id.fragmentContainer, seatFragment);
                    // Add the transaction to the back stack (optional)
                    fragmentTransaction.addToBackStack(null);
                    // Commit the transaction
                    fragmentTransaction.commit();
                }
            }
        });

        return view;
    }

    private void startCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraSource.start(surfaceView.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showToast(final String message) {
        String[] seatArray = getResources().getStringArray(R.array.SeatQr);
        final String finalLoc = findSeatLocation(message.substring(24), seatArray);
        ///////////////////////
        seatDatabase = Room.databaseBuilder(getActivity().getApplicationContext(), SeatDatabase.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
        seatDao = seatDatabase.seatDao();

        boolean occupied = true;
        String seatId = finalLoc;
        String userId = "";
        SeatEntity seat = new SeatEntity(seatId, userId, occupied);
        seatDao.update(seat);
        /////////////////////////
        //getActivity().runOnUiThread(() -> Toast.makeText(getContext(), finalLoc, Toast.LENGTH_SHORT).show());
    }

    private String findSeatLocation(String message, String[] seatArray) {
        String loc = "";
        for (int i = 0; i < seatArray.length; i++) {
            if (seatArray[i].equals(message)) {
                loc = Integer.toString(i + 1);
                break;
            }
        }
        return loc;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}