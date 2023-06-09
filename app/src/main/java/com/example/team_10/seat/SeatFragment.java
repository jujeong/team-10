package com.example.team_10.seat;

import android.graphics.Color;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.room.Room;

import com.example.team_10.R;
import com.example.team_10.seat.seatDB.SeatDao;
import com.example.team_10.seat.seatDB.SeatDatabase;
import com.example.team_10.seat.seatDB.SeatEntity;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.util.ArrayList;
import java.util.List;

public class SeatFragment extends Fragment implements BarcodeDetector.Processor<Barcode> {

    private ArrayList<Boolean> seatStatusList;
    private GridLayout gridLayout;
    private BarcodeDetector barcodeDetector;

    private SeatDatabase seatDatabase;
    private SeatDao seatDao;
    private static final String DATABASE_NAME = "seats-database";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.seat_fragment, container, false);

        gridLayout = view.findViewById(R.id.gridLayout);

        initializeSeatStatus();
        updateSeatView();

        // Initialize the SeatDatabase and obtain the SeatDao instance
        SeatDatabase seatDatabase = Room.databaseBuilder(requireContext(), SeatDatabase.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
        seatDao = seatDatabase.seatDao();

        barcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        barcodeDetector.setProcessor(this);

        return view;
    }

    @Override
    public void release() {
        // Implement release method if needed
    }

    @Override
    public void receiveDetections(Detector.Detections<Barcode> detections) {
        final SparseArray<Barcode> barcodes = detections.getDetectedItems();
        if (barcodes.size() != 0) {
            String qrCodeValue = barcodes.valueAt(0).displayValue;
            int seatIndex = extractSeatIndex(qrCodeValue);
            if (seatIndex >= 0 && seatIndex < seatStatusList.size()) {
                seatStatusList.set(seatIndex, false);
                updateSeatView();
                // Update the seat status in the database
                String seatId = String.valueOf(seatIndex);
                boolean occupied = false;
                seatDao.update(new SeatEntity(seatId, null, occupied));
            }
        }
    }

    private void initializeSeatStatus() {
        seatStatusList = new ArrayList<>();
        int totalSeats = 20; // 4 rows * 5 columns
        for (int i = 0; i < totalSeats; i++) {
            seatStatusList.add(true); // true: 사용 가능, false: 사용 중
        }
    }

    private void updateSeatView() {
        int totalSeats = 20; // 4 rows * 5 columns

        gridLayout.removeAllViews(); // Clear the grid layout before updating

        seatDatabase = Room.databaseBuilder(getActivity().getApplicationContext(), SeatDatabase.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
        seatDao = seatDatabase.seatDao();

        for (int i = 0; i < totalSeats; i++) {
            View seatView = new View(getContext());

            // Set some basic layout parameters for the seat views
            int size = getResources().getDimensionPixelSize(R.dimen.seat_size);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(10, 10, 10, 10); // Set margins if you want some spacing between your seats
            seatView.setLayoutParams(params);

            if (!seatDao.isSeatOccupied(Integer.toString(i + 1))) {
                seatView.setBackgroundColor(Color.WHITE); // 사용 가능한 좌석은 흰색으로 표시
            } else {
                seatView.setBackgroundColor(Color.RED); // 사용 중인 좌석은 빨간색으로 표시
            }

            gridLayout.addView(seatView);
        }
    }

    private int extractSeatIndex(String qrCodeValue) {
        // QR 코드에서 좌석 인덱스를 추출하는 로직을 구현하세요.
        // 예를 들어, qrCodeValue가 좌석의 고유 ID나 인덱스를 나타낸다고 가정하고 해당 값을 추출합니다.
        // 추출된 값은 seatStatusList의 인덱스로 사용됩니다.
        // 적절한 로직으로 qrCodeValue에서 좌석 인덱스를 추출하여 반환합니다.
        // 예제로 인덱스를 추출하는 코드를 작성합니다.
        try {
            int seatIndex = Integer.parseInt(qrCodeValue); // 가정: qrCodeValue는 정수로 표현된 좌석 인덱스
            return seatIndex;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1; // 추출에 실패한 경우 -1을 반환
        }
    }
}