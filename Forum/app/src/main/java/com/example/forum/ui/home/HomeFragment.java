package com.example.forum.ui.home;

import static android.content.Context.LOCATION_SERVICE;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.forum.AVLTreeFactory;
import com.example.forum.House;
import com.example.forum.HouseAdapter;
import com.example.forum.HouseData;
import com.example.forum.HouseTree;
import com.example.forum.House_Detail_Page;

import android.Manifest;
import android.widget.Toast;

import com.example.forum.R;
import com.example.forum.TokenParse;
import com.example.forum.databinding.FragmentHomeBinding;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.eazegraph.lib.models.PieModel;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;
    private List<String> dataList = new ArrayList<>();//初始全部房源
    private List<House> filteredDataList;//搜索后符合条件的房子

    private ArrayAdapter<String> arrayAdapter;
    private EditText editText;
    private int lastVisibleItemPosition = 0;
    private RecyclerView recyclerViewhouse;
    private List<House> houseList = new ArrayList<>();
    private TextView textview;
    LocationManager locationManager;
    LocationListener locationListener;
    private FragmentHomeBinding binding;
    String district;
    HouseAdapter adapter1;
    private Handler handler = new Handler();
    private final int INTERVAL = 30000; // 30 seconds in milliseconds
    private boolean fetchingData = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 使用视图对象查找RecyclerView
        recyclerViewhouse = root.findViewById(R.id.recyclerViewforhouse);

        // 初始化适配器，这里你需要创建一个自定义适配器，比如 HouseAdapter
        adapter1 = new HouseAdapter(houseList);

        // 设置适配器给 RecyclerView
        recyclerViewhouse.setAdapter(adapter1);

        // 使用线性布局管理器
        recyclerViewhouse.setLayoutManager(new LinearLayoutManager(getActivity()));

        recyclerViewhouse.setVisibility(View.VISIBLE);
        // 初始化数据列表
        textview = root.findViewById(R.id.textViewMap);
        locationManager = (LocationManager) requireActivity().getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
//                textView.setText("New Location:\nLatitude:" + location.getLatitude() + "\nLongitude:" + location.getLongitude());
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    // Reverse Geocoding
                    Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                        if (addresses.size() > 0) {
//                            mySignature.setText(addresses.get(0).getLocality());
                            district = addresses.get(0).getLocality();
                            textview.setText(district);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };
        refresh();


