package com.example.camerax;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.camerax.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private com.example.camerax.databinding.ActivityMainBinding mViewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());
    }
}