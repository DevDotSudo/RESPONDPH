package com.ionres.respondph.sendsms;


import java.util.List;

public interface DisasterDAO {
    List<DisasterModel> getAllDisasters();
    DisasterModel getDisasterById(int disasterId);
}