//        System.out.println(houseList);

        Button start = binding.btnLocationStart;
        start.setOnClickListener(v -> {
            applayUpdate();
        });

        Button searchNearby = binding.btnNearby;

        searchNearby.setOnClickListener(v -> {
// FirebaseDatabase uses the singleton design pattern (we cannot directly create a new instance of it).
            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
            // Get a reference to the users collection in the database and then get the specific user (as specified by the user id in this case).
            DatabaseReference databaseReference = firebaseDatabase.getReference("House").child("key:HouseId-value:city;suburb;street;building_no;unit;price;bedroom;email;recommend");

            //首页地区房子
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {


                    if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                        List<House> houseListNearBy = new ArrayList<>();


                        for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                            String item = "" + itemSnapshot.getKey() + ";" + itemSnapshot.getValue(String.class);
                            String[] property = item.split(";");
                            if (property[2].equals(textview.getText().toString())) {
                                // Set the data houselist
                                houseListNearBy.add(new House(property[0], property[1], property[2], property[3], property[4], property[5],
                                        Integer.parseInt(property[6]), Integer.parseInt(property[7]), property[8],
                                        Integer.parseInt(property[9])));
                            }

                        }
                        houseListNearBy.sort(Comparator.comparingInt(House::getLikes).reversed());
                        if (houseListNearBy.size() < 3) {
                            Toast.makeText(requireContext(), "Please Search Other Location To Get More", Toast.LENGTH_SHORT).show();
                        } else {
                            houseList.clear();
                            for (House h : houseListNearBy) {
                                houseList.add(h);
                            }

                            adapter1.notifyDataSetChanged(); // 通知适配器数据已更改
                        }

                    } else {
                        Log.d("FirebaseData", "No data available or data is null");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle any errors that may occur during the read operation
                    Log.e("FirebaseError", "Error reading data from Firebase", databaseError.toException());
                }
            });

        });


        recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setVisibility(View.GONE);

        editText = binding.inputSearch;
        loadData();

        arrayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, dataList);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int starts, int before, int count) {
                showContentIfEmpty();
                String query = s.toString();

                if (!query.isEmpty()) {
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerViewhouse.setVisibility(View.GONE);
                    textview.setVisibility(View.GONE);
                    searchNearby.setVisibility(View.GONE);
                    start.setVisibility(View.GONE);

                } else {
                    recyclerView.setVisibility(View.GONE);
                    recyclerViewhouse.setVisibility(View.VISIBLE);
                    textview.setVisibility(View.VISIBLE);
                    searchNearby.setVisibility(View.VISIBLE);
                    start.setVisibility(View.VISIBLE);

                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0 && recyclerView.getVisibility() == View.VISIBLE) {
                    recyclerView.setVisibility(View.GONE);
                }
                showContentIfChanged(s.toString()); // 传递当前文本内容以检查是否有变化
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                return new RecyclerView.ViewHolder(view) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {

                String item = dataList.get(position);
                String[] recycleshow = item.split(";");
                House searchHouse = new House(recycleshow[0], recycleshow[1], recycleshow[2], recycleshow[3], recycleshow[4], recycleshow[5], Integer.parseInt(recycleshow[6]), Integer.parseInt(recycleshow[7]), recycleshow[8], Integer.parseInt(recycleshow[9]));

                ((TextView) holder.itemView).setText(searchHouse.getSuburb() + " " + searchHouse.getStreet() + " $" + searchHouse.getPrice() + " " + searchHouse.getXbxb() + " Bedroom");
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Handle item click
                        String selectedItem = dataList.get(position);
                        // Depending on the item clicked, you can navigate to a different activity and pass data
                        String[] recycleshow = selectedItem.split(";");
                        House searchHouse = new House(recycleshow[0], recycleshow[1], recycleshow[2], recycleshow[3], recycleshow[4], recycleshow[5], Integer.parseInt(recycleshow[6]), Integer.parseInt(recycleshow[7]), recycleshow[8], Integer.parseInt(recycleshow[9]));

                        Intent intent = new Intent(v.getContext(), House_Detail_Page.class);
                        // Add data to the intent

                        intent.putExtra("houseData", (Serializable) searchHouse);
                        v.getContext().startActivity(intent);
                    }
                });
            }

            @Override
            public int getItemCount() {
                return dataList.size();
            }
        };
        recyclerView.setAdapter(adapter);

        Button searchButton = binding.buttonSearch;
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applySearch(v);
            }

            //根据token搜索

            //根据token搜索
            public void applySearch(View view) {
                if(!dataList.isEmpty()){
                    //最后展示的结果List
                    filteredDataList = new ArrayList<>();
                    //temp是一个暂时存储每次提取出来的房子
                    List<House> temp = new ArrayList<>();
                    String query = editText.getText().toString().trim();
                    TokenParse tp = new TokenParse(query);
                    //转换成AVL树
                    AVLTreeFactory avlTreeFactory = AVLTreeFactory.getInstance();
                    HouseTree houseTree = avlTreeFactory.houseTreeCreator(dataList);
//                if (query.isEmpty()) {
//                    filteredDataList = new ArrayList<>(dataList);
//                }
                    //价格
                    if (tp.getpriceRange()!=null) {
                        filteredDataList = houseTree.getHousesPriceRange(tp.getpriceRange().get(0), tp.getpriceRange().get(1));
                    } else {
                        filteredDataList = houseTree.toList();
                    }
                    boolean validSuburb = tp.getLocation() != null;
                    boolean validBed = tp.getBedrooms() != 0;
                    if (validBed && validSuburb) {
                        for (House house : filteredDataList) {
                            if (house.getXbxb() == tp.getBedrooms() && house.getSuburb().equals(tp.getLocation())) {
                                temp.add(house);
                            }
                        }
                    } else if (!validBed && validSuburb) {
                        for (House house : filteredDataList) {
                            if (house.getSuburb().equals(tp.getLocation())) {
                                temp.add(house);
                            }
                        }
                    } else if (validBed && !validSuburb) {
                        for (House house : filteredDataList) {
                            if (house.getXbxb() == tp.getBedrooms()) {
                                temp.add(house);
                            }
                        }
                    } else {
                        for (House house : filteredDataList) {
                            temp.add(house);
                        }
                    }
                    temp.sort(Comparator.comparingInt(House::getLikes).reversed());
                    //把House类型的List转化为String List的显示结果
                    dataList=new ArrayList<>();

                    for (House house : temp) {
                        System.out.println(house);
                        dataList.add(house.getId() + ";" + house.getCity() + ";" + house.getSuburb() + ";" + house.getStreet() + ";" + house.getStreetNumber() + ";" + house.getUnit() + ";" + house.getPrice() + ";" + house.getXbxb() + ";" + house.getEmail() + ";" + house.getLikes() + ";");
//                    System.out.println(house.toString());
                        System.out.println(dataList);
                    }

                    Toast.makeText(requireContext(), "Find "+dataList.size()+" Place", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    Log.d("Debug", "Adapter notified of data change");
                }
            }

        });


        DatabaseReference dR = FirebaseDatabase.getInstance().getReference("House").child("1");

        dR.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                // This method is called when new data is added.
                // You can access the new data in the 'dataSnapshot' object.
                String newData = dataSnapshot.getValue(String.class);

                // 'previousChildName' is the key of the previous child in the query (if any).
                // This can help you distinguish new additions from updates.
                refresh();
                Toast.makeText(getContext(), "New Houses Available!", Toast.LENGTH_SHORT).show();
                // Your code to handle the new data addition here.
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                // This method is called when an existing child's data is updated.
                // Handle existing data changes here.

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // This method is called when a child is removed from the database.
                // Handle data removal here.
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                // This method is called when a child's position changes (not relevant for new data additions).
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle any database error.
            }
        });


        return root;
    }

    private void refresh() {
        if (!fetchingData) {
            fetchingData = true;

            // Initialize Firebase
            // Fetch data from Firebase and handle it here
            FirebaseDatabase firebaseDatabase1 = FirebaseDatabase.getInstance();
            // Get a reference to the users collection in the database and then get the specific user (as specified by the user id in this case).
            DatabaseReference databaseReference1 = firebaseDatabase1.getReference("House").child("key:HouseId-value:city;suburb;street;building_no;unit;price;bedroom;email;recommend");
            houseList.clear();
            databaseReference1.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                        for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                            String item = "" + itemSnapshot.getKey() + ";" + itemSnapshot.getValue(String.class);
                            String[] property = item.split(";");
                            // Set the data houselist
                            houseList.add(new House(property[0], property[1], property[2], property[3], property[4], property[5],
                                    Integer.parseInt(property[6]), Integer.parseInt(property[7]), property[8],
                                    Integer.parseInt(property[9])));


                        }
                        houseList.sort(Comparator.comparingInt(House::getLikes).reversed());

// After data fetch is complete, reset the flag and schedule the next task
                        fetchingData = false;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                refresh();
                            }
                        }, INTERVAL);
                        adapter1.notifyDataSetChanged(); // 通知适配器数据已更改
                        Toast.makeText(getActivity(), "Refreshed", Toast.LENGTH_SHORT).show();

                    } else {
                        Log.d("FirebaseData", "No data available or data is null");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle any errors that may occur during the read operation
                    Log.e("FirebaseError", "Error reading data from Firebase", databaseError.toException());
                }
            });

        }


    }

    //全部数据
    private void loadData() {

        // FirebaseDatabase uses the singleton design pattern (we cannot directly create a new instance of it).
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        // Get a reference to the users collection in the database and then get the specific user (as specified by the user id in this case).
        DatabaseReference databaseReference = firebaseDatabase.getReference("House").child("key:HouseId-value:city;suburb;street;building_no;unit;price;bedroom;email;recommend");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    dataList.clear();
                    for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                        String item = "" + itemSnapshot.getKey() + ";" + itemSnapshot.getValue(String.class);
                        dataList.add(item);
                    }

                } else {
                    Log.d("FirebaseData", "No data available or data is null");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle any errors that may occur during the read operation
                Log.e("FirebaseError", "Error reading data from Firebase", databaseError.toException());
            }
        });
    }




    @SuppressLint("NotifyDataSetChanged")
    public void showContentIfEmpty() {
        String query = editText.getText().toString().trim();
        if (query.isEmpty()) {
            dataList.clear();
            loadData();
            adapter.notifyDataSetChanged();
        }
    }

    public void showContentIfChanged(String newText) {
        String query = editText.getText().toString().trim();
        if (!newText.equals(query)) {
            // 当文本发生变化时执行加载数据的操作
            dataList.clear();
            loadData();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void applayUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.INTERNET

                }, 0);
                return;
            }
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
//            }
        }
        locationManager.requestLocationUpdates("gps", 0, 0, locationListener);
    }
}
