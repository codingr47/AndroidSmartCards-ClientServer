package com.codingr.hceapp;

import com.codingr.nfclib.hce.annotations.HceService;
import com.codingr.nfclib.hce.core.ApduRouterService;

@HceService(
    description = "My Awesome HCE Service",
    aids = {"F0010203040506", "F0010203040507"}
)
public class MyHceService extends ApduRouterService {
}
