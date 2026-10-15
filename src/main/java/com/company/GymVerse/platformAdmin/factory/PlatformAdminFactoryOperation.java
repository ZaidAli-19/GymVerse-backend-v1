package com.company.GymVerse.platformAdmin.factory;

import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.response.PlatformAdminResponse;
import com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType;

public interface PlatformAdminFactoryOperation {
    PlatformAdminRequestType getRequestType();
    PlatformAdminResponse execute(PlatformAdminRequest request);
}
