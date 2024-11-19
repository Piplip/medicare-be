package com.nkd.medicare.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
@Getter
@Setter
@AllArgsConstructor
public class PharmacistSignal {
    String signalID;
    Map<?,?> data;
}
