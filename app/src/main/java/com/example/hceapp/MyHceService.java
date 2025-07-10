package com.example.hceapp;

import com.example.hcelibrary.annotations.HceService;
import com.example.hcelibrary.core.ApduRouterService;

@HceService(
    description = "My Awesome HCE Service",
    aids = {"F0010203040506", "F0010203040507"}
)
public class MyHceService extends ApduRouterService {
}